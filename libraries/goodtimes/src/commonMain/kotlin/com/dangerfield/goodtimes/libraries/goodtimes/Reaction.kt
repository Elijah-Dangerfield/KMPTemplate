package com.dangerfield.goodtimes.libraries.goodtimes

/**
 * A reactive comment from the app based on how the user completed a task.
 * Makes the app feel alive by occasionally remarking on user behavior.
 */
data class Reaction(
    val message: String,
    val style: ReactionStyle,
    val followUpPrompt: String? = null,
)

/**
 * The tone/intent of the reaction.
 */
enum class ReactionStyle {
    /** Quick witty remark ("A person of few words.") */
    QUIP,
    /** Observational comment ("You think before you write. I like that.") */
    OBSERVATION,
    /** Rhetorical or soft question ("Is that how you do everything?") */
    QUESTION,
    /** Acknowledgment of something specific ("That one felt different, didn't it?") */
    ACKNOWLEDGMENT,
}
