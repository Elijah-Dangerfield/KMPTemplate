package com.dangerfield.goodtimes.libraries.goodtimes.impl

import com.dangerfield.goodtimes.libraries.core.logging.KLog
import com.dangerfield.goodtimes.libraries.goodtimes.AwarenessContext
import com.dangerfield.goodtimes.libraries.goodtimes.Difficulty
import com.dangerfield.goodtimes.libraries.goodtimes.GetAwarenessContextUseCase
import com.dangerfield.goodtimes.libraries.goodtimes.GetNextTaskUseCase
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.MoodTrend
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.goodtimes.libraries.goodtimes.TaskCategory
import com.dangerfield.goodtimes.libraries.goodtimes.TaskRepository
import com.dangerfield.goodtimes.libraries.goodtimes.TaskType
import com.dangerfield.goodtimes.libraries.goodtimes.User
import com.dangerfield.goodtimes.libraries.goodtimes.UserRepository
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Implementation that uses a fixed sequence for the first [CALIBRATION_THRESHOLD] tasks,
 * then uses context-aware adaptive selection.
 * 
 * The fixed sequence solves the cold start problem:
 * - We can't personalize without signals
 * - These 15 tasks touch multiple dimensions for calibration
 * - They vary task types to discover user preferences
 * - They stay light early and build trust before harder asks
 * 
 * After calibration, we use [AwarenessContext] to score tasks by:
 * - Mood appropriateness (filter tasks that don't fit current/trending mood)
 * - Time conditions (late night tasks, morning tasks)
 * - Response style affinity (match user's writing/photo/audio preferences)
 * - Difficulty appropriateness (harder tasks for engaged users)
 * - Category variety (don't repeat same category back-to-back)
 * - Device capabilities (can't do photo tasks without camera permission)
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class GetNextTaskUseCaseImpl(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val getAwarenessContext: GetAwarenessContextUseCase,
) : GetNextTaskUseCase {

    private val logger = KLog.withTag("GetNextTaskUseCase")
    
    // Track recently shown categories to encourage variety
    private var recentCategories: MutableList<TaskCategory> = mutableListOf()

    override suspend fun invoke(): Task? {
        val tasks = taskRepository.getAllTasks()
        val user = userRepository.getUser()
        val completedCount = user?.tasksCompleted ?: 0
        
        logger.d("Task selection: tasksCompleted=$completedCount, sequenceSize=${FIXED_SEQUENCE.size}")
        
        if (tasks.isEmpty()) {
            logger.w("No tasks available")
            return null
        }
        
        // Use fixed sequence until complete, then switch to adaptive
        return if (completedCount < FIXED_SEQUENCE.size) {
            selectFromFixedSequence(tasks, completedCount)
        } else {
            selectAdaptive(tasks)
        }
    }
    
    /**
     * Select from the fixed intro/calibration sequence.
     * These tasks are ordered intentionally to:
     * - Start with a warm welcome
     * - Vary task types (prompt, selection, hold, photo, drawing, audio)
     * - Touch different calibration dimensions
     * - Start light and gradually introduce variety
     * - Build trust before asking harder things
     */
    private suspend fun selectFromFixedSequence(tasks: List<Task>, completedCount: Int): Task? {
        val taskMap = tasks.associateBy { it.id }
        
        // Find the next task in sequence that exists
        for (i in completedCount until FIXED_SEQUENCE.size) {
            val taskId = FIXED_SEQUENCE[i]
            val task = taskMap[taskId]
            if (task != null) {
                logger.d("Fixed sequence: returning task '$taskId' (position ${i + 1}/${FIXED_SEQUENCE.size})")
                return task
            } else {
                logger.w("Fixed sequence: task '$taskId' not found, skipping")
            }
        }
        
        // If we've exhausted the sequence (shouldn't happen normally), fall back to adaptive
        logger.w("Fixed sequence exhausted before reaching threshold, falling back to adaptive")
        return selectAdaptive(tasks)
    }
    
    /**
     * Adaptive selection using AwarenessContext for smart scoring.
     * 
     * Pipeline:
     * 1. Filter out tasks that are impossible (device capabilities, time conditions)
     * 2. Filter out tasks inappropriate for current mood
     * 3. Score remaining tasks by multiple factors
     * 4. Select the highest scoring task
     */
    private suspend fun selectAdaptive(tasks: List<Task>): Task? {
        val context = getAwarenessContext()
        val user = userRepository.getUser()
        
        // Step 1: Filter by hard requirements (device, time, etc.)
        val eligible = tasks.filter { task -> 
            isTaskEligible(task, context) 
        }
        
        if (eligible.isEmpty()) {
            logger.w("No eligible tasks after filtering")
            // Fall back to any task as last resort
            return tasks.firstOrNull()
        }
        
        // Step 2: Score each eligible task
        val scored = eligible.map { task ->
            val score = scoreTask(task, context, user)
            logger.v("Task '${task.id}' scored $score")
            task to score
        }
        
        // Step 3: Select highest scoring task
        val selected = scored.maxByOrNull { it.second }?.first
        
        if (selected != null) {
            // Track the category for variety calculation
            selected.categories.firstOrNull()?.let { cat ->
                recentCategories.add(0, cat)
                if (recentCategories.size > VARIETY_WINDOW) {
                    recentCategories.removeLast()
                }
            }
            logger.d("Adaptive: selected '${selected.id}' (score: ${scored.find { it.first == selected }?.second})")
        }
        
        return selected
    }
    
    // =========================================================================
    // ELIGIBILITY CHECKS (hard filters - task is impossible or inappropriate)
    // =========================================================================
    
    /**
     * Check if a task is even possible given current context.
     */
    private fun isTaskEligible(task: Task, ctx: AwarenessContext): Boolean {
        // Device capability checks
        if (task.type == TaskType.PHOTO_CAPTURE && !ctx.canCapturePhoto) {
            logger.v("Task '${task.id}' ineligible: no camera access")
            return false
        }
        
        if (task.type == TaskType.AUDIO_CAPTURE && !ctx.canRecordAudio) {
            logger.v("Task '${task.id}' ineligible: no microphone access")
            return false
        }
        
        // Time condition checks
        task.conditions?.let { conditions ->
            if (!checkTimeConditions(conditions.timeAfter, conditions.timeBefore, ctx)) {
                logger.v("Task '${task.id}' ineligible: time conditions not met")
                return false
            }
            
            // Date condition checks (month range)
            conditions.monthRange?.let { monthRange ->
                val currentMonth = ctx.time.month
                if (!monthRange.contains(currentMonth)) {
                    logger.v("Task '${task.id}' ineligible: month $currentMonth not in range ${monthRange.startMonth}-${monthRange.endMonth}")
                    return false
                }
            }
            
            // Date condition checks (day of month range)
            conditions.dayOfMonthRange?.let { dayRange ->
                val currentDay = ctx.time.dayOfMonth
                if (!dayRange.contains(currentDay)) {
                    logger.v("Task '${task.id}' ineligible: day $currentDay not in range ${dayRange.startDay}-${dayRange.endDay}")
                    return false
                }
            }
            
            // Mood trend conditions
            conditions.requiresMoodTrend?.let { required ->
                if (ctx.moodTrend != required) {
                    logger.v("Task '${task.id}' ineligible: requires mood trend $required, have ${ctx.moodTrend}")
                    return false
                }
            }
        }
        
        // Mood appropriateness (hard filter for BAD mood)
        ctx.currentMood?.let { mood ->
            if (mood == Mood.BAD && task.avoidForMoods?.contains(Mood.BAD) == true) {
                logger.v("Task '${task.id}' ineligible: avoided for BAD mood")
                return false
            }
        }
        
        // Social tasks need specific context
        if (task.requiresSocial && ctx.isLateNight) {
            // Social tasks at 2am are unlikely to be completable
            logger.v("Task '${task.id}' ineligible: social task at late night")
            return false
        }
        
        return true
    }
    
    private fun checkTimeConditions(timeAfter: String?, timeBefore: String?, ctx: AwarenessContext): Boolean {
        if (timeAfter == null && timeBefore == null) return true
        
        val currentHour = ctx.time.hour
        val afterHour = timeAfter?.substringBefore(":")?.toIntOrNull()
        val beforeHour = timeBefore?.substringBefore(":")?.toIntOrNull()
        
        // Handle wrap-around (e.g., 22:00 to 05:00 means late night)
        return when {
            afterHour != null && beforeHour != null -> {
                if (afterHour > beforeHour) {
                    // Wrap around midnight: 22:00 to 05:00
                    currentHour >= afterHour || currentHour < beforeHour
                } else {
                    // Same day: 09:00 to 17:00
                    currentHour >= afterHour && currentHour < beforeHour
                }
            }
            afterHour != null -> currentHour >= afterHour
            beforeHour != null -> currentHour < beforeHour
            else -> true
        }
    }
    
    // =========================================================================
    // SCORING (soft preferences - higher score = better fit)
    // =========================================================================
    
    /**
     * Score a task based on how well it fits the current context.
     * Higher score = better fit. Range is roughly -50 to +50.
     */
    private fun scoreTask(task: Task, ctx: AwarenessContext, user: User?): Int {
        var score = 0
        
        // Mood match scoring
        score += scoreMoodMatch(task, ctx)
        
        // Response style affinity scoring
        score += scoreAffinityMatch(task, user)
        
        // Difficulty appropriateness
        score += scoreDifficultyMatch(task, ctx, user)
        
        // Category variety (penalize recently shown categories)
        score += scoreCategoryVariety(task)
        
        // Time-of-day bonuses
        score += scoreTimeMatch(task, ctx)
        
        // Personality match bonuses
        score += scorePersonalityMatch(task, ctx)
        
        return score
    }
    
    /**
     * Score based on mood match.
     * +10 if task is best for current mood
     * -10 if task should be avoided for current mood (but wasn't hard-filtered)
     */
    private fun scoreMoodMatch(task: Task, ctx: AwarenessContext): Int {
        val currentMood = ctx.currentMood ?: return 0
        
        var score = 0
        
        // Bonus for tasks suited to current mood
        if (task.bestForMoods?.contains(currentMood) == true) {
            score += 10
        }
        
        // Penalty for tasks to avoid (soft penalty since hard filter handles BAD)
        if (task.avoidForMoods?.contains(currentMood) == true) {
            score -= 10
        }
        
        // Mood trend considerations
        when (ctx.moodTrend) {
            MoodTrend.DECLINING -> {
                // Prefer lighter, safer tasks when mood is declining
                if (task.difficulty == Difficulty.LIGHT) score += 5
                if (task.difficulty == Difficulty.HEAVY) score -= 10
                if (task.safeToReflect) score += 5
            }
            MoodTrend.IMPROVING -> {
                // Can handle more variety when things are getting better
                if (task.difficulty == Difficulty.MEDIUM) score += 3
            }
            else -> {}
        }
        
        return score
    }
    
    /**
     * Score based on user's response style affinities.
     * Users who like writing get more prompts, visual users get more photo tasks, etc.
     */
    private fun scoreAffinityMatch(task: Task, user: User?): Int {
        if (user == null) return 0
        
        var score = 0
        val style = task.responseStyle
        
        // Writing affinity (50 is neutral, >50 means prefers writing)
        if (style.allowsText) {
            score += (user.writingAffinity - 50) / 10  // -5 to +5
        }
        
        // Photo affinity
        if (style.allowsPhoto || task.type == TaskType.PHOTO_CAPTURE) {
            score += (user.photoAffinity - 50) / 10
        }
        
        // Audio affinity
        if (style.allowsAudio || task.type == TaskType.AUDIO_CAPTURE) {
            score += (user.audioAffinity - 50) / 10
        }
        
        // Drawing affinity
        if (style.allowsDrawing || task.type == TaskType.DRAWING) {
            score += (user.drawingAffinity - 50) / 10
        }
        
        // Game affinity
        if (task.type == TaskType.GAME) {
            score += (user.gameAffinity - 50) / 10
        }
        
        return score
    }
    
    /**
     * Score based on difficulty appropriateness.
     * New users should get lighter tasks, veterans can handle more.
     */
    private fun scoreDifficultyMatch(task: Task, ctx: AwarenessContext, user: User?): Int {
        var score = 0
        
        // New users prefer light tasks
        if (ctx.isNewUser) {
            when (task.difficulty) {
                Difficulty.LIGHT -> score += 5
                Difficulty.MEDIUM -> score += 0
                Difficulty.HEAVY -> score -= 10
            }
        }
        
        // Veterans can handle anything, slight preference for variety
        if (ctx.isVeteranUser) {
            // No strong preference, let other factors dominate
        }
        
        // Patient users can handle stillness/hold tasks better
        if (user != null && user.patience > 60) {
            if (task.type == TaskType.STILLNESS || task.type == TaskType.HOLD_FINGER) {
                score += 5
            }
        }
        
        // Social comfort affects social tasks
        if (task.requiresSocial && user != null) {
            score += (user.socialComfort - 50) / 10
        }
        
        return score
    }
    
    /**
     * Penalize tasks from categories we've shown recently.
     * Encourages variety in the task stream.
     */
    private fun scoreCategoryVariety(task: Task): Int {
        val taskCategory = task.categories.firstOrNull() ?: return 0
        
        // Penalize if this category was shown recently
        val recentIndex = recentCategories.indexOf(taskCategory)
        return when (recentIndex) {
            0 -> -15  // Just showed this category
            1 -> -10  // Showed 2 tasks ago
            2 -> -5   // Showed 3 tasks ago
            else -> 0 // Not recent, no penalty
        }
    }
    
    /**
     * Time-of-day bonuses for contextually appropriate tasks.
     */
    private fun scoreTimeMatch(task: Task, ctx: AwarenessContext): Int {
        var score = 0
        
        // Late night bonuses
        if (ctx.isLateNight) {
            // Reflection tasks are good for quiet hours
            if (task.categories.contains(TaskCategory.REFLECTION)) score += 5
            // Stillness is natural at night
            if (task.categories.contains(TaskCategory.STILLNESS)) score += 5
            // Playful tasks feel weird at 3am
            if (task.categories.contains(TaskCategory.PLAY)) score -= 5
        }
        
        // Morning bonuses
        if (ctx.isMorning) {
            // Light tasks for morning energy
            if (task.difficulty == Difficulty.LIGHT) score += 3
        }
        
        // Weekend bonuses
        if (ctx.isWeekend) {
            // More open to social tasks on weekends
            if (task.requiresSocial) score += 5
            // More open to play on weekends
            if (task.categories.contains(TaskCategory.PLAY)) score += 3
        }
        
        return score
    }
    
    /**
     * Personality-based bonuses.
     */
    private fun scorePersonalityMatch(task: Task, ctx: AwarenessContext): Int {
        var score = 0
        
        // Curious users might enjoy more varied/unusual tasks
        if (ctx.isCurious) {
            if (task.type == TaskType.GAME) score += 3
            if (task.categories.contains(TaskCategory.DISCOMFORT)) score += 3
        }
        
        // Night owls get bonuses for late-night-suitable tasks when it's late
        if (ctx.isNightOwl && ctx.isLateNight) {
            if (task.categories.contains(TaskCategory.REFLECTION)) score += 5
        }
        
        // Thoughtful users prefer tasks with depth
        if (ctx.isThoughtful) {
            if (task.requiresDepth) score += 5
            if (task.responseStyle.allowsText) score += 3
        }
        
        // Visual users prefer photo/drawing tasks
        if (ctx.isVisual) {
            if (task.type == TaskType.PHOTO_CAPTURE || task.type == TaskType.DRAWING) {
                score += 5
            }
        }
        
        return score
    }
    
    companion object {
        /**
         * How many recent categories to track for variety scoring.
         */
        const val VARIETY_WINDOW = 3
        
        /**
         * Fixed sequence of task IDs for new users.
         * 
         * Design rationale for each position:
         * 0. intro_welcome - Warm welcome, sets the tone, one-word answer
         * 1. first_task - Simple observation, low barrier, gets them writing
         * 2. light_or_deep - Selection task, reveals preference, gives them agency
         * 3. hold_finger_30 - Non-verbal, tests patience, introduces app's quirky side
         * 4. three_sounds - Mindfulness-lite, easy to complete, varies the rhythm
         * 5. favorite_word - Playful prompt, shows the app isn't all serious
         * 6. draw_turtle_dark - Drawing task, playful, tests drawing affinity
         * 7. stranger_or_alone - Selection, reveals social comfort (key signal)
         * 8. photo_something_old - First photo task, low creative pressure
         * 9. describe_sky - Writing prompt, tests willingness for observation tasks
         * 10. grateful_for_small - Gentle reflection, positive framing
         * 11. hum_something - Audio task, tests audio affinity (some will skip)
         * 12. photo_your_hands - Photo, slightly more personal
         * 13. stillness_60 - Patience task (harder), by now they trust the app
         * 14. draw_your_mood - Drawing + reflection, calibration checkpoint
         * 15. used_to_be - Medium depth prompt, they've earned deeper questions
         */
        val FIXED_SEQUENCE = listOf(
            "intro_welcome",        // PROMPT - warm welcome, sets tone
            "first_task",           // PROMPT - observation, low barrier
            "light_or_deep",        // SELECTION - preference signal
            "hold_finger_30",       // HOLD_FINGER - patience, introduces quirky side
            "three_sounds",         // PROMPT - mindfulness, easy
            "favorite_word",        // PROMPT - playful, shows personality
            "draw_turtle_dark",     // DRAWING - drawing affinity
            "stranger_or_alone",    // SELECTION - social comfort signal
            "photo_something_old",  // PHOTO - photo affinity, low pressure
            "describe_sky",         // PROMPT - observation depth
            "grateful_for_small",   // PROMPT - gentle reflection
            "hum_something",        // AUDIO - audio affinity
            "photo_your_hands",     // PHOTO - slightly more personal
            "stillness_60",         // STILLNESS - patience (harder)
            "draw_your_mood",       // DRAWING - calibration checkpoint
            "used_to_be",           // PROMPT - medium depth, earned trust
        )
    }
}
