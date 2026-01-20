package com.dangerfield.goodtimes.libraries.goodtimes.impl

import com.dangerfield.goodtimes.libraries.core.logging.KLog
import com.dangerfield.goodtimes.libraries.flowroutines.AppCoroutineScope
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.AppEvent
import com.dangerfield.goodtimes.libraries.goodtimes.AppEventListener
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.Session
import com.dangerfield.goodtimes.libraries.goodtimes.SessionCache
import com.dangerfield.goodtimes.libraries.goodtimes.SessionRepository
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

private val SESSION_TIMEOUT = 10.minutes

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SessionRepository::class)
@ContributesBinding(AppScope::class, multibinding = true, boundType = AppEventListener::class)
@Inject
class SessionRepositoryImpl(
    private val sessionCache: SessionCache,
    private val appCache: AppCache,
    private val userDao: UserDao,
    private val appScope: AppCoroutineScope,
    private val clock: Clock
) : SessionRepository, AppEventListener {

    private val logger = KLog.withTag("SessionRepository")

    private val _currentSession = MutableStateFlow<Session?>(null)
    override val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()
    
    override val moodBannerDisabled: Flow<Boolean> = appCache.updates.map { it.moodBannerDisabled }
    
    override val moodBannerDismissCount: Flow<Int> = appCache.updates.map { it.moodBannerDismissCount }
    
    override val moodBannerToggleCount: Flow<Int> = appCache.updates.map { it.moodBannerToggleCount }
    
    override val hasEverAnsweredMood: Flow<Boolean> = appCache.updates.map { it.hasEverAnsweredMood }
    
    override val lastMoodInteractionAt: Flow<Long?> = appCache.updates.map { it.lastMoodInteractionAt }

    private var lastBackgroundedAt: Instant? = null

    override fun onForeground(event: AppEvent.OnForeground) {
        logger.d("onForeground isColdBoot=${event.isColdBoot}")

        val shouldCreateNewSession = when {
            event.isColdBoot -> true
            _currentSession.value == null -> true
            hasSessionTimedOut() -> true
            else -> false
        }

        if (shouldCreateNewSession) {
            createNewSession()
        }

        lastBackgroundedAt = null
    }

    override fun onBackground(event: AppEvent.OnBackground) {
        logger.d("onBackground")
        lastBackgroundedAt = clock.now()
    }

    override fun setMood(mood: Mood) {
        logger.d("setMood mood=$mood")
        _currentSession.update { session ->
            session?.copy(mood = mood, moodDismissed = false)
        }
        appScope.launch {
            appCache.update { data ->
                data.copy(
                    hasEverAnsweredMood = true,
                    lastMoodInteractionAt = clock.now().toEpochMilliseconds()
                )
            }
        }
    }

    override fun dismissMood() {
        logger.d("dismissMood")
        _currentSession.update { session ->
            session?.copy(moodDismissed = true)
        }
        appScope.launch {
            appCache.update { data ->
                data.copy(
                    moodBannerDismissCount = data.moodBannerDismissCount + 1,
                    lastMoodInteractionAt = clock.now().toEpochMilliseconds()
                )
            }
        }
    }
    
    override fun disableMoodBannerPermanently() {
        logger.d("disableMoodBannerPermanently")
        _currentSession.update { session ->
            session?.copy(moodDismissed = true)
        }
        appScope.launch {
            appCache.update { data ->
                data.copy(
                    moodBannerDisabled = true,
                    moodBannerToggleCount = data.moodBannerToggleCount + 1
                )
            }
        }
    }
    
    override fun enableMoodBanner() {
        logger.d("enableMoodBanner")
        appScope.launch {
            appCache.update { data ->
                data.copy(
                    moodBannerDisabled = false,
                    moodBannerToggleCount = data.moodBannerToggleCount + 1
                )
            }
        }
    }

    private fun hasSessionTimedOut(): Boolean {
        val backgroundedAt = lastBackgroundedAt ?: return false
        val elapsed = clock.now() - backgroundedAt
        return elapsed >= SESSION_TIMEOUT
    }

    private fun createNewSession() {
        appScope.launch {
            val now = clock.now()
            val hour = now.toLocalDateTime(TimeZone.currentSystemDefault()).hour
            
            val cacheData = sessionCache.update { data ->
                data.copy(totalSessionCount = data.totalSessionCount + 1)
            }

            val newSession = Session(
                id = Uuid.random().toString(),
                sessionNumber = cacheData.totalSessionCount,
                startedAt = now,
            )

            logger.i("Created new session: id=${newSession.id}, number=${newSession.sessionNumber}, hour=$hour")
            _currentSession.value = newSession
            
            // Track time-of-day patterns on User
            userDao.incrementSessionCount(now.toEpochMilliseconds())
            when (hour) {
                in 0..4 -> userDao.incrementLateNightSessionCount()
                in 6..10 -> userDao.incrementMorningSessionCount()
                in 11..14 -> userDao.incrementMiddaySessionCount()
            }
        }
    }
}
