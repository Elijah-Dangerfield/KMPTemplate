package com.dangerfield.goodtimes.features.home.impl

import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.random.Random

/**
 * Generates contextual "thinking" messages that make transitions feel alive.
 * 
 * The goal is to make the app feel like it's genuinely considering what to show,
 * not just pulling from a queue. Messages adapt to context but stay mysterious
 * enough that users can't predict the algorithm.
 *
 * Thoughts for improvement:
 * - what other bits of context could we add
 * - how can we add to and or improve our messages with the context to make this seem more dynamic?
 * - Maybe using the persons name?
 */
interface ThinkingMessageProvider {
    fun getThinkingMessage(context: ThinkingContext): String
}

data class ThinkingContext(
    val isFirstTask: Boolean = false,
    val justCompletedTask: Boolean = false,
    val justSkipped: Boolean = false,
    val consecutiveSkips: Int = 0,
    val tasksCompletedThisSession: Int = 0,
    val currentMood: Mood? = null,
    val isLateNight: Boolean = false,
    val sessionNumber: Int = 1,
)

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ThinkingMessageProviderImpl : ThinkingMessageProvider {
    
    // Track recently used messages to avoid repetition
    private val recentMessages = mutableListOf<String>()
    private val maxRecent = 5
    
    override fun getThinkingMessage(context: ThinkingContext): String {
        val candidates = buildCandidateMessages(context)
        val available = candidates.filter { it !in recentMessages }
        
        val chosen = if (available.isNotEmpty()) {
            available.random()
        } else {
            candidates.random()
        }
        
        recentMessages.add(chosen)
        if (recentMessages.size > maxRecent) {
            recentMessages.removeAt(0)
        }
        
        return chosen
    }
    
    private fun buildCandidateMessages(context: ThinkingContext): List<String> = buildList {
        // First task of session - welcoming/orienting
        if (context.isFirstTask) {
            addAll(firstTaskMessages(context))
            return@buildList
        }
        
        // After skipping - acknowledge without judgment
        if (context.justSkipped) {
            addAll(afterSkipMessages(context))
            return@buildList
        }
        
        // After completing a task - acknowledge and transition
        if (context.justCompletedTask) {
            addAll(afterCompletionMessages(context))
            return@buildList
        }
        
        // General transitional messages
        addAll(generalMessages(context))
    }
    
    private fun firstTaskMessages(context: ThinkingContext): List<String> {
        val base = listOf(
            "Let's see...",
            "Thinking...",
            "Where to start...",
            "Hmm...",
        )
        
        return when {
            context.isLateNight -> base + listOf(
                "Late night thoughts...",
                "Quiet hours...",
            )
            context.sessionNumber == 1 -> base + listOf(
                "First things first...",
                "Starting somewhere...",
            )
            context.sessionNumber > 10 -> base + listOf(
                "Welcome back...",
                "You again...",
            )
            else -> base
        }
    }
    
    private fun afterSkipMessages(context: ThinkingContext): List<String> {
        val base = listOf(
            "Okay, something else...",
            "Let me think...",
            "Fair enough...",
            "Moving on...",
        )
        
        return when {
            context.consecutiveSkips >= 3 -> base + listOf(
                "Looking for the right one...",
                "Trying something different...",
                "Maybe this...",
            )
            context.consecutiveSkips >= 2 -> base + listOf(
                "How about...",
                "What about this...",
            )
            else -> base
        }
    }
    
    private fun afterCompletionMessages(context: ThinkingContext): List<String> {
        val base = listOf(
            "Okay...",
            "Got it...",
            "Noted...",
            "Interesting...",
            "Hmm...",
        )
        
        val taskCount = context.tasksCompletedThisSession
        
        return when {
            taskCount >= 5 -> base + listOf(
                "You're on a roll...",
                "One more thing...",
                "While you're here...",
            )
            taskCount >= 3 -> base + listOf(
                "And another...",
                "What else...",
                "Let's see...",
            )
            context.currentMood == Mood.GREAT || context.currentMood == Mood.GOOD -> base + listOf(
                "Good energy...",
                "Let's keep going...",
            )
            context.currentMood == Mood.BAD || context.currentMood == Mood.LOW -> base + listOf(
                "Something lighter...",
                "Easy one...",
            )
            else -> base
        }
    }
    
    private fun generalMessages(context: ThinkingContext): List<String> {
        val base = listOf(
            "Thinking...",
            "Let me see...",
            "One moment...",
            "Considering...",
            "Hmm...",
            "...",
        )
        
        // Add contextual variety
        return when {
            context.isLateNight -> base + listOf(
                "For the night...",
                "Something quiet...",
            )
            Random.nextFloat() < 0.1f -> base + listOf(
                "Bear with me...",
                "Almost...",
            )
            else -> base
        }
    }
}
