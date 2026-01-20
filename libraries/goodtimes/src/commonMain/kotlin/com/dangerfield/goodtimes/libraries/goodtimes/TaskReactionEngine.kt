package com.dangerfield.goodtimes.libraries.goodtimes



/**
 * Engine that decides whether and how to react to task completion.
 * 
 * Reactions are brief, surprising moments where the app acknowledges
 * something specific about how the user completed a task. They make the
 * app feel alive without being annoying.
 * 
 * Key principles:
 * - Low probability (~10-20% of completions get a reaction)
 * - Context-aware (considers mood, time of day, recent patterns)
 * - Non-repetitive (tracks recently used reactions)
 * - Genuine (never sarcastic about real struggles)
 */
interface TaskReactionEngine {
    /**
     * Consider whether to react to a task completion.
     * 
     * @param result The completion result including signals, response, time spent
     * @param context Context about the user and current session
     * @return A Reaction if one is warranted, null otherwise
     */
    suspend fun considerReaction(
        result: TaskCompletionResult,
        context: ReactionContext,
    ): Reaction?
}

/**
 * Context provided to the reaction engine for making decisions.
 */
data class ReactionContext(
    val task: Task,
    val sessionNumber: Int,
    val tasksCompletedThisSession: Int,
    val totalTasksCompleted: Int,
    val skipsThisSession: Int = 0,
    val consecutiveSkips: Int = 0,
    val currentMood: Mood?,
    val isLateNight: Boolean,
    val isFirstSession: Boolean,
    val recentReactionIds: List<String>,
)
