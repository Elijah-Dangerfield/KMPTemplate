package com.dangerfield.merizo

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dangerfield.merizo.libraries.core.logging.KLog
import com.dangerfield.merizo.libraries.merizo.UsageEvent
import com.dangerfield.merizo.libraries.merizo.UsageEventSink
import com.dangerfield.merizo.libraries.merizo.UsageSource
import com.dangerfield.merizo.libraries.merizo.impl.AndroidUsageMonitoringCoordinator
import com.dangerfield.merizo.libraries.merizo.impl.UsageMonitoringPrefs
import kotlinx.datetime.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Periodically pulls UsageStats events for the tracked packages and forwards them into the shared
 * UsageEventSink. This worker is scheduled by [AndroidUsageMonitoringCoordinator].
 */
class UsageStatsCollectorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val logger = KLog.withTag("UsageStatsWorker")

    private val component: AppComponent by lazy {
        AndroidAppComponent::class.create(applicationContext)
    }

    private val prefs by lazy { UsageMonitoringPrefs(applicationContext) }

    override suspend fun doWork(): Result {
        val selections = prefs.loadSelections()
        if (selections.isEmpty()) {
            logger.i { "No tracked selections; skipping usage pull" }
            return Result.success()
        }

        val usageStats = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return Result.retry()

        val end = System.currentTimeMillis()
        val start = prefs.lastCursor() ?: (end - TimeUnit.MINUTES.toMillis(15))

        val events = usageStats.queryEvents(start, end)
        val sessions = buildSessions(events, selections.associateBy { it.packageName })

        if (sessions.isEmpty()) {
            prefs.markCollectionStart(end)
            return Result.success()
        }

        val sink: UsageEventSink = component.usageEventSink
        sink.onUsageEvents(sessions)

        prefs.markCollectionStart(end)
        return Result.success()
    }

    private fun buildSessions(
        usageEvents: android.app.usage.UsageEvents,
        tracked: Map<String, UsageMonitoringPrefs.TrackedSelection>
    ): List<UsageEvent> {
        val sessions = mutableListOf<UsageEvent>()
        val inFlight = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val packageName = event.packageName ?: continue
            val selection = tracked[packageName] ?: continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    inFlight[packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = inFlight.remove(packageName) ?: continue
                    val duration = (event.timeStamp - start).coerceAtLeast(0)
                    if (duration <= 0) continue
                    sessions += UsageEvent(
                        selectionId = packageName,
                        bundleIdentifier = packageName,
                        displayName = selection.displayName ?: packageName,
                        openedAt = Instant.fromEpochMilliseconds(start),
                        closedAt = Instant.fromEpochMilliseconds(event.timeStamp),
                        duration = duration.milliseconds,
                        openCountDelta = 1,
                        source = UsageSource.USAGE_STATS,
                        rawMetadata = emptyMap()
                    )
                }
            }
        }
        return sessions
    }
}
