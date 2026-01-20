package com.dangerfield.goodtimes.features.home.impl

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Determines when mood prompts should appear.
 * 
 * This is designed to be flexible - the timing strategy can be changed easily
 * without touching the ViewModel logic. All mood prompt timing decisions live here.
 * 
 * Current strategy:
 * - First session ever: Wait for 3-5 tasks (let them settle in, experience the app)
 * - Returning sessions: Wait for 1-3 tasks before mood prompt
 * - Don't ask more than once every 6 hours
 */
class MoodPromptTiming(
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default
) {
    
    /**
     * The minimum number of tasks to complete before allowing a mood prompt.
     * Set based on whether this is the user's first session.
     */
    private var tasksRequiredBeforeMood: Int = pickRandomThreshold(isFirstSession = false)
    
    /**
     * Resets the timing for a new session.
     * 
     * @param isFirstSession True if this is the user's very first session ever.
     *        First session gets a longer delay to let them settle into the app.
     */
    fun resetForNewSession(isFirstSession: Boolean) {
        tasksRequiredBeforeMood = pickRandomThreshold(isFirstSession)
    }
    
    /**
     * Returns true if all timing conditions are met to show a mood prompt.
     * 
     * @param tasksInteractedWith Total tasks the user has interacted with (completed + skipped)
     * @param lastMoodInteractionAt Epoch millis when user last answered/dismissed mood, or null if never
     */
    fun shouldAllowMoodPrompt(
        tasksInteractedWith: Int,
        lastMoodInteractionAt: Long?
    ): Boolean {
        // Check if enough tasks have been completed
        if (tasksInteractedWith < tasksRequiredBeforeMood) return false
        
        // Check if enough time has passed since last mood interaction
        if (!hasEnoughTimePassed(lastMoodInteractionAt)) return false
        
        return true
    }
    
    private fun hasEnoughTimePassed(lastMoodInteractionAt: Long?): Boolean {
        if (lastMoodInteractionAt == null) return true
        
        val elapsed = clock.now().toEpochMilliseconds() - lastMoodInteractionAt
        return elapsed >= MIN_TIME_BETWEEN_PROMPTS.inWholeMilliseconds
    }
    
    private fun pickRandomThreshold(isFirstSession: Boolean): Int {
        return if (isFirstSession) {
            // First session: let them experience the app before asking about mood
            random.nextInt(FIRST_SESSION_MIN_TASKS, FIRST_SESSION_MAX_TASKS + 1)
        } else {
            // Returning sessions: they know what to expect
            random.nextInt(MIN_TASKS_BEFORE_MOOD, MAX_TASKS_BEFORE_MOOD + 1)
        }
    }
    
    companion object {
        // ============================================================
        // FIRST SESSION - Let them settle in before asking about mood
        // ============================================================
        
        /**
         * Minimum tasks before mood prompt on first session ever.
         * Higher than normal because we want them to experience the app first.
         */
        private const val FIRST_SESSION_MIN_TASKS = 3
        
        /**
         * Maximum tasks before mood prompt on first session (inclusive).
         */
        private const val FIRST_SESSION_MAX_TASKS = 5
        
        // ============================================================
        // RETURNING SESSIONS - They know the app, can ask sooner
        // ============================================================
        
        /**
         * Minimum number of tasks before we consider showing mood prompt.
         */
        private const val MIN_TASKS_BEFORE_MOOD = 1
        
        /**
         * Maximum number of tasks before mood prompt (inclusive).
         * The actual threshold is randomly chosen between MIN and MAX.
         */
        private const val MAX_TASKS_BEFORE_MOOD = 3
        
        // ============================================================
        // TIME GAP - Don't ask too frequently across sessions
        // ============================================================
        
        /**
         * Minimum time between mood prompts.
         * We don't want to repeatedly ask across quick successive sessions.
         */
        private val MIN_TIME_BETWEEN_PROMPTS = 6.hours
    }
}
