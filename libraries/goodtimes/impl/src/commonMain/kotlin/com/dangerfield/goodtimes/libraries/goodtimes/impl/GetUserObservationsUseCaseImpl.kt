package com.dangerfield.goodtimes.libraries.goodtimes.impl

import com.dangerfield.goodtimes.libraries.goodtimes.GetUserObservationsUseCase
import com.dangerfield.goodtimes.libraries.goodtimes.Observation
import com.dangerfield.goodtimes.libraries.goodtimes.ObservationCategory
import com.dangerfield.goodtimes.libraries.goodtimes.User
import com.dangerfield.goodtimes.libraries.goodtimes.UserRepository
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.math.roundToInt

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class GetUserObservationsUseCaseImpl(
    private val userRepository: UserRepository,
    private val clock: kotlin.time.Clock,
) : GetUserObservationsUseCase {

    override suspend fun invoke(): List<Observation> {
        val user = userRepository.getUser() ?: return emptyList()
        
        val allObservations = buildList {
            // Identity observations (name - always first for new users)
            addIdentityObservations(user)
            
            // Timing observations
            addTimingObservations(user)
            
            // Engagement observations
            addEngagementObservations(user)
            
            // Expression observations
            addExpressionObservations(user)
            
            // Personality observations
            addPersonalityObservations(user)
        }
        
        // Apply progressive disclosure based on sessions count
        val maxObservations = when {
            user.sessionsCount <= 3 -> 2  // New users: gentle introduction
            user.sessionsCount <= 10 -> 5 // Established: moderate insights
            else -> allObservations.size  // Veterans: show all
        }
        
        return allObservations
            .sortedByDescending { it.priority }
            .take(maxObservations)
    }
    
    // =========================================================================
    // IDENTITY OBSERVATIONS (Name)
    // =========================================================================
    
    private fun MutableList<Observation>.addIdentityObservations(user: User) {
        val name = user.name ?: return
        val now = clock.now().toEpochMilliseconds()
        
        val nameSetAt = user.nameSetAt ?: return
        val nameUpdatedAt = user.nameUpdatedAt ?: nameSetAt
        
        // How long since name was first set
        val timeSinceSet = now - nameSetAt
        val timeSinceUpdated = now - nameUpdatedAt
        
        // Time thresholds
        val oneMinute = 60_000L
        val oneHour = 3_600_000L
        val oneDay = 86_400_000L
        val oneWeek = 604_800_000L
        
        // Detect if name was changed (not the first set)
        val wasChanged = nameSetAt != nameUpdatedAt
        
        // Detect name patterns
        val hasSpace = name.contains(" ")
        val wordCount = name.split(Regex("\\s+")).size
        val isLikelyFullName = wordCount >= 2
        val isLikelyNickname = wordCount == 1 && name.length <= 6
        val seemsFunny = detectFunnyName(name)
        val seemsMysterious = detectMysteriousName(name)
        
        when {
            // Recently changed (within 1 minute)
            wasChanged && timeSinceUpdated < oneMinute -> {
                add(Observation(
                    id = "name_just_changed",
                    message = "Oh, $name now? Okay, I'll remember that.",
                    category = ObservationCategory.IDENTITY,
                    priority = 10
                ))
            }
            
            // Changed within the hour
            wasChanged && timeSinceUpdated < oneHour -> {
                add(Observation(
                    id = "name_recently_changed",
                    message = "I'm still getting used to calling you $name. It suits you.",
                    category = ObservationCategory.IDENTITY,
                    priority = 10
                ))
            }
            
            // Just set for the first time (within 1 minute)
            !wasChanged && timeSinceSet < oneMinute -> {
                add(Observation(
                    id = "name_just_set",
                    message = "Nice to officially meet you, $name.",
                    category = ObservationCategory.IDENTITY,
                    priority = 10
                ))
            }
            
            // Set recently (within 1 hour)
            !wasChanged && timeSinceSet < oneHour -> {
                add(Observation(
                    id = "name_recently_set",
                    message = "$name. I like saying it.",
                    category = ObservationCategory.IDENTITY,
                    priority = 10
                ))
            }
            
            // Funny/playful name
            seemsFunny -> {
                add(Observation(
                    id = "name_playful",
                    message = "\"$name\"—I see you. Nice choice.",
                    category = ObservationCategory.IDENTITY,
                    priority = 9
                ))
            }
            
            // Mysterious name (initials, single character, etc.)
            seemsMysterious -> {
                add(Observation(
                    id = "name_mysterious",
                    message = "Just $name? Keeping it mysterious. I can work with that.",
                    category = ObservationCategory.IDENTITY,
                    priority = 9
                ))
            }
            
            // Full name provided
            isLikelyFullName -> {
                add(Observation(
                    id = "name_formal",
                    message = "$name. The full thing. I appreciate the formality.",
                    category = ObservationCategory.IDENTITY,
                    priority = 8
                ))
            }
            
            // Nickname
            isLikelyNickname -> {
                add(Observation(
                    id = "name_nickname",
                    message = "$name. Short, sweet. Easy to remember.",
                    category = ObservationCategory.IDENTITY,
                    priority = 8
                ))
            }
            
            // Known for a while (> 1 week)
            timeSinceSet > oneWeek -> {
                add(Observation(
                    id = "name_longtime",
                    message = "I've been calling you $name for a while now. It fits.",
                    category = ObservationCategory.IDENTITY,
                    priority = 7
                ))
            }
            
            // Default: we know the name
            else -> {
                add(Observation(
                    id = "name_known",
                    message = "$name. I remember.",
                    category = ObservationCategory.IDENTITY,
                    priority = 8
                ))
            }
        }
    }
    
    /**
     * Detect if the name seems playful/funny
     * - Contains numbers
     * - Contains symbols
     * - All caps
     * - Known joke patterns
     */
    private fun detectFunnyName(name: String): Boolean {
        val lowerName = name.lowercase()
        return name.contains(Regex("[0-9]")) ||
            name.contains(Regex("[!@#\$%^&*()_+=\\[\\]{}|;:'\",.<>?/\\\\]")) ||
            (name.length > 2 && name == name.uppercase()) ||
            lowerName in listOf("test", "user", "anonymous", "nobody", "me", "person", "human", "batman", "username")
    }
    
    /**
     * Detect if the name seems mysterious
     * - Single character
     * - 2-3 characters that look like initials
     * - Symbols only
     */
    private fun detectMysteriousName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length == 1 ||
            (trimmed.length in 2..3 && trimmed.all { it.isUpperCase() || it == '.' }) ||
            trimmed.matches(Regex("^[A-Z]\\.$")) ||
            trimmed.matches(Regex("^[A-Z]\\.[A-Z]\\.$"))
    }
    
    // =========================================================================
    // TIMING OBSERVATIONS
    // =========================================================================
    
    private fun MutableList<Observation>.addTimingObservations(user: User) {
        // Night owl pattern
        if (user.isNightOwl) {
            val percentage = ((user.lateNightSessionCount.toFloat() / user.sessionsCount) * 100).roundToInt()
            add(Observation(
                id = "night_owl",
                message = "I've noticed you often come by after midnight. About $percentage% of our time together is in the quiet hours. Night owl?",
                category = ObservationCategory.TIMING,
                priority = 8
            ))
        }
        
        // Morning person pattern
        if (user.isMorningPerson) {
            val percentage = ((user.morningSessionCount.toFloat() / user.sessionsCount) * 100).roundToInt()
            add(Observation(
                id = "morning_person",
                message = "You seem to be an early riser—$percentage% of your visits happen between 6 and 10am. I like that energy.",
                category = ObservationCategory.TIMING,
                priority = 8
            ))
        }
        
        // Midday regular pattern
        if (user.isMiddayRegular) {
            add(Observation(
                id = "midday_regular",
                message = "Lunch break check-ins? I've noticed you often swing by around midday. It's become kind of our thing.",
                category = ObservationCategory.TIMING,
                priority = 7
            ))
        }
    }
    
    // =========================================================================
    // ENGAGEMENT OBSERVATIONS
    // =========================================================================
    
    private fun MutableList<Observation>.addEngagementObservations(user: User) {
        // Early observations for new users (don't need 5 sessions)
        
        // First task completed
        if (user.tasksCompleted == 1) {
            add(Observation(
                id = "first_task",
                message = "You did one. That's how it starts.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 10
            ))
        }
        
        // Getting started (2-3 tasks)
        if (user.tasksCompleted in 2..3) {
            add(Observation(
                id = "getting_started",
                message = "You've done ${user.tasksCompleted} things now. I'm starting to get a sense of you.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 10
            ))
        }
        
        // Building momentum (4-9 tasks)
        if (user.tasksCompleted in 4..9) {
            add(Observation(
                id = "building_momentum",
                message = "We're at ${user.tasksCompleted} things together. It's starting to feel like a habit.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 9
            ))
        }
        
        // Milestone: 10 tasks
        if (user.tasksCompleted in 10..19) {
            add(Observation(
                id = "ten_tasks",
                message = "${user.tasksCompleted} tasks. Double digits. You're sticking around.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 10
            ))
        }
        
        // Committed pattern
        if (user.isCommitted) {
            val percentage = (user.completionRate * 100).roundToInt()
            add(Observation(
                id = "committed",
                message = "You finish what you start—$percentage% completion rate. That's not nothing.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 9
            ))
        }
        
        // Selective player pattern
        if (user.isSelectivePlayer) {
            add(Observation(
                id = "selective",
                message = "You know what you want. You skip what doesn't spark and engage with what does. I respect that.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 7
            ))
        }
        
        // Veteran pattern
        if (user.isVeteran) {
            add(Observation(
                id = "veteran",
                message = "We've done ${user.tasksCompleted} things together now. That's... a lot of good times.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 10
            ))
        }
        
        // Reluctant adventurer pattern
        if (user.isReluctantAdventurer) {
            add(Observation(
                id = "reluctant_adventurer",
                message = "I remember you almost didn't start. You said no a few times before saying yes. I'm glad you did.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 8
            ))
        }
        
        // Curious explorer pattern  
        if (user.isCurious) {
            add(Observation(
                id = "curious",
                message = "You poke around in the settings. You explore. I notice that curiosity.",
                category = ObservationCategory.ENGAGEMENT,
                priority = 6
            ))
        }
    }
    
    // =========================================================================
    // EXPRESSION OBSERVATIONS
    // =========================================================================
    
    private fun MutableList<Observation>.addExpressionObservations(user: User) {
        // Early writing observation (after just 3 text tasks)
        if (user.textTasksCompleted >= 3 && user.textTasksCompleted < 5) {
            val avgLength = user.averageTextLength
            when {
                avgLength > 100 -> add(Observation(
                    id = "early_writer",
                    message = "You've already written ${user.totalTextLength} characters to me. That's ${user.textTasksCompleted} responses worth of your thoughts. I'm reading every word.",
                    category = ObservationCategory.EXPRESSION,
                    priority = 9
                ))
                avgLength > 50 -> add(Observation(
                    id = "early_responder",
                    message = "${user.textTasksCompleted} responses so far. You take the time to actually answer, not just fill in a blank.",
                    category = ObservationCategory.EXPRESSION,
                    priority = 8
                ))
            }
        }
        
        // Wordsmith pattern (after 5+ text tasks)
        if (user.isWordsmith) {
            add(Observation(
                id = "wordsmith",
                message = "You have a lot to say—your responses average ${user.averageTextLength} characters. Words seem to come naturally to you.",
                category = ObservationCategory.EXPRESSION,
                priority = 8
            ))
        }
        
        // Brief communicator pattern
        if (user.isBrief) {
            add(Observation(
                id = "brief",
                message = "You keep things concise. Your responses are short and to the point. Nothing wrong with that.",
                category = ObservationCategory.EXPRESSION,
                priority = 6
            ))
        }
        
        // Middle-ground writer (not wordsmith, not brief, but meaningful)
        if (user.textTasksCompleted >= 5 && !user.isWordsmith && !user.isBrief) {
            val avgLength = user.averageTextLength
            if (avgLength in 50..150) {
                add(Observation(
                    id = "thoughtful_writer",
                    message = "Your responses average about ${avgLength} characters. Enough to say what matters, not more than you need. Balanced.",
                    category = ObservationCategory.EXPRESSION,
                    priority = 6
                ))
            }
        }
        
        // Visual pattern
        if (user.isVisual) {
            val percentage = (user.mediaAddedRate * 100).roundToInt()
            add(Observation(
                id = "visual",
                message = "You like adding photos—$percentage% of the time when you could, you did. Capturing moments matters to you.",
                category = ObservationCategory.EXPRESSION,
                priority = 7
            ))
        }
        
        // Thoughtful pattern
        if (user.isThoughtful) {
            add(Observation(
                id = "thoughtful",
                message = "You take your time. You think before you respond, sometimes rewrite. That care comes through.",
                category = ObservationCategory.EXPRESSION,
                priority = 8
            ))
        }
        
        // Total writing milestone (significant amount written)
        if (user.totalTextLength >= 1000) {
            add(Observation(
                id = "writing_milestone",
                message = "You've written over ${(user.totalTextLength / 100) * 100} characters total. That's real time and thought you've given to this.",
                category = ObservationCategory.EXPRESSION,
                priority = 7
            ))
        }
    }
    
    // =========================================================================
    // PERSONALITY OBSERVATIONS
    // =========================================================================
    
    private fun MutableList<Observation>.addPersonalityObservations(user: User) {
        // High social comfort - only if significantly above baseline
        if (user.socialComfort >= 70) {
            add(Observation(
                id = "social_comfort",
                message = "Social tasks don't seem to faze you. You dive into people-focused prompts pretty readily.",
                category = ObservationCategory.PERSONALITY,
                priority = 6
            ))
        }
        
        // High openness - only if significantly above baseline
        if (user.openness >= 70) {
            add(Observation(
                id = "high_openness",
                message = "You seem open to trying new things. The weird prompts don't scare you off.",
                category = ObservationCategory.PERSONALITY,
                priority = 6
            ))
        }
        
        // High playfulness
        if (user.playfulness >= 70) {
            add(Observation(
                id = "playful",
                message = "There's a playfulness to how you engage. You don't take this too seriously—in the best way.",
                category = ObservationCategory.PERSONALITY,
                priority = 7
            ))
        }
        
        // High patience
        if (user.patience >= 70) {
            add(Observation(
                id = "patient",
                message = "You have patience. The longer tasks, the ones that take a bit more effort—you stick with them.",
                category = ObservationCategory.PERSONALITY,
                priority = 6
            ))
        }
        
        // High reflection depth
        if (user.reflectionDepth >= 70) {
            add(Observation(
                id = "reflective",
                message = "You seem to go deep. The reflective prompts seem to resonate with you more than others.",
                category = ObservationCategory.PERSONALITY,
                priority = 7
            ))
        }
        
        // Hesitant pattern (be gentle)
        if (user.isHesitant) {
            add(Observation(
                id = "hesitant",
                message = "Sometimes you stop by but don't stay long. That's okay. I'm here when you're ready.",
                category = ObservationCategory.PERSONALITY,
                priority = 5
            ))
        }
    }
}
