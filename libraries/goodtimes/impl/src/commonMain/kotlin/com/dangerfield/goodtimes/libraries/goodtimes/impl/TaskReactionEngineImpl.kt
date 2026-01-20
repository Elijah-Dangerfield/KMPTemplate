package com.dangerfield.goodtimes.libraries.goodtimes.impl

import com.dangerfield.goodtimes.libraries.goodtimes.TaskCompletionResult
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import com.dangerfield.goodtimes.libraries.goodtimes.TaskResponse
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.Reaction
import com.dangerfield.goodtimes.libraries.goodtimes.ReactionContext
import com.dangerfield.goodtimes.libraries.goodtimes.ReactionStyle
import com.dangerfield.goodtimes.libraries.goodtimes.TaskReactionEngine
import com.dangerfield.goodtimes.libraries.goodtimes.TaskType
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.random.Random

private const val BASE_REACTION_PROBABILITY = 0.15f // 15% base chance to react

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TaskReactionEngineImpl : TaskReactionEngine {
    
    // Track recently used reaction IDs to avoid repetition
    private val recentlyUsed = mutableListOf<String>()
    private val maxRecentlyUsed = 10
    
    override suspend fun considerReaction(
        result: TaskCompletionResult,
        context: ReactionContext,
    ): Reaction? {
        // Handle skip patterns - only after 2+ consecutive skips
        if (result.outcome != TaskOutcome.COMPLETED && context.consecutiveSkips >= 2) {
            return considerSkipPatternReaction(context)
        }
        
        // Don't react to skipped tasks otherwise - feels judgmental
        if (result.outcome != TaskOutcome.COMPLETED) return null
        
        // Don't react in first session - let them get comfortable
        if (context.isFirstSession && context.tasksCompletedThisSession < 3) return null
        
        // Find applicable reactions
        val candidates = findCandidateReactions(result, context)
            .filter { it.id !in context.recentReactionIds && it.id !in recentlyUsed }
        
        if (candidates.isEmpty()) return null
        
        // Roll the dice with adjusted probability
        val adjustedProbability = calculateProbability(result, context)
        if (Random.nextFloat() > adjustedProbability) return null
        
        // Pick a reaction
        val chosen = candidates.random()
        
        // Track usage
        recentlyUsed.add(chosen.id)
        if (recentlyUsed.size > maxRecentlyUsed) {
            recentlyUsed.removeAt(0)
        }
        
        return chosen.reaction
    }
    
    private fun calculateProbability(result: TaskCompletionResult, context: ReactionContext): Float {
        var probability = BASE_REACTION_PROBABILITY
        
        // Increase probability for notable behaviors
        if (isNotableResponse(result)) {
            probability += 0.15f
        }
        
        // Slightly higher chance late at night (more intimate feeling)
        if (context.isLateNight) {
            probability += 0.05f
        }
        
        // Lower chance if user seems to be in a bad mood
        if (context.currentMood == Mood.BAD || context.currentMood == Mood.LOW) {
            probability -= 0.10f
        }
        
        // Don't spam reactions - lower chance if they've completed many tasks this session
        if (context.tasksCompletedThisSession > 5) {
            probability -= 0.05f
        }
        
        return probability.coerceIn(0.05f, 0.35f)
    }
    
    private fun isNotableResponse(result: TaskCompletionResult): Boolean {
        val response = result.response ?: return false
        
        return when (response) {
            is TaskResponse.Text -> {
                response.value.length < 10 || response.value.length > 300
            }
            else -> false
        } || result.timeSpentMs > 180_000 || result.timeSpentMs < 5_000
    }
    
    private fun findCandidateReactions(
        result: TaskCompletionResult,
        context: ReactionContext,
    ): List<CandidateReaction> = buildList {
        
        // Text response reactions
        (result.response as? TaskResponse.Text)?.let { textResponse ->
            val length = textResponse.value.length
            
            // Very short responses
            if (length < 15) {
                add(CandidateReaction(
                    id = "short_response_1",
                    reaction = Reaction(
                        message = "A person of few words. Interesting.",
                        style = ReactionStyle.QUIP,
                    )
                ))
                add(CandidateReaction(
                    id = "short_response_2",
                    reaction = Reaction(
                        message = "Brief. I respect that.",
                        style = ReactionStyle.OBSERVATION,
                    )
                ))
                add(CandidateReaction(
                    id = "short_response_3",
                    reaction = Reaction(
                        message = "You said just enough.",
                        style = ReactionStyle.ACKNOWLEDGMENT,
                    )
                ))
            }
            
            // Very long responses
            if (length > 250) {
                add(CandidateReaction(
                    id = "long_response_1",
                    reaction = Reaction(
                        message = "Do all humans talk this much? Interesting.",
                        style = ReactionStyle.QUIP,
                    )
                ))
                add(CandidateReaction(
                    id = "long_response_2",
                    reaction = Reaction(
                        message = "You had a lot to say. That's rare.",
                        style = ReactionStyle.OBSERVATION,
                    )
                ))
                add(CandidateReaction(
                    id = "long_response_3",
                    reaction = Reaction(
                        message = "Most people don't write that much. I'm listening.",
                        style = ReactionStyle.ACKNOWLEDGMENT,
                    )
                ))
            }
            
            // Medium-length thoughtful responses
            if (length in 80..200) {
                add(CandidateReaction(
                    id = "thoughtful_response_1",
                    reaction = Reaction(
                        message = "You thought about that one.",
                        style = ReactionStyle.OBSERVATION,
                    )
                ))
            }
        }
        
        // Time-based reactions
        if (result.timeSpentMs > 180_000) { // More than 3 minutes
            add(CandidateReaction(
                id = "took_time_1",
                reaction = Reaction(
                    message = "You took your time. I noticed.",
                    style = ReactionStyle.OBSERVATION,
                )
            ))
            add(CandidateReaction(
                id = "took_time_2",
                reaction = Reaction(
                    message = "No rush. I have nowhere else to be.",
                    style = ReactionStyle.ACKNOWLEDGMENT,
                )
            ))
        }
        
        if (result.timeSpentMs < 8_000 && context.task.type == TaskType.PROMPT) { // Less than 8 seconds for a prompt
            add(CandidateReaction(
                id = "fast_response_1",
                reaction = Reaction(
                    message = "You move fast. Is that how you do everything?",
                    style = ReactionStyle.QUESTION,
                )
            ))
            add(CandidateReaction(
                id = "fast_response_2",
                reaction = Reaction(
                    message = "Quick. Decisive. I see.",
                    style = ReactionStyle.OBSERVATION,
                )
            ))
        }
        
        // Late night reactions
        if (context.isLateNight && context.tasksCompletedThisSession >= 2) {
            add(CandidateReaction(
                id = "late_night_1",
                reaction = Reaction(
                    message = "It's late. You're still here.",
                    style = ReactionStyle.OBSERVATION,
                )
            ))
            add(CandidateReaction(
                id = "late_night_2",
                reaction = Reaction(
                    message = "We keep meeting like this.",
                    style = ReactionStyle.QUIP,
                )
            ))
        }
        
        // Milestone reactions
        if (context.totalTasksCompleted == 10) {
            add(CandidateReaction(
                id = "milestone_10",
                reaction = Reaction(
                    message = "That's 10. We're getting somewhere.",
                    style = ReactionStyle.ACKNOWLEDGMENT,
                )
            ))
        }
        
        if (context.totalTasksCompleted == 50) {
            add(CandidateReaction(
                id = "milestone_50",
                reaction = Reaction(
                    message = "50 pages. You've been patient with me.",
                    style = ReactionStyle.ACKNOWLEDGMENT,
                )
            ))
        }
        
        if (context.totalTasksCompleted == 100) {
            add(CandidateReaction(
                id = "milestone_100",
                reaction = Reaction(
                    message = "100. Most people don't get this far.",
                    style = ReactionStyle.OBSERVATION,
                )
            ))
        }
        
        // Task-type specific
        if (context.task.type == TaskType.INSTRUCTION) {
            // These are "go do this" tasks - if completed quickly, maybe they didn't do it
            if (result.timeSpentMs < 30_000) {
                add(CandidateReaction(
                    id = "instruction_fast",
                    reaction = Reaction(
                        message = "That was quick.",
                        style = ReactionStyle.QUIP,
                    )
                ))
            }
        }
        
        // Signal-based reactions
        val writingSignal = result.signals.find { it.dimension == "WRITING_AFFINITY" }
        if (writingSignal != null && writingSignal.adjustment >= 2) {
            add(CandidateReaction(
                id = "writing_affinity_high",
                reaction = Reaction(
                    message = "You like to write. I can tell.",
                    style = ReactionStyle.OBSERVATION,
                )
            ))
        }
        
        val hesitancySignal = result.signals.find { it.dimension == "HESITANCY" }
        if (hesitancySignal != null && hesitancySignal.adjustment >= 1) {
            add(CandidateReaction(
                id = "hesitant_start",
                reaction = Reaction(
                    message = "You think before you write. I like that.",
                    style = ReactionStyle.OBSERVATION,
                )
            ))
        }
    }
    
    /**
     * Consider a reaction when user has been skipping multiple tasks in a row.
     * This should feel gentle and understanding, not judgmental.
     */
    private fun considerSkipPatternReaction(context: ReactionContext): Reaction? {
        // Only react once per session for skip patterns
        val skipReactionId = "skip_pattern_${context.consecutiveSkips}"
        if (skipReactionId in recentlyUsed) return null
        
        val reaction = when {
            context.consecutiveSkips >= 4 -> {
                Reaction(
                    message = "We can stop for today if you're not feeling it. No pressure.",
                    style = ReactionStyle.ACKNOWLEDGMENT,
                )
            }
            context.consecutiveSkips >= 2 -> {
                val options = listOf(
                    "You've passed on a few. Not feeling it today?",
                    "Some days are like that. These questions aren't going anywhere.",
                    "No rush. I'll be here when the timing feels right.",
                )
                Reaction(
                    message = options.random(),
                    style = ReactionStyle.ACKNOWLEDGMENT,
                )
            }
            else -> return null
        }
        
        recentlyUsed.add(skipReactionId)
        if (recentlyUsed.size > maxRecentlyUsed) {
            recentlyUsed.removeAt(0)
        }
        
        return reaction
    }
    
    private data class CandidateReaction(
        val id: String,
        val reaction: Reaction,
    )
}
