package com.dangerfield.goodtimes.features.home.impl

import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Generates contextual "thinking" messages that make transitions feel alive.
 * 
 * The goal is to make the app feel like it's genuinely considering what to show,
 * not just pulling from a queue. Messages adapt to context but stay mysterious
 * enough that users can't predict the algorithm.
 *
 * Key contexts that shape messages:
 * - Time away: Acknowledges absence when returning after hours/days
 * - Time of day: Late night, early morning get special treatment
 * - User name: Personalizes greetings when known
 * - Session patterns: First session vs veteran user
 * - Device state: Battery level acknowledgment
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
    val isEarlyMorning: Boolean = false,
    val sessionNumber: Int = 1,
    // New awareness fields
    val userName: String? = null,
    val timeSinceLastSession: Duration? = null,
    val isBatteryLow: Boolean = false,
    val isWeekend: Boolean = false,
) {
    /** User is returning after 6+ hours */
    val isReturningUser: Boolean 
        get() = timeSinceLastSession?.let { it >= 6.hours } ?: false
}

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
        // Returning after substantial absence - prioritize welcome back
        if (context.isFirstTask && context.isReturningUser) {
            addAll(welcomeBackMessages(context))
            return@buildList
        }
        
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
    
    /**
     * Messages for users returning after a significant time away.
     * These acknowledge the absence and ease back in.
     */
    private fun welcomeBackMessages(context: ThinkingContext): List<String> {
        val name = context.userName
        val timeSince = context.timeSinceLastSession
        
        // Week+ absence - warm, acknowledging
        if (timeSince != null && timeSince >= 7.days) {
            return buildList {
                if (name != null) {
                    add("$name. It's been a while...")
                    add("Oh, $name. Welcome back...")
                    add("$name returns...")
                }
                add("You've been away...")
                add("It's been a while...")
                add("I was starting to wonder...")
                
                // Time of day awareness even for long returns
                if (context.isLateNight) {
                    add("Late night return...")
                }
            }
        }
        
        // 1-7 days - gentle acknowledgment
        if (timeSince != null && timeSince >= 1.days) {
            val days = timeSince.inWholeDays.toInt()
            return buildList {
                if (name != null) {
                    add("Welcome back, $name...")
                    add("$name, picking up where we left off...")
                    if (days == 1) add("$name. Just a day, but I noticed...")
                }
                add("Picking up where we left off...")
                add("Back again...")
                add("Let's continue...")
                
                if (context.isLateNight) {
                    add("Late night visit...")
                } else if (context.isEarlyMorning) {
                    add("Early morning check-in...")
                }
            }
        }
        
        // 6+ hours - light acknowledgment
        if (timeSince != null && timeSince >= 6.hours) {
            return buildList {
                if (name != null) {
                    add("Hey $name...")
                    add("$name, back for more...")
                }
                add("Back again...")
                add("Where were we...")
                add("Continuing...")
                
                if (context.isLateNight) {
                    add("Couldn't sleep?...")
                } else if (context.isEarlyMorning) {
                    add("Early riser...")
                }
                
                if (context.isBatteryLow) {
                    add("Your battery's low, but you're here...")
                }
            }
        }
        
        // Fallback to regular first task messages
        return firstTaskMessages(context)
    }
    
    private fun firstTaskMessages(context: ThinkingContext): List<String> {
        val name = context.userName
        val base = buildList {
            add("Let's see...")
            add("Thinking...")
            add("Where to start...")
            add("Hmm...")
            // Add personalized options if we have a name
            if (name != null) {
                add("Okay $name...")
                add("Alright $name, let's see...")
            }
        }
        
        return when {
            context.isLateNight -> base + buildList {
                add("Late night thoughts...")
                add("Quiet hours...")
                if (name != null) add("Burning the midnight oil, $name?...")
            }
            context.isEarlyMorning -> base + buildList {
                add("Early start...")
                add("Fresh morning...")
                if (name != null) add("Up early, $name...")
            }
            context.sessionNumber == 1 -> base + listOf(
                "Let's get started...",
                "Here we go...",
                "Starting with something simple...",
            )
            context.sessionNumber > 10 -> base + buildList {
                add("Welcome back...")
                if (name != null) add("$name, good to see you...")
            }
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
        val name = context.userName
        val isNewUser = context.sessionNumber <= 2
        
        // Early tasks for new users - be warm and encouraging
        // This is their first impression of the app's personality
        if (isNewUser && taskCount <= 5) {
            return earlyUserCompletionMessages(taskCount, name)
        }
        
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
    
    /**
     * Warm, encouraging messages for new users completing their first few tasks.
     * First impressions matter - we want them to feel seen and encouraged.
     */
    private fun earlyUserCompletionMessages(taskCount: Int, name: String?): List<String> {
        return when (taskCount) {
            1 -> buildList {
                add("Good start...")
                add("I like that...")
                add("Okay, I see you...")
                if (name != null) {
                    add("Nice, $name...")
                    add("Good one, $name...")
                }
            }
            2 -> buildList {
                add("I'm learning about you...")
                add("This is helpful...")
                add("Interesting...")
                add("Getting a sense of you...")
                if (name != null) {
                    add("Thanks $name, keep going...")
                }
            }
            3 -> buildList {
                add("This is great, I'm learning a lot...")
                add("You're giving me a lot to work with...")
                add("We're getting somewhere...")
                add("I like this...")
                if (name != null) {
                    add("$name, this is great...")
                }
            }
            4 -> buildList {
                add("I feel like I'm starting to know you...")
                add("Keep going...")
                add("This is good...")
                add("Getting to know you...")
                if (name != null) {
                    add("$name, you're doing great...")
                }
            }
            5 -> buildList {
                add("We're building something here...")
                add("This is really helpful...")
                add("I'm getting better at this...")
                add("Thanks for sticking around...")
                if (name != null) {
                    add("$name, I think we're going to get along...")
                }
            }
            else -> listOf(
                "Let's keep going...",
                "What else...",
                "One more...",
            )
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
