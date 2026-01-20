package com.dangerfield.goodtimes.features.home.impl

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val FIRST_EVER_PROMPT = "Hey, if you don't mind, I'll be asking how you're feeling from time to time. It helps me understand you better. Which is kinda my whole thing. So, how are you?"

fun getMoodPromptText(
    dismissCount: Int,
    sessionNumber: Int,
    isFirstEverMoodPrompt: Boolean,
    clock: Clock
): String {
    if (isFirstEverMoodPrompt) return FIRST_EVER_PROMPT
    
    val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = now.hour
    val dayOfWeek = now.dayOfWeek.ordinal // 0 = Monday, 6 = Sunday
    val isWeekend = dayOfWeek >= 5
    
    return when (hour) {
        in 0..4 -> lateNightPrompts.random()

        // Early morning
        in 5..7 -> earlyMorningPrompts.random()

        // Morning
        in 8..11 -> if (isWeekend) weekendMorningPrompts.random() else morningPrompts.random()

        // Afternoon
        in 12..16 -> afternoonPrompts.random()

        // Evening
        in 17..20 -> eveningPrompts.random()

        // Night
        in 21..23 -> nightPrompts.random()

        // Fallback based on session/dismiss context
        else -> getContextualPrompt(dismissCount, sessionNumber)
    }.let { timeBasedPrompt ->
        // Override with context-based prompts in certain situations
        when {
            dismissCount >= 10 -> persistentPrompts.random()
            dismissCount >= 5 -> frequentDismissPrompts.random()
            dismissCount >= 3 -> occasionalDismissPrompts.random()
            sessionNumber == 1 -> firstSessionPrompts.random()
            sessionNumber == 2 -> secondSessionPrompts.random()
            sessionNumber <= 5 -> earlySessionPrompts.random()
            sessionNumber == 10 -> tenthSessionPrompts.random()
            sessionNumber == 50 -> fiftiethSessionPrompts.random()
            sessionNumber == 100 -> hundredthSessionPrompts.random()
            sessionNumber % 25 == 0 -> milestonePrompts.random()
            else -> timeBasedPrompt
        }
    }
}

private fun getContextualPrompt(dismissCount: Int, sessionNumber: Int): String {
    return when {
        dismissCount >= 5 -> frequentDismissPrompts.random()
        dismissCount >= 3 -> occasionalDismissPrompts.random()
        sessionNumber == 1 -> firstSessionPrompts.random()
        sessionNumber <= 3 -> earlySessionPrompts.random()
        else -> genericPrompts.random()
    }
}

// ============================================================================
// TIME-BASED PROMPTS
// ============================================================================

private val lateNightPrompts = listOf(
    "You're up late. How are you doing?",
    "Can't sleep?",
    "The quiet hours. How are you?",
    "We keep meeting like this. Everything okay?",
    "Late night thoughts hitting different?",
    "The world's asleep. How are you holding up?",
    "Burning the midnight oil or just can't shut off?",
    "Hey night owl. What's going on with you?",
    "It's late. You okay?",
    "The 3am version of you — how's it feeling?",
    "Insomnia club. How are we doing tonight?",
    "These hours are honest hours. How are you really?",
)

private val earlyMorningPrompts = listOf(
    "Hey there, early bird. How are you feeling?",
    "Up before the sun. Intentional or accidental?",
    "Early riser. How's the morning treating you?",
    "The world's still waking up. How about you?",
    "Coffee yet? How are you?",
    "Dawn patrol. How are you feeling?",
    "You're up early. Everything alright?",
    "First light. How are you starting this one?",
    "The early hours. Peaceful or restless?",
)

private val morningPrompts = listOf(
    "Good morning. How are you?",
    "Morning check-in. How are we doing?",
    "How's the morning going?",
    "Starting the day — how are you feeling?",
    "Morning. What's the vibe today?",
    "How'd you wake up feeling?",
    "New day. How are you entering it?",
    "Morning! How's your head?",
)

private val weekendMorningPrompts = listOf(
    "Weekend morning. How are you?",
    "No alarm today? How are you feeling?",
    "Lazy morning vibes. How are you?",
    "Weekend mode. How's it going?",
    "Saturday/Sunday energy. How are you?",
    "The good kind of morning. How are you feeling?",
)

private val afternoonPrompts = listOf(
    "Afternoon check-in. How are you?",
    "Midday. How's it going?",
    "How's the day treating you so far?",
    "Afternoon. Still standing?",
    "Hey. How's your day been?",
    "Post-lunch mood check. How are you?",
    "The middle of the day. How are you feeling?",
    "Checking in. How's the afternoon?",
)

private val eveningPrompts = listOf(
    "Evening. How was today?",
    "Day's winding down. How are you?",
    "How are you feeling as the day ends?",
    "Evening check-in. How's it going?",
    "The day's almost done. How are you?",
    "Settling into the evening. How are you feeling?",
    "End of day energy. How are you?",
    "Evening. Anything on your mind?",
)

private val nightPrompts = listOf(
    "Night time. How are you?",
    "Winding down? How are you feeling?",
    "Evening hours. How's it going?",
    "Getting late. How are you?",
    "Night check-in. How are you doing?",
    "The day's behind you. How are you feeling?",
    "Quiet evening. How are you?",
)

// ============================================================================
// SESSION-BASED PROMPTS
// ============================================================================

private val firstSessionPrompts = listOf(
    "Hey. How are you feeling right now?",
    "First time here. How are you?",
    "Welcome. How are you doing?",
    "Nice to meet you. How are you feeling?",
    "Hey there. How's it going?",
    "Starting out. How are you?",
    "Hello. How are you right now?",
)

private val secondSessionPrompts = listOf(
    "You came back. How are you feeling?",
    "Hey again. How are you?",
    "Good to see you. How are you doing?",
    "Welcome back. How's it going?",
    "You returned. How are you?",
    "Second time's the charm. How are you feeling?",
)

private val earlySessionPrompts = listOf(
    "Welcome back. How are you today?",
    "Hey. How are you feeling?",
    "Good to see you again. How are you?",
    "Back again. How's it going?",
    "How are you doing today?",
    "Hey there. How are you?",
)

private val tenthSessionPrompts = listOf(
    "Hey, for the 10th time. How are you feeling?",
    "Double digits. How are you?",
    "You keep coming back. How are you doing?",
)

private val fiftiethSessionPrompts = listOf(
    "50th time is the charm. How are you?",
    "Halfway to 100 . How are you feeling?",
    "50 visits. That means something. How are you?",
)

private val hundredthSessionPrompts = listOf(
    "100 times. How are you?",
    "Triple digits. How are you feeling?",
    "Visit 100. Still curious — how are you?",
)

private val milestonePrompts = listOf(
    "Another milestone. How are you?",
    "You've been here a while. How are you feeling?",
    "Still showing up. How are you?",
)

// ============================================================================
// DISMISS-BASED PROMPTS
// ============================================================================

private val occasionalDismissPrompts = listOf(
    "I'll stop asking eventually. Promise. How are you?",
    "I know, I know. But still — how are you?",
    "Sorry to keep asking. How are you though?",
    "One more time. How are you?",
    "Bear with me. How are you feeling?",
)

private val frequentDismissPrompts = listOf(
    "I know, I know. But... how are you?",
    "Okay I get it. But really, how are you?",
    "You keep closing this. Fair. But how are you?",
    "I'm persistent. How are you?",
    "Last time today, I promise. How are you?",
    "I can take a hint. But first — how are you?",
)

private val persistentPrompts = listOf(
    "Still asking. How are you?",
    "I don't give up easy. How are you?",
    "You know the drill. How are you?",
    "Yes, again. How are you feeling?",
    "I'm nothing if not consistent. How are you?",
    "Me again. How are you?",
    "Relentless, I know. How are you?",
)

// ============================================================================
// GENERIC FALLBACKS
// ============================================================================

private val genericPrompts = listOf(
    "How are you feeling?",
    "How are you?",
    "How's it going?",
    "How are you doing?",
    "What's your mood?",
    "Hey. How are you?",
    "Check in with yourself. How are you?",
    "Pause. How are you feeling?",
    "Quick question — how are you?",
    "How are you right now?",
)

// ============================================================================
// DISMISSAL GOODBYE MESSAGES
// ============================================================================

fun getDismissingText(): String = dismissingMessages.random()

private val dismissingMessages = listOf(
    "Understood. I'll be here if you change your mind.",
    "Got it. You know where to find me.",
    "No worries. The setting is in Settings if you ever want this back.",
    "Okay. I'll stop asking.",
    "Fair enough. Take care.",
    "Noted. I'll be quiet now.",
    "Alright. The door's always open.",
    "I respect that. See you around.",
    "Gone but not forgotten. Settings if you miss me.",
    "Okay. I hope you're doing well, though.",
    "Understood. I'll still be paying attention in other ways.",
    "Got it. No hard feelings.",
)

// ============================================================================
// MOOD RESPONSE MESSAGES
// ============================================================================

fun getMoodResponseText(mood: com.dangerfield.goodtimes.libraries.goodtimes.Mood): String {
    return when (mood) {
        com.dangerfield.goodtimes.libraries.goodtimes.Mood.GREAT -> greatMoodResponses.random()
        com.dangerfield.goodtimes.libraries.goodtimes.Mood.GOOD -> goodMoodResponses.random()
        com.dangerfield.goodtimes.libraries.goodtimes.Mood.OKAY -> okayMoodResponses.random()
        com.dangerfield.goodtimes.libraries.goodtimes.Mood.LOW -> lowMoodResponses.random()
        com.dangerfield.goodtimes.libraries.goodtimes.Mood.BAD -> badMoodResponses.random()
        com.dangerfield.goodtimes.libraries.goodtimes.Mood.COMPLICATED -> complicatedMoodResponses.random()
    }
}

private val greatMoodResponses = listOf(
    "Love that for you.",
    "Hell yeah. Ride that wave.",
    "That's what I like to hear.",
    "Amazing. You deserve it.",
    "Great energy. I'm taking notes.",
    "Look at you thriving.",
    "Yes! Keep doing whatever you're doing.",
    "I love when you're in your element.",
    "This is the good stuff. Noted.",
    "Wonderful. Genuinely happy for you.",
)

private val goodMoodResponses = listOf(
    "Good is good. I'll take it.",
    "Solid. Stable. I appreciate that.",
    "Nice. Nothing wrong with good.",
    "Good vibes. Noted.",
    "That's a win in my book.",
    "Glad to hear it.",
    "Good is underrated. I'm here for it.",
    "Cool. Thanks for sharing.",
    "A good day is a gift. Enjoy it.",
    "Noted. Hope it stays that way.",
)

private val okayMoodResponses = listOf(
    "Okay is okay. No pressure.",
    "Middle ground. I get it.",
    "Not every day is a highlight. That's fine.",
    "Okay is valid. Thanks for telling me.",
    "Neutral isn't bad. It's just... here.",
    "I hear you. Sometimes 'okay' is all we've got.",
    "Okay is honest. I respect that.",
    "Thanks for checking in anyway.",
    "The middle is still somewhere. Noted.",
    "Okay today, maybe better tomorrow. Or not. Either way.",
)

private val lowMoodResponses = listOf(
    "I'm sorry. That's hard.",
    "Thanks for being honest. That takes something.",
    "Low is tough. I see you.",
    "I hope it lifts soon. Or doesn't. I'm not in charge.",
    "That's real. I appreciate you sharing.",
    "I'm here. Not that I can do much. But I'm here.",
    "Low days happen. You're not alone in that.",
    "Noted. Be gentle with yourself if you can.",
    "I hear you. Sending whatever good energy I can.",
    "Thank you for telling me. I won't forget.",
)

private val badMoodResponses = listOf(
    "I'm really sorry.",
    "That's heavy. Thank you for trusting me with it.",
    "Bad days are brutal. I see you.",
    "I wish I could do more than just listen.",
    "You don't have to have it together. Not for me.",
    "I'm here. For whatever that's worth.",
    "Thank you for being real. That's not easy.",
    "I hope something good finds you today.",
    "Noted. Please take care of yourself.",
    "I'm sorry it's like this right now.",
)

private val complicatedMoodResponses = listOf(
    "Complicated is honest. I respect that.",
    "Feelings are weird. I get it.",
    "Not everything fits in a box. That's okay.",
    "Thanks for not oversimplifying.",
    "Complicated is valid. Human, even.",
    "I appreciate the nuance.",
    "Life is messy. I see that.",
    "Thank you for being honest about the complexity.",
    "Noted. I won't try to untangle it for you.",
    "Some days just... are. I hear you.",
)
