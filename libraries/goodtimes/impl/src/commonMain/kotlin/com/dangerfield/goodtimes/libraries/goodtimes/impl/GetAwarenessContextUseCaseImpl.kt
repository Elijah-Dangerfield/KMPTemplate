package com.dangerfield.goodtimes.libraries.goodtimes.impl

import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.AwarenessContext
import com.dangerfield.goodtimes.libraries.goodtimes.DeviceContext
import com.dangerfield.goodtimes.libraries.goodtimes.GetAwarenessContextUseCase
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.MoodContext
import com.dangerfield.goodtimes.libraries.goodtimes.PersonalityContext
import com.dangerfield.goodtimes.libraries.goodtimes.SessionContext
import com.dangerfield.goodtimes.libraries.goodtimes.TimeContext
import com.dangerfield.goodtimes.libraries.goodtimes.UserRepository
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.SessionDao
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class GetAwarenessContextUseCaseImpl(
    private val userRepository: UserRepository,
    private val sessionDao: SessionDao,
    private val appCache: AppCache,
    private val clock: kotlin.time.Clock,
) : GetAwarenessContextUseCase {

    override suspend fun invoke(screenVisitCount: Int): AwarenessContext {
        val user = userRepository.getUser()
        val currentSession = sessionDao.getActiveSession() ?: sessionDao.getLatestSession()
        val allSessions = sessionDao.getAllSessions()
        val appData = appCache.get()
        
        // Build time context
        val now = clock.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val timeContext = TimeContext(
            hour = localDateTime.hour,
            dayOfWeek = localDateTime.dayOfWeek.ordinal + 1, // ordinal is 0-indexed, we want 1-7
            month = localDateTime.monthNumber,
            dayOfMonth = localDateTime.dayOfMonth,
            isWeekend = localDateTime.dayOfWeek == DayOfWeek.SATURDAY || 
                        localDateTime.dayOfWeek == DayOfWeek.SUNDAY,
        )
        
        // Build device context (currently limited - can expand with platform-specific providers)
        val deviceContext = DeviceContext.Unknown // TODO: Wire up platform-specific device info
        
        // Build session context
        val currentSessionDuration = currentSession?.let {
            now - it.startedAt
        } ?: Duration.ZERO
        
        val timeSinceLastSession = if (allSessions.size >= 2) {
            val previousSession = allSessions[1] // Most recent is current, second is previous
            previousSession.endedAt?.let { endedAt ->
                now - endedAt
            }
        } else null
        
        val sessionContext = SessionContext(
            sessionNumber = user?.sessionsCount ?: 1,
            currentSessionDuration = currentSessionDuration,
            timeSinceLastSession = timeSinceLastSession,
            totalTasksCompleted = user?.tasksCompleted ?: 0,
            totalAppOpens = user?.appOpenCount ?: 0,
            screenVisitCount = screenVisitCount,
        )
        
        // Build personality context from user traits + app cache data
        val uselessButtonClicks = appData.uselessButtonClicks
        val personalityContext = PersonalityContext(
            isNightOwl = user?.isNightOwl ?: false,
            isMorningPerson = user?.isMorningPerson ?: false,
            isMiddayRegular = user?.isMiddayRegular ?: false,
            isReluctantAdventurer = user?.isReluctantAdventurer ?: false,
            isCurious = user?.isCurious ?: false || uselessButtonClicks >= 3,
            isHesitant = user?.isHesitant ?: false,
            isVisual = user?.isVisual ?: false,
            isThoughtful = user?.isThoughtful ?: false,
            isPersistent = uselessButtonClicks >= 5,
            completedUselessButtonJourney = uselessButtonClicks >= 10,
            isCommitted = user?.isCommitted ?: false,
            isSelectivePlayer = user?.isSelectivePlayer ?: false,
            isWordsmith = user?.isWordsmith ?: false,
            isBrief = user?.isBrief ?: false,
        )
        
        // Build mood context
        val moodTrend = userRepository.getMoodTrend()
        val recentMoods = userRepository.getRecentMoods(5)
        val currentMood = currentSession?.mood?.let { Mood.valueOf(it) }
        
        // Calculate streak length (consecutive sessions with same mood direction)
        val streakLength = calculateMoodStreak(recentMoods)
        
        val moodContext = MoodContext(
            currentMood = currentMood,
            trend = moodTrend,
            recentMoods = recentMoods,
            streakLength = streakLength,
        )
        
        return AwarenessContext(
            time = timeContext,
            device = deviceContext,
            session = sessionContext,
            personality = personalityContext,
            mood = moodContext,
        )
    }
    
    /**
     * Calculate how many consecutive sessions have had the same mood "direction".
     * Direction is simplified to: LOW (BAD, LOW), NEUTRAL (OKAY, COMPLICATED), HIGH (GOOD, GREAT)
     */
    private fun calculateMoodStreak(moods: List<Mood>): Int {
        if (moods.isEmpty()) return 0
        
        val directions = moods.map { mood ->
            when (mood) {
                Mood.BAD, Mood.LOW -> -1
                Mood.OKAY, Mood.COMPLICATED -> 0
                Mood.GOOD, Mood.GREAT -> 1
            }
        }
        
        val firstDirection = directions.first()
        var streak = 1
        
        for (i in 1 until directions.size) {
            if (directions[i] == firstDirection) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }
}
