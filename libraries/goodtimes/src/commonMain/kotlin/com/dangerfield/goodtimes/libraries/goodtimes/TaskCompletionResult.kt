package com.dangerfield.goodtimes.libraries.goodtimes

/**
 * Result of completing a task screen.
 * Passed back to the session controller to save and determine next task.
 */
data class TaskCompletionResult(
    val taskId: String,
    val outcome: TaskOutcome,
    val response: TaskResponse?,
    val followUpResult: FollowUpResult?,
    val timeSpentMs: Long,
    val signals: List<TaskSignal>,
)

enum class TaskOutcome {
    COMPLETED,
    SKIPPED_PERMANENT,
    SKIPPED_RESCHEDULE,
}

/**
 * User's response to a task. Sealed to ensure type safety.
 */
sealed class TaskResponse {
    data class Text(val value: String) : TaskResponse()
    data class Selection(val selected: List<String>) : TaskResponse()
    data class Media(val type: MediaType, val filePath: String) : TaskResponse()
    data class Drawing(val filePath: String) : TaskResponse()
    data class Compound(val text: String?, val media: List<Media>?) : TaskResponse()
    data object None : TaskResponse()
}

enum class MediaType {
    PHOTO, AUDIO
}

/**
 * A behavioral signal captured during task completion.
 */
data class TaskSignal(
    val dimension: String,
    val adjustment: Int,
    val reason: String? = null,
)
