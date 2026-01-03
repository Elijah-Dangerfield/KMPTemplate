package com.dangerfield.merizo.libraries.merizo.impl

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dangerfield.merizo.UsageStatsCollectorWorker
import com.dangerfield.merizo.libraries.core.logging.KLog
import com.dangerfield.merizo.libraries.merizo.PlatformApp
import com.dangerfield.merizo.libraries.merizo.MonitoringRuleSpec
import com.dangerfield.merizo.libraries.merizo.UsageMonitoringCoordinator
import com.dangerfield.merizo.libraries.merizo.UsageMonitoringResult
import com.dangerfield.merizo.libraries.merizo.UsageMonitoringStatus
import com.dangerfield.merizo.libraries.merizo.UsageSelectionRefreshResult
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.util.concurrent.TimeUnit

private const val COLLECTOR_UNIQUE_NAME = "usage-stats-collector"

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = UsageMonitoringCoordinator::class, replaces = [NoopUsageMonitoringCoordinator::class])
@Inject
class AndroidUsageMonitoringCoordinator(
    private val context: Context,
    private val prefs: UsageMonitoringPrefs
) : UsageMonitoringCoordinator {

    private val logger = KLog.withTag("UsageMonitoring")

    override fun authorizationStatus(): UsageMonitoringStatus =
        if (hasUsageStatsPermission()) UsageMonitoringStatus.Authorized
        else UsageMonitoringStatus.NotDetermined

    override suspend fun requestAuthorization(): UsageMonitoringResult {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        kotlin.runCatching { context.startActivity(intent) }
            .onFailure { logger.e(it) { "Failed to launch usage access settings" } }
        val status = authorizationStatus()
        return UsageMonitoringResult(status = status)
    }

    override suspend fun ensureMonitoring(ruleSpecs: List<MonitoringRuleSpec>) {
        if (ruleSpecs.isEmpty()) {
            logger.i { "No monitoring specs; clearing tracked selections" }
            prefs.clearSelections()
            scheduleCollector()
            return
        }

        val selections = ruleSpecs.flatMap { spec ->
            spec.appSelections.mapNotNull { selection -> selection as? PlatformApp.AndroidPlatformApp }
        }.distinctBy { it.packageName }
        prefs.saveSelections(selections)

        // Reset the cursor when schedule changes so we do not miss new data windows
        prefs.markCollectionStart(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15))

        scheduleCollector()
    }

    override suspend fun refreshSelections(currentSelections: List<PlatformApp>): UsageSelectionRefreshResult {
        val selections = currentSelections.mapNotNull { it as? PlatformApp.AndroidPlatformApp }.distinctBy { it.packageName }
        if (selections.isEmpty()) {
            prefs.clearSelections()
        } else {
            prefs.saveSelections(selections)
        }
        scheduleCollector()
        return UsageSelectionRefreshResult(selections = currentSelections, cancelled = false)
    }

    private fun scheduleCollector() {
        val workManager = WorkManager.getInstance(context)
        val periodic = PeriodicWorkRequestBuilder<UsageStatsCollectorWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            COLLECTOR_UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )

        val oneTime = OneTimeWorkRequestBuilder<UsageStatsCollectorWorker>().build()
        workManager.enqueueUniqueWork(
            "${COLLECTOR_UNIQUE_NAME}-immediate",
            ExistingWorkPolicy.REPLACE,
            oneTime
        )
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
