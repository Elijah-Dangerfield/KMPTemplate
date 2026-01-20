package com.dangerfield.goodtimes.libraries.goodtimes

/**
 * A task is an invitation to do something.
 * 
 * Tasks are static definitions shipped with the app. The user's progress
 * and responses are tracked separately.
 *
 * Thoughts for cleanup:
 * What's up with the duration here? I think the domain model can be shaped differently than the DB row
 * obviously we need a flat boy for the DB but here we can probably make task a sealed class or use
 * some type of inheritance system to clean this up
 *
 * Also why doesnt task conditions contain best for moods and avoid for moods? DO we even need task condition? Seems
 * kinda blury what its purpose is.
 *
 */
data class Task(
    val id: String,
    val type: TaskType,
    val categories: List<TaskCategory>,
    val difficulty: Difficulty,
    val instruction: String,
    val requiresSocial: Boolean,
    val bestForMoods: List<Mood>?,
    val avoidForMoods: List<Mood>?,
    val safeToReflect: Boolean,
    val responseStyle: ResponseStyle,
    val conditions: TaskConditions?,
    val assets: TaskAssets?,
    val followUp: FollowUpConfig?,
    // Depth requirements
    val requiresDepth: Boolean = false,      // If true, short answers trigger follow-up
    val minCharacters: Int? = null,          // Minimum characters for depth (default 20 if requiresDepth)
    val isIntroTask: Boolean = false,        // If true, this is shown first to new users
    // Type-specific fields
    val placeholder: String?,
    val durationSeconds: Int?,
    val selectionOptions: List<String>?,
    val minSelections: Int?,
    val maxSelections: Int?,
    val routingOptions: List<RoutingOption>?,
    val requireFrontCamera: Boolean?,
)

data class ResponseStyle(
    val allowsText: Boolean = false,
    val allowsPhoto: Boolean = false,
    val allowsAudio: Boolean = false,
    val allowsDrawing: Boolean = false,
    val expectedLength: ExpectedLength = ExpectedLength.MEDIUM,
)

/**
 * How much text input this task expects.
 * Used to calibrate writing affinity signals - a one-word answer to
 * "favorite word" is perfect, but a one-word answer to "describe the sky" is lazy.
 */
enum class ExpectedLength(val minChars: Int, val goodChars: Int, val greatChars: Int) {
    /** Single word or very short phrase (e.g., "What's your favorite word?") */
    WORD(1, 5, 20),
    /** A few words to a sentence (e.g., "What's something you noticed today?") */
    SHORT(10, 30, 80),
    /** A sentence or two (e.g., "Tell me about your day") */
    MEDIUM(20, 80, 200),
    /** A paragraph or more (e.g., "Describe the sky like explaining to someone who's never seen one") */
    LONG(50, 150, 400),
}

data class TaskConditions(
    val timeAfter: String?,  // "22:00"
    val timeBefore: String?, // "05:00"
    val minDaysSinceLastSession: Int?,
    val requiresMoodTrend: MoodTrend?,
    val monthRange: MonthRange?, // e.g., December only, or Nov-Jan for "end of year"
    val dayOfMonthRange: DayRange?, // e.g., 1-7 for "first week of month"
)

/**
 * A range of months (1-12) when a task should be available.
 * Supports wrap-around (e.g., Nov-Feb for winter).
 */
data class MonthRange(
    val startMonth: Int, // 1-12
    val endMonth: Int,   // 1-12
) {
    fun contains(month: Int): Boolean {
        return if (startMonth <= endMonth) {
            month in startMonth..endMonth
        } else {
            // Wrap around (e.g., Nov-Feb = 11-2)
            month >= startMonth || month <= endMonth
        }
    }
}

/**
 * A range of days within a month.
 */
data class DayRange(
    val startDay: Int, // 1-31
    val endDay: Int,   // 1-31
) {
    fun contains(day: Int): Boolean = day in startDay..endDay
}

data class TaskAssets(
    val imagePath: String?,
    val backgroundImagePath: String?,
    val accentColor: String?,
)

/**
 * Configuration for what happens after a task is completed.
 * Follow-ups can chain to gather more context about the experience.
 */
data class FollowUpConfig(
    val type: FollowUpType,
    val required: Boolean = false,  // If false, app decides based on context (completion time, mood, etc.)
    val ifYes: FollowUpConfig? = null,  // For DID_YOU type, what to show if they did it
    val options: List<FollowUpOption>? = null,  // For CUSTOM or DID_YOU "no" options
)

/**
 * A follow-up option for when the user needs choices.
 */
data class FollowUpOption(
    val id: String,
    val text: String,
    val reschedule: Boolean = false,     // Reschedule the task for later
    val skipPermanent: Boolean = false,  // Never show this task again
)

/**
 * Types of follow-up flows that can attach to any task.
 */
enum class FollowUpType {
    /** "How did that feel?" - Shows a feeling scale */
    FEELING,
    
    /** "Did you actually do it?" - Yes/No with options if No */
    DID_YOU,
    
    /** "Any thoughts on that?" - Optional text reflection */
    REFLECTION,
    
    /** Task-specific options defined in followUp.options */
    CUSTOM,
}

data class RoutingOption(
    val text: String,
    val preferCategory: TaskCategory?,
    val avoidCategory: TaskCategory?,
    val preferDifficulty: Difficulty?,
    val effectDuration: Int,
)

enum class TaskType {
    /** Text input with optional media */
    PROMPT,
    /** Freeform drawing canvas */
    DRAWING,
    /** Take a photo */
    PHOTO_CAPTURE,
    /** Voice recording */
    AUDIO_CAPTURE,
    /** Pick from options (single or multi) */
    SELECTION,
    /** "Go do this" with completion button */
    INSTRUCTION,
    /** Mood/preference questions for routing */
    ROUTING,
    /** Hold still challenge (accelerometer) */
    STILLNESS,
    /** Keep finger on screen challenge */
    HOLD_FINGER,
    /** Countdown timer challenge */
    WAIT_TIMER,
    /** Time-locked content */
    DONT_OPEN_UNTIL,
    /** Custom game (routes by task.id) */
    GAME,
}

enum class TaskCategory {
    SOCIAL,
    REFLECTION,
    PLAY,
    ACTION,
    STILLNESS,
    DISCOMFORT,
}

enum class Difficulty {
    LIGHT, MEDIUM, HEAVY
}

enum class MoodTrend {
    IMPROVING, STABLE, DECLINING, UNKNOWN
}

/**
 * Data captured from follow-up flows after task completion.
 */
data class FollowUpResult(
    val didComplete: Boolean? = null,      // from DID_YOU
    val feelingScore: Int? = null,         // from FEELING (1-5)
    val reflection: String? = null,        // from REFLECTION
    val selectedOptionId: String? = null,  // from CUSTOM or DID_YOU no options
)
