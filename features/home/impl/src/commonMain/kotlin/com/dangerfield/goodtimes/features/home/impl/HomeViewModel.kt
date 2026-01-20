package com.dangerfield.goodtimes.features.home.impl

import androidx.lifecycle.viewModelScope
import com.dangerfield.goodtimes.features.home.impl.HomeEvent.*
import com.dangerfield.goodtimes.libraries.goodtimes.TaskCompletionResult
import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.GetNextTaskUseCase
import com.dangerfield.goodtimes.libraries.goodtimes.Reaction
import com.dangerfield.goodtimes.libraries.goodtimes.ReactionContext
import com.dangerfield.goodtimes.libraries.goodtimes.ScoreDimension
import com.dangerfield.goodtimes.libraries.goodtimes.Session
import com.dangerfield.goodtimes.libraries.goodtimes.SessionRepository
import com.dangerfield.goodtimes.libraries.goodtimes.Signal
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import com.dangerfield.goodtimes.libraries.goodtimes.TaskReactionEngine
import com.dangerfield.goodtimes.libraries.goodtimes.TaskRepository
import com.dangerfield.goodtimes.libraries.goodtimes.TaskResponse
import com.dangerfield.goodtimes.libraries.goodtimes.TaskType
import com.dangerfield.goodtimes.libraries.goodtimes.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val USELESS_BUTTON_MAX_CLICKS = 10

// Thinking delay range - varies to feel organic, not mechanical
private val THINKING_DELAY_MIN = 800.milliseconds
private val THINKING_DELAY_MAX = 2000.milliseconds

/**
 * Task flow follows an FSM pattern:
 * 
 * ```
 * [Loading] -> [ShowingTask] -> (task completed) -> [ShowingReaction]? -> [Loading] -> ...
 *                    ^                                      |
 *                    |______________________________________|
 *                              (reaction dismissed)
 * ```
 * 
 * Reactions are treated as part of the flow, not separate navigation destinations.
 * When a task completes, we check if a reaction is warranted. If so, we transition
 * to ShowingReaction state. When the user dismisses the reaction, we load the next task.
 */
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val appCache: AppCache,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val getNextTask: GetNextTaskUseCase,
    private val taskReactionEngine: TaskReactionEngine,
    private val thinkingMessageProvider: ThinkingMessageProvider,
    private val clock: Clock,
) : SEAViewModel<HomeState, HomeEvent, HomeAction>(
    initialStateArg = HomeState()
) {

    private var hasShownMoodPromptThisSession = false
    private var tasksCompletedThisSession = 0
    private var skipsThisSession = 0
    private var consecutiveSkips = 0
    private var lastOutcomeWasSkip = false
    private val recentReactionIds = mutableListOf<String>()
    private var currentSessionNumber: Int? = null
    
    // Controls when mood prompts appear - waits for 1-3 tasks before first prompt, 6hr gap
    private val moodPromptTiming = MoodPromptTiming(clock = clock)

    init {
        viewModelScope.launch {
            // Ensure user entity exists before any operations
            userRepository.ensureUserExists()
            taskRepository.initialize()
            takeAction(HomeAction.LoadNextTask)
        }

        // Re-evaluate mood prompt when session state changes
        // Also detect new sessions to reset timing
        combine(
            sessionRepository.currentSession,
            sessionRepository.moodBannerDisabled,
            sessionRepository.moodBannerDismissCount,
            sessionRepository.hasEverAnsweredMood,
            sessionRepository.lastMoodInteractionAt,
        ) { session, _, _, _, _ -> session }
            .distinctUntilChanged()
            .onEach { session ->
                // Detect session change and reset timing
                if (session != null && session.sessionNumber != currentSessionNumber) {
                    val isFirstSession = session.sessionNumber == 1
                    currentSessionNumber = session.sessionNumber
                    hasShownMoodPromptThisSession = false
                    moodPromptTiming.resetForNewSession(isFirstSession = isFirstSession)
                }
                // Check if we should show mood prompt
                checkAndShowMoodPromptIfNeeded()
            }
            .launchIn(viewModelScope)
            
        appCache.updates
            .onEach { appData ->
                takeAction(HomeAction.ShowUselessButton(appData.uselessButtonClicks < USELESS_BUTTON_MAX_CLICKS))
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Checks if conditions are right to show the mood prompt and shows it if so.
     * Called both from flow updates and after task interactions.
     */
    private suspend fun checkAndShowMoodPromptIfNeeded() {
        if (hasShownMoodPromptThisSession) return
        
        val session = sessionRepository.currentSession.value ?: return
        if (session.mood != null || session.moodDismissed) return
        
        val appData = appCache.get()
        if (appData.moodBannerDisabled) return
        
        // All timing logic is centralized in MoodPromptTiming
        val totalTaskInteractions = tasksCompletedThisSession + skipsThisSession
        if (!moodPromptTiming.shouldAllowMoodPrompt(
            tasksInteractedWith = totalTaskInteractions,
            lastMoodInteractionAt = appData.lastMoodInteractionAt
        )) return
        
        // All conditions met - show the mood prompt
        hasShownMoodPromptThisSession = true
        sendEvent(HomeEvent.ShowMoodBottomSheet(
            dismissCount = appData.moodBannerDismissCount,
            sessionNumber = session.sessionNumber,
            isFirstEverMoodPrompt = !appData.hasEverAnsweredMood
        ))
    }

    override suspend fun handleAction(action: HomeAction) {
        when (action) {
            is HomeAction.ClickUselessButton -> {
                val newCount = appCache.update { 
                    it.copy(uselessButtonClicks = it.uselessButtonClicks + 1) 
                }.uselessButtonClicks
                
                sendEvent(NavigateToUselessButtonDialog(clickCount = newCount))
            }

            is HomeAction.ShowUselessButton -> {
                action.updateState { it.copy(showUselessButton = action.show) }
            }
            
            is HomeAction.LoadNextTask -> {
                action.loadNextTaskWithThinking(
                    isFirstTask = tasksCompletedThisSession == 0 && skipsThisSession == 0,
                    justSkipped = lastOutcomeWasSkip,
                    justCompleted = !lastOutcomeWasSkip && tasksCompletedThisSession > 0,
                )
            }
            
            is HomeAction.TaskCompleted -> {
                val currentFlowState = state.taskFlowState
                val currentTask = (currentFlowState as? TaskFlowState.ShowingTask)?.task ?: return
                
                // Track completion vs skip patterns
                val wasSkipped = action.result.outcome != TaskOutcome.COMPLETED
                lastOutcomeWasSkip = wasSkipped
                if (wasSkipped) {
                    skipsThisSession++
                    consecutiveSkips++
                } else {
                    tasksCompletedThisSession++
                    consecutiveSkips = 0 // Reset consecutive skip counter on completion
                }
                
                // Record to user repository for personality/affinity updates
                recordTaskResult(action.result, currentTask, wasSkipped)
                
                // Check if we should show mood prompt after this task interaction
                checkAndShowMoodPromptIfNeeded()
                
                // Build context for reaction consideration
                
                // Build context for reaction consideration
                val session = sessionRepository.currentSession.value
                /*
                Thoughts for improvement:
                TBH seems like we need inheritance for context
                Like the base one might have isLateNight and userObservations and stuff and then this
                Reaction one would add onto it with consecutive skips and stuff
                 */
                val context = ReactionContext(
                    task = currentTask,
                    sessionNumber = session?.sessionNumber ?: 1,
                    tasksCompletedThisSession = tasksCompletedThisSession,
                    totalTasksCompleted = tasksCompletedThisSession,
                    skipsThisSession = skipsThisSession,
                    consecutiveSkips = consecutiveSkips,
                    currentMood = session?.mood,
                    isLateNight = isLateNight(),
                    isFirstSession = session?.sessionNumber == 1,
                    recentReactionIds = recentReactionIds.toList(),
                )
                
                val reaction = taskReactionEngine.considerReaction(action.result, context)
                
                if (reaction != null) {
                    // Transition to showing reaction - next task loads when reaction is dismissed
                    action.updateState { it.copy(taskFlowState = TaskFlowState.ShowingReaction(reaction)) }
                } else {
                    // No reaction, load next task with thinking animation
                    action.loadNextTaskWithThinking(
                        isFirstTask = false,
                        justSkipped = wasSkipped,
                        justCompleted = !wasSkipped,
                    )
                }
            }
            
            is HomeAction.ReactionDismissed -> {
                // User acknowledged the reaction, now load next task with thinking
                action.loadNextTaskWithThinking(
                    isFirstTask = false,
                    justSkipped = lastOutcomeWasSkip,
                    justCompleted = !lastOutcomeWasSkip,
                )
            }
        }
    }
    
    /**
     * Shows a "thinking" message while loading the next task.
     * The delay and message make it feel like the app is genuinely considering
     * what to show, not just pulling from a queue.
     */
    private suspend fun HomeAction.loadNextTaskWithThinking(
        isFirstTask: Boolean,
        justSkipped: Boolean,
        justCompleted: Boolean,
    ) {
        val session = sessionRepository.currentSession.value
        val thinkingContext = ThinkingContext(
            isFirstTask = isFirstTask,
            justCompletedTask = justCompleted,
            justSkipped = justSkipped,
            consecutiveSkips = consecutiveSkips,
            tasksCompletedThisSession = tasksCompletedThisSession,
            currentMood = session?.mood,
            isLateNight = isLateNight(),
            sessionNumber = session?.sessionNumber ?: 1,
        )
        
        val thinkingMessage = thinkingMessageProvider.getThinkingMessage(thinkingContext)
        updateState { it.copy(taskFlowState = TaskFlowState.Loading(thinkingMessage)) }
        
        // Variable delay to feel organic - longer after completions, shorter after skips
        val baseDelay = if (justSkipped) {
            THINKING_DELAY_MIN
        } else {
            THINKING_DELAY_MIN + (THINKING_DELAY_MAX - THINKING_DELAY_MIN) / 2
        }
        val variance = ((THINKING_DELAY_MAX - THINKING_DELAY_MIN).inWholeMilliseconds * Random.nextFloat()).toLong()
        delay(baseDelay + variance.milliseconds)
        
        val task = getNextTask()
        if (task != null) {
            updateState { it.copy(taskFlowState = TaskFlowState.ShowingTask(task)) }
        }
        /*
        Okay so what would we do here? i'd rather it be a defninative: You finished rather than just running out of tasks
        like how do I know we didnt just hit some error? like the DB was dumped? How do we know if the user really finished tho?
        id like to have the algorithm be able to check that. Probably by being able to say: all the tasks we Would give the user have either been completed or removed from the pool
        and if we have none but that isnt the case then we have some type of error.
         */
        // TODO: Handle no more tasks (end state)
    }
    
    /**
     * Record task completion/skip to UserRepository for personality/affinity tracking.
     * Converts TaskSignals to Signals and passes along metadata for analysis.
     */
    private fun recordTaskResult(result: TaskCompletionResult, task: Task, wasSkipped: Boolean) {
        viewModelScope.launch {
            // Convert TaskSignals to domain Signals
            // TaskSignal uses String dimension + adjustment, Signal uses ScoreDimension + delta
            val signals = result.signals.mapNotNull { taskSignal ->
                val dimension = try {
                    ScoreDimension.valueOf(taskSignal.dimension)
                } catch (e: IllegalArgumentException) {
                    null // Skip unknown dimensions
                }
                dimension?.let {
                    Signal(
                        dimension = it,
                        delta = taskSignal.adjustment,
                    )
                }
            }
            
            val isTextTask = task.type == TaskType.PROMPT && task.responseStyle.allowsText
            val characterCount = when (val response = result.response) {
                is TaskResponse.Text -> response.value.length
                is TaskResponse.Compound -> response.text?.length
                else -> null
            }
            
            if (wasSkipped) {
                userRepository.onTaskSkipped(
                    taskId = result.taskId,
                    signals = signals,
                )
            } else {
                userRepository.onTaskCompleted(
                    taskId = result.taskId,
                    signals = signals,
                    responseTimeMs = result.timeSpentMs,
                    characterCount = characterCount,
                    isTextTask = isTextTask,
                )
            }
        }
    }

    // TODO should we not get this from like a context repository?
    // maybe instead of CopyContext its AwarenessContext and it comes from a repo.
    // maybe we even have a base Context type?
    private fun isLateNight(): Boolean {
        val nowMs = clock.now().toEpochMilliseconds()
        val hour = ((nowMs / 3600000) % 24).toInt()
        return hour !in 5..<22
    }
}

data class HomeState(
    val showUselessButton: Boolean = true,
    val taskFlowState: TaskFlowState = TaskFlowState.Loading(),
)

/**
 * Represents the current state of the task flow FSM.
 */
sealed class TaskFlowState {
    /** 
     * Loading the next task - shows a "thinking" message to make it feel alive.
     * The message changes based on context to feel dynamic, not mechanical.
     */
    data class Loading(val thinkingMessage: String = "...") : TaskFlowState()
    
    /** Displaying a task for the user to complete */
    data class ShowingTask(val task: Task) : TaskFlowState()
    
    /** Showing a reaction to the user's task completion */
    data class ShowingReaction(val reaction: Reaction) : TaskFlowState()
}

sealed class HomeEvent {
    data class ShowMoodBottomSheet(
        val dismissCount: Int,
        val sessionNumber: Int,
        val isFirstEverMoodPrompt: Boolean,
    ) : HomeEvent()
    
    data class NavigateToUselessButtonDialog(
        val clickCount: Int,
    ) : HomeEvent()
}

sealed class HomeAction {
    data object ClickUselessButton : HomeAction()
    data class ShowUselessButton(val show: Boolean): HomeAction()
    data object LoadNextTask : HomeAction()
    data class TaskCompleted(val result: TaskCompletionResult) : HomeAction()
    data object ReactionDismissed : HomeAction()
}
