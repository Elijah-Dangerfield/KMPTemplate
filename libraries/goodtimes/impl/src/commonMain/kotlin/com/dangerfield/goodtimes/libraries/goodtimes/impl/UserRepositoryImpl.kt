package com.dangerfield.goodtimes.libraries.goodtimes.impl

import com.dangerfield.goodtimes.libraries.core.logging.KLog
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.MoodTrend
import com.dangerfield.goodtimes.libraries.goodtimes.ScoreDimension
import com.dangerfield.goodtimes.libraries.goodtimes.Signal
import com.dangerfield.goodtimes.libraries.goodtimes.User
import com.dangerfield.goodtimes.libraries.goodtimes.UserRepository
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.SessionDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.UserDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val clock: Clock,
) : UserRepository {
    
    private val logger = KLog.withTag("UserRepository")
    
    // =========================================================================
    // INITIALIZATION
    // =========================================================================
    
    override suspend fun ensureUserExists() {
        if (userDao.getUser() != null) return
        
        logger.i("Creating new user entity")
        val now = clock.now().toEpochMilliseconds()
        userDao.insert(
            UserEntity(
                id = "user",
                name = null,
                nameSetAt = null,
                nameUpdatedAt = null,
                createdAt = now,
                lastSessionAt = null,
                currentTaskId = null,
                routingEffectsJson = null,
                averageHesitationMs = null,
            )
        )
    }
    
    // =========================================================================
    // OBSERVE
    // =========================================================================
    
    override fun observeUser(): Flow<User?> = userDao.observeUser().map { it?.toDomain() }
    
    // =========================================================================
    // READ
    // =========================================================================
    
    override suspend fun getUser(): User? = userDao.getUser()?.toDomain()
    
    override suspend fun getMoodTrend(): MoodTrend {
        val recentSessions = sessionDao.getRecentSessionsWithMood(5)
        if (recentSessions.size < 2) return MoodTrend.UNKNOWN
        
        val moods = recentSessions.mapNotNull { session ->
            session.mood?.let { Mood.valueOf(it) }
        }
        
        if (moods.size < 2) return MoodTrend.UNKNOWN
        
        // Compare recent moods to older moods
        val recentScore = moods.take(2).map { it.score }.average()
        val olderScore = moods.drop(2).map { it.score }.average()
        
        return when {
            olderScore.isNaN() -> MoodTrend.UNKNOWN
            recentScore - olderScore >= 0.5 -> MoodTrend.IMPROVING
            olderScore - recentScore >= 0.5 -> MoodTrend.DECLINING
            else -> MoodTrend.STABLE
        }
    }
    
    override suspend fun getRecentMoods(limit: Int): List<Mood> {
        return sessionDao.getRecentSessionsWithMood(limit)
            .mapNotNull { it.mood?.let { m -> Mood.valueOf(m) } }
    }
    
    // =========================================================================
    // USER PROFILE
    // =========================================================================
    
    override suspend fun setName(name: String?) {
        val now = clock.now().toEpochMilliseconds()
        val user = userDao.getUser()
        
        // First time setting name
        if (user?.nameSetAt == null && name != null) {
            userDao.setNameFirstTime(name, now)
        } else {
            userDao.updateName(name, now)
        }
    }
    
    override suspend fun setHasBeenAskedForName(asked: Boolean) {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(hasBeenAskedForName = asked))
    }
    
    // =========================================================================
    // TASK STATE
    // =========================================================================
    
    override suspend fun setCurrentTaskId(taskId: String?) {
        userDao.setCurrentTaskId(taskId)
    }
    
    override suspend fun clearCurrentTaskId() {
        userDao.setCurrentTaskId(null)
    }
    
    override suspend fun setRoutingEffects(effectsJson: String?) {
        userDao.setRoutingEffects(effectsJson)
    }
    
    override suspend fun clearRoutingEffects() {
        userDao.setRoutingEffects(null)
    }
    
    // =========================================================================
    // TASK COMPLETION SIGNALS
    // =========================================================================
    
    override suspend fun onTaskCompleted(
        taskId: String,
        signals: List<Signal>,
        responseTimeMs: Long,
        characterCount: Int?,
        isTextTask: Boolean,
    ) {
        val userBefore = userDao.getUser()
        userDao.incrementTasksCompleted()
        
        logger.i("📝 Task Completed: $taskId") {
            "responseTimeMs" to responseTimeMs
            "characterCount" to characterCount
            "isTextTask" to isTextTask
            "signals" to signals.map { "${it.dimension.name}: ${if (it.delta >= 0) "+" else ""}${it.delta}" }
        }
        
        // Track text task stats
        if (isTextTask && characterCount != null) {
            val user = userDao.getUser() ?: return
            val avgBefore = if (user.textTasksCompleted > 0) user.totalTextLength / user.textTasksCompleted else 0
            userDao.update(user.copy(
                textTasksCompleted = user.textTasksCompleted + 1,
                totalTextLength = user.totalTextLength + characterCount
            ))
            val avgAfter = (user.totalTextLength + characterCount) / (user.textTasksCompleted + 1)
            logger.d("   Text stats: ${user.textTasksCompleted + 1} tasks, avg length $avgBefore → $avgAfter chars")
        }
        
        // Apply personality/affinity signals
        val user = userDao.getUser() ?: return
        var updatedUser = user
        
        val changes = mutableListOf<String>()
        for (signal in signals) {
            val before = updatedUser.getScoreFor(signal.dimension)
            updatedUser = updatedUser.applySignal(signal)
            val after = updatedUser.getScoreFor(signal.dimension)
            if (before != after) {
                changes.add("   ${signal.dimension.name}: $before → $after (${if (signal.delta >= 0) "+" else ""}${signal.delta})")
            }
        }
        
        userDao.update(updatedUser)
        
        if (changes.isNotEmpty()) {
            logger.i("🧠 Personality Updates:\n${changes.joinToString("\n")}")
        }
        
        // Log summary of current personality state
        logPersonalitySummary(updatedUser)
    }
    
    override suspend fun onTaskSkipped(taskId: String, signals: List<Signal>) {
        userDao.incrementTasksSkipped()
        
        logger.i("⏭️ Task Skipped: $taskId") {
            "signals" to signals.map { "${it.dimension.name}: ${if (it.delta >= 0) "+" else ""}${it.delta}" }
        }
        
        // Apply any signals from skipping
        val user = userDao.getUser() ?: return
        var updatedUser = user
        
        val changes = mutableListOf<String>()
        for (signal in signals) {
            val before = updatedUser.getScoreFor(signal.dimension)
            updatedUser = updatedUser.applySignal(signal)
            val after = updatedUser.getScoreFor(signal.dimension)
            if (before != after) {
                changes.add("   ${signal.dimension.name}: $before → $after (${if (signal.delta >= 0) "+" else ""}${signal.delta})")
            }
        }
        
        userDao.update(updatedUser)
        
        if (changes.isNotEmpty()) {
            logger.i("🧠 Personality Updates (from skip):\n${changes.joinToString("\n")}")
        }
    }
    
    private fun UserEntity.getScoreFor(dimension: ScoreDimension): Int = when (dimension) {
        ScoreDimension.SOCIAL_COMFORT -> socialComfort
        ScoreDimension.OPENNESS -> openness
        ScoreDimension.PLAYFULNESS -> playfulness
        ScoreDimension.PATIENCE -> patience
        ScoreDimension.REFLECTION_DEPTH -> reflectionDepth
        ScoreDimension.WRITING_AFFINITY -> writingAffinity
        ScoreDimension.PHOTO_AFFINITY -> photoAffinity
        ScoreDimension.AUDIO_AFFINITY -> audioAffinity
        ScoreDimension.DRAWING_AFFINITY -> drawingAffinity
        ScoreDimension.GAME_AFFINITY -> gameAffinity
    }
    
    private fun logPersonalitySummary(user: UserEntity) {
        logger.d("""
            |📊 Current Profile (tasks: ${user.tasksCompleted}, skipped: ${user.tasksSkipped}):
            |   Personality: social=${user.socialComfort}, open=${user.openness}, playful=${user.playfulness}, patient=${user.patience}, reflective=${user.reflectionDepth}
            |   Affinities: write=${user.writingAffinity}, photo=${user.photoAffinity}, audio=${user.audioAffinity}, draw=${user.drawingAffinity}, game=${user.gameAffinity}
        """.trimMargin())
    }
    
    private fun UserEntity.applySignal(signal: Signal): UserEntity {
        val newValue = { current: Int -> (current + signal.delta).coerceIn(0, 100) }
        
        return when (signal.dimension) {
            ScoreDimension.SOCIAL_COMFORT -> copy(socialComfort = newValue(socialComfort))
            ScoreDimension.OPENNESS -> copy(openness = newValue(openness))
            ScoreDimension.PLAYFULNESS -> copy(playfulness = newValue(playfulness))
            ScoreDimension.PATIENCE -> copy(patience = newValue(patience))
            ScoreDimension.REFLECTION_DEPTH -> copy(reflectionDepth = newValue(reflectionDepth))
            ScoreDimension.WRITING_AFFINITY -> copy(writingAffinity = newValue(writingAffinity))
            ScoreDimension.PHOTO_AFFINITY -> copy(photoAffinity = newValue(photoAffinity))
            ScoreDimension.AUDIO_AFFINITY -> copy(audioAffinity = newValue(audioAffinity))
            ScoreDimension.DRAWING_AFFINITY -> copy(drawingAffinity = newValue(drawingAffinity))
            ScoreDimension.GAME_AFFINITY -> copy(gameAffinity = newValue(gameAffinity))
        }
    }
    
    // =========================================================================
    // SESSION SIGNALS
    // =========================================================================
    
    override suspend fun onSessionStarted(hour: Int) {
        userDao.incrementSessionCount(clock.now().toEpochMilliseconds())
        
        // Track time-of-day patterns
        when (hour) {
            in 0..4 -> userDao.incrementLateNightSessionCount()
            in 6..10 -> userDao.incrementMorningSessionCount()
            in 11..14 -> userDao.incrementMiddaySessionCount()
        }
    }
    
    override suspend fun onIdleSession() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(idleSessionCount = user.idleSessionCount + 1))
    }
    
    override suspend fun onQuickExit() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(quickExitCount = user.quickExitCount + 1))
    }
    
    // =========================================================================
    // BEHAVIORAL SIGNALS
    // =========================================================================
    
    override suspend fun onAppOpened() {
        userDao.incrementAppOpenCount()
    }
    
    override suspend fun onSettingsOpened() {
        userDao.incrementSettingsOpenCount()
    }
    
    override suspend fun onAboutOpened() {
        userDao.incrementAboutOpenCount()
    }
    
    override suspend fun onOnboardingNoClick() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(noClickCountOnboarding = user.noClickCountOnboarding + 1))
    }
    
    override suspend fun onBugReported() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(bugReportCount = user.bugReportCount + 1))
    }
    
    override suspend fun onPermissionDenied() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(permissionDenialCount = user.permissionDenialCount + 1))
    }
    
    override suspend fun onShakeDetected() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(shakeCount = user.shakeCount + 1))
    }
    
    override suspend fun onOptionalMediaAdded() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(optionalMediaAddedCount = user.optionalMediaAddedCount + 1))
    }
    
    override suspend fun onOptionalMediaOpportunity() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(optionalMediaOpportunities = user.optionalMediaOpportunities + 1))
    }
    
    override suspend fun onDeleteAndRewrite() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(deleteAndRewriteCount = user.deleteAndRewriteCount + 1))
    }
    
    override suspend fun onResponseHesitation(hesitationMs: Long) {
        val user = userDao.getUser() ?: return
        
        // Rolling average of hesitation time
        val currentAvg = user.averageHesitationMs ?: hesitationMs
        val newAvg = (currentAvg + hesitationMs) / 2
        
        userDao.update(user.copy(averageHesitationMs = newAvg))
    }
    
    // =========================================================================
    // FLAGS
    // =========================================================================
    
    override suspend fun setOnboardingComplete() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(hasCompletedOnboarding = true))
    }
    
    override suspend fun setHasAskedAboutSocialSkips() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(hasBeenAskedAboutSocialSkips = true))
    }
    
    override suspend fun setHasSeenDecliningMoodRouting() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(hasSeenDecliningMoodRouting = true))
    }
    
    override suspend fun setHasSeenStopAskingRouting() {
        val user = userDao.getUser() ?: return
        userDao.update(user.copy(hasSeenStopAskingRouting = true))
    }
    
    // =========================================================================
    // RESET
    // =========================================================================
    
    override suspend fun deleteAll() {
        userDao.deleteAll()
        sessionDao.deleteAllSessions()
    }
    
    // =========================================================================
    // MAPPING
    // =========================================================================
    
    private fun UserEntity.toDomain(): User = User(
        name = name,
        nameSetAt = nameSetAt,
        nameUpdatedAt = nameUpdatedAt,
        createdAt = createdAt,
        lastSessionAt = lastSessionAt,
        socialComfort = socialComfort,
        openness = openness,
        playfulness = playfulness,
        patience = patience,
        reflectionDepth = reflectionDepth,
        writingAffinity = writingAffinity,
        photoAffinity = photoAffinity,
        audioAffinity = audioAffinity,
        drawingAffinity = drawingAffinity,
        gameAffinity = gameAffinity,
        hasCompletedOnboarding = hasCompletedOnboarding,
        hasCompletedIntroTask = hasCompletedIntroTask,
        hasBeenAskedAboutSocialSkips = hasBeenAskedAboutSocialSkips,
        hasBeenAskedForName = hasBeenAskedForName,
        hasSeenDecliningMoodRouting = hasSeenDecliningMoodRouting,
        hasSeenStopAskingRouting = hasSeenStopAskingRouting,
        currentTaskId = currentTaskId,
        routingEffectsJson = routingEffectsJson,
        sessionsCount = sessionsCount,
        tasksCompleted = tasksCompleted,
        tasksSkipped = tasksSkipped,
        appOpenCount = appOpenCount,
        settingsOpenCount = settingsOpenCount,
        aboutOpenCount = aboutOpenCount,
        noClickCountOnboarding = noClickCountOnboarding,
        bugReportCount = bugReportCount,
        permissionDenialCount = permissionDenialCount,
        backButtonPressCount = backButtonPressCount,
        shakeCount = shakeCount,
        lateNightSessionCount = lateNightSessionCount,
        morningSessionCount = morningSessionCount,
        middaySessionCount = middaySessionCount,
        averageHesitationMs = averageHesitationMs,
        optionalMediaAddedCount = optionalMediaAddedCount,
        deleteAndRewriteCount = deleteAndRewriteCount,
        quickExitCount = quickExitCount,
        idleSessionCount = idleSessionCount,
        optionalMediaOpportunities = optionalMediaOpportunities,
        textTasksCompleted = textTasksCompleted,
        totalTextLength = totalTextLength,
    )
    
    /** Map Mood to a numeric score for trend calculation */
    private val Mood.score: Double get() = when (this) {
        Mood.GREAT -> 5.0
        Mood.GOOD -> 4.0
        Mood.OKAY -> 3.0
        Mood.LOW -> 2.0
        Mood.BAD -> 1.0
        Mood.COMPLICATED -> 2.5 // Somewhere in the middle
    }
}
