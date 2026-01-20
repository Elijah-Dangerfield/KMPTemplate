package com.dangerfield.goodtimes.libraries.goodtimes

import kotlin.time.Duration

/**
 * Domain model representing everything we know about the user.
 * 
 * This is a unified view combining:
 * - Basic profile info (name)
 * - Personality scores (social comfort, openness, etc.)
 * - Response style affinities (writing, photo, audio, etc.)
 * - Behavioral signals (counts for various interactions)
 * - Derived traits (computed booleans like isNightOwl, isMorningPerson)
 * - Flags (one-time prompts, routing states)
 * - Stats (sessions count, tasks completed/skipped)
 * 
 * This model is read-only. Use UserRepository methods to update.
 */
data class User(
    // =========================================================================
    // BASIC INFO
    // =========================================================================
    
    val name: String?,
    val nameSetAt: Long?, // epoch millis - when name was first set
    val nameUpdatedAt: Long?, // epoch millis - when name was last changed
    val createdAt: Long, // epoch millis
    val lastSessionAt: Long?, // epoch millis
    
    // =========================================================================
    // PERSONALITY SCORES (0-100, start at 50)
    // =========================================================================
    
    val socialComfort: Int,
    val openness: Int,
    val playfulness: Int,
    val patience: Int,
    val reflectionDepth: Int,
    
    // =========================================================================
    // RESPONSE STYLE AFFINITIES (0-100, start at 50)
    // =========================================================================
    
    val writingAffinity: Int,
    val photoAffinity: Int,
    val audioAffinity: Int,
    val drawingAffinity: Int,
    val gameAffinity: Int,
    
    // =========================================================================
    // FLAGS (one-time states)
    // =========================================================================
    
    val hasCompletedOnboarding: Boolean,
    val hasCompletedIntroTask: Boolean,
    val hasBeenAskedAboutSocialSkips: Boolean,
    val hasBeenAskedForName: Boolean,
    val hasSeenDecliningMoodRouting: Boolean,
    val hasSeenStopAskingRouting: Boolean,
    
    // =========================================================================
    // TASK STATE
    // =========================================================================
    
    val currentTaskId: String?,
    val routingEffectsJson: String?,
    
    // =========================================================================
    // STATS
    // =========================================================================
    
    val sessionsCount: Int,
    val tasksCompleted: Int,
    val tasksSkipped: Int,
    
    // =========================================================================
    // BEHAVIORAL SIGNALS (raw counts)
    // =========================================================================
    
    val appOpenCount: Int,
    val settingsOpenCount: Int,
    val aboutOpenCount: Int,
    val noClickCountOnboarding: Int,
    val bugReportCount: Int,
    val permissionDenialCount: Int,
    val backButtonPressCount: Int,
    val shakeCount: Int,
    val lateNightSessionCount: Int,
    val morningSessionCount: Int,
    val middaySessionCount: Int,
    val averageHesitationMs: Long?,
    val optionalMediaAddedCount: Int,
    val deleteAndRewriteCount: Int,
    val quickExitCount: Int,
    val idleSessionCount: Int,
    
    // New fields for ratio-based traits
    val optionalMediaOpportunities: Int, // Tasks where optional media was available
    val textTasksCompleted: Int, // Tasks requiring text responses
    val totalTextLength: Int, // Cumulative characters typed in text responses
) {
    // =========================================================================
    // DERIVED TRAITS (computed from behavioral signals using ratios)
    // =========================================================================
    
    /** Average text response length in characters */
    val averageTextLength: Int get() = 
        if (textTasksCompleted > 0) totalTextLength / textTasksCompleted else 0
    
    /** Ratio of sessions that occurred late at night (midnight-4am) */
    private val lateNightRatio: Float get() = 
        if (sessionsCount > 0) lateNightSessionCount.toFloat() / sessionsCount else 0f
    
    /** Ratio of sessions that occurred in the morning (6am-10am) */
    private val morningRatio: Float get() = 
        if (sessionsCount > 0) morningSessionCount.toFloat() / sessionsCount else 0f
    
    /** Ratio of sessions that occurred midday (11am-2pm) */
    private val middayRatio: Float get() = 
        if (sessionsCount > 0) middaySessionCount.toFloat() / sessionsCount else 0f
    
    /** Completion rate (completed / total attempts) */
    val completionRate: Float get() {
        val total = tasksCompleted + tasksSkipped
        return if (total > 0) tasksCompleted.toFloat() / total else 0f
    }
    
    /** Ratio of optional media opportunities where user added media */
    val mediaAddedRate: Float get() = 
        if (optionalMediaOpportunities > 0) optionalMediaAddedCount.toFloat() / optionalMediaOpportunities else 0f
    
    // =========================================================================
    // TRAIT BOOLEANS (meaningful thresholds)
    // =========================================================================
    
    /** >30% of sessions are after midnight - real night owl behavior */
    val isNightOwl: Boolean get() = sessionsCount >= 5 && lateNightRatio > 0.3f
    
    /** >30% of sessions are morning - consistent early bird */
    val isMorningPerson: Boolean get() = sessionsCount >= 5 && morningRatio > 0.3f
    
    /** >30% of sessions are midday - lunch break regular */
    val isMiddayRegular: Boolean get() = sessionsCount >= 5 && middayRatio > 0.3f
    
    /** Clicked "no" multiple times during onboarding but eventually said yes */
    val isReluctantAdventurer: Boolean get() = noClickCountOnboarding >= 3
    
    /** Opens settings frequently relative to sessions - likes to explore */
    val isCurious: Boolean get() = sessionsCount >= 5 && settingsOpenCount.toFloat() / sessionsCount > 0.3f
    
    /** Opens app but often doesn't complete tasks - pattern of hesitancy */
    val isHesitant: Boolean get() = sessionsCount >= 5 && idleSessionCount.toFloat() / sessionsCount > 0.3f
    
    /** Adds optional media >40% of the time when available */
    val isVisual: Boolean get() = optionalMediaOpportunities >= 5 && mediaAddedRate > 0.4f
    
    /** Takes time before responding (high hesitation) or rewrites often */
    val isThoughtful: Boolean get() = 
        (averageHesitationMs ?: 0) > 30_000 || (textTasksCompleted >= 3 && deleteAndRewriteCount.toFloat() / textTasksCompleted > 0.2f)
    
    /** >80% completion rate with 10+ tasks - genuinely committed */
    val isCommitted: Boolean get() = tasksCompleted >= 10 && completionRate > 0.8f
    
    /** <50% completion rate with 10+ attempts - selective about engagement */
    val isSelectivePlayer: Boolean get() = (tasksCompleted + tasksSkipped) >= 10 && completionRate < 0.5f
    
    /** Has completed 50+ tasks */
    val isVeteran: Boolean get() = tasksCompleted >= 50
    
    /** Fewer than 3 sessions */
    val isNewUser: Boolean get() = sessionsCount <= 3
    
    /** Writes long responses (avg >150 chars) - expressive writer */
    val isWordsmith: Boolean get() = textTasksCompleted >= 5 && averageTextLength > 150
    
    /** Writes short responses (avg <50 chars) - brief communicator */
    val isBrief: Boolean get() = textTasksCompleted >= 5 && averageTextLength < 50
}
