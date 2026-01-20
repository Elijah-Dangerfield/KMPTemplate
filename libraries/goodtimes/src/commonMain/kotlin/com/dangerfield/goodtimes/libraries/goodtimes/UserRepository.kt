package com.dangerfield.goodtimes.libraries.goodtimes

import kotlinx.coroutines.flow.Flow

/**
 * Central repository for all user data and behavioral signals.
 * 
 * This consolidates access to:
 * - User profile and personality scores
 * - Behavioral signals and derived traits
 * - Mood trends (computed from sessions)
 * - Task state (current task, routing effects)
 * 
 * Use this repository instead of accessing UserDao/UserCache directly.
 * 
 * Updates flow through specific methods that handle all side effects:
 * - onTaskCompleted() - updates scores, increments counts
 * - onSessionStarted() - tracks time-of-day patterns
 * - onUserAction() - records specific behavioral signals
 */
interface UserRepository {
    
    // =========================================================================
    // INITIALIZATION
    // =========================================================================
    
    /** Ensure user entity exists in database. Creates one if not present. */
    suspend fun ensureUserExists()
    
    // =========================================================================
    // OBSERVE
    // =========================================================================
    
    /** Observe the user (reactive, emits on changes) */
    fun observeUser(): Flow<User?>
    
    // =========================================================================
    // READ
    // =========================================================================
    
    /** Get current user snapshot */
    suspend fun getUser(): User?
    
    /** Get mood trend computed from recent sessions */
    suspend fun getMoodTrend(): MoodTrend
    
    /** Get recent moods (from sessions) */
    suspend fun getRecentMoods(limit: Int = 5): List<Mood>
    
    // =========================================================================
    // USER PROFILE
    // =========================================================================
    
    /** Set the user's name */
    suspend fun setName(name: String?)
    
    /** Mark that we've asked the user for their name */
    suspend fun setHasBeenAskedForName(asked: Boolean)
    
    // =========================================================================
    // TASK STATE
    // =========================================================================
    
    /** Set the current task the user is working on */
    suspend fun setCurrentTaskId(taskId: String?)
    
    /** Clear the current task */
    suspend fun clearCurrentTaskId()
    
    /** Set routing effects from a ROUTING task */
    suspend fun setRoutingEffects(effectsJson: String?)
    
    /** Clear routing effects */
    suspend fun clearRoutingEffects()
    
    // =========================================================================
    // TASK COMPLETION SIGNALS
    // =========================================================================
    
    /**
     * Record that the user completed a task.
     * 
     * This updates:
     * - tasksCompleted count
     * - Personality scores based on signals
     * - Response style affinities based on response type
     * - Session-level task count
     * - Text task stats (if isTextTask and characterCount provided)
     */
    suspend fun onTaskCompleted(
        taskId: String,
        signals: List<Signal>,
        responseTimeMs: Long,
        characterCount: Int?,
        isTextTask: Boolean = false,
    )
    
    /**
     * Record that the user skipped a task.
     * 
     * This updates:
     * - tasksSkipped count
     * - Potentially personality scores (e.g., social comfort if skipping social tasks)
     */
    suspend fun onTaskSkipped(taskId: String, signals: List<Signal>)
    
    // =========================================================================
    // SESSION SIGNALS
    // =========================================================================
    
    /**
     * Record that a new session started.
     * 
     * This tracks time-of-day patterns (night owl, morning person, etc.)
     * Called by SessionRepository when creating a new session.
     */
    suspend fun onSessionStarted(hour: Int)
    
    /**
     * Record that the user entered an idle session (opened but did nothing).
     */
    suspend fun onIdleSession()
    
    /**
     * Record that the user did a quick exit (closed within 10s of opening).
     */
    suspend fun onQuickExit()
    
    // =========================================================================
    // BEHAVIORAL SIGNALS
    // =========================================================================
    
    /** Increment app open count */
    suspend fun onAppOpened()
    
    /** Increment settings open count */
    suspend fun onSettingsOpened()
    
    /** Increment about page open count */
    suspend fun onAboutOpened()
    
    /** Record a "no" click during onboarding */
    suspend fun onOnboardingNoClick()
    
    /** Increment bug report count */
    suspend fun onBugReported()
    
    /** Increment permission denial count */
    suspend fun onPermissionDenied()
    
    /** Increment shake count */
    suspend fun onShakeDetected()
    
    /** Record optional media added (photo/video/audio beyond required) */
    suspend fun onOptionalMediaAdded()
    
    /** Record that a task with optional media opportunity was encountered */
    suspend fun onOptionalMediaOpportunity()
    
    /** Record that user deleted and rewrote their response */
    suspend fun onDeleteAndRewrite()
    
    /** Update average hesitation time (time before user starts typing/responding) */
    suspend fun onResponseHesitation(hesitationMs: Long)
    
    // =========================================================================
    // FLAGS
    // =========================================================================
    
    /** Mark onboarding as complete */
    suspend fun setOnboardingComplete()
    
    /** Mark that we've asked about social skips */
    suspend fun setHasAskedAboutSocialSkips()
    
    /** Mark that we've shown the declining mood routing */
    suspend fun setHasSeenDecliningMoodRouting()
    
    /** Mark that we've shown the stop asking routing */
    suspend fun setHasSeenStopAskingRouting()
    
    // =========================================================================
    // RESET
    // =========================================================================
    
    /** Delete all user data (Fresh Start) */
    suspend fun deleteAll()
}

/**
 * Signals emitted from task completion that affect user scores.
 */
data class Signal(
    val dimension: ScoreDimension,
    val delta: Int, // positive or negative adjustment
)

enum class ScoreDimension {
    // Personality
    SOCIAL_COMFORT,
    OPENNESS,
    PLAYFULNESS,
    PATIENCE,
    REFLECTION_DEPTH,
    
    // Response style
    WRITING_AFFINITY,
    PHOTO_AFFINITY,
    AUDIO_AFFINITY,
    DRAWING_AFFINITY,
    GAME_AFFINITY,
}
