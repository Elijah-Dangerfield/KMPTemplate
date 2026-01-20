package com.dangerfield.goodtimes.features.tasks.impl.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangerfield.goodtimes.libraries.goodtimes.TaskCompletionResult
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import com.dangerfield.goodtimes.libraries.goodtimes.TaskResponse
import com.dangerfield.goodtimes.libraries.goodtimes.TaskSignal
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpResult
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Base ViewModel for all task screens.
 * Handles timing, signal computation, and completion flow.
 *
 * @param S The state type for this task screen
 * @param A The action type for this task screen
 *
 * TODO: Should this not be a SEA view model? Seems like alot of this is setup for that.
 * I imagine we might want a base Event thing that has TaskCompleted that can be observed properly.
 */
abstract class BaseTaskViewModel<S : TaskScreenState, A : Any>(
    protected val task: Task,
    initialState: S,
) : ViewModel() {

    private val startedAt: Long = Clock.System.now().toEpochMilliseconds()
    
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _completionResult = Channel<TaskCompletionResult>(Channel.BUFFERED)
    val completionResult = _completionResult.receiveAsFlow()

    /**
     * Update the current state.
     */
    protected fun updateState(transform: (S) -> S) {
        _state.update(transform)
    }

    /**
     * Get the current state value.
     */
    protected val currentState: S get() = _state.value

    /**
     * Handle an action from the UI.
     */
    abstract fun onAction(action: A)

    /**
     * Compute behavioral signals based on how the user completed the task.
     * Override to add task-specific signal logic.
     */
    protected open fun computeSignals(): List<TaskSignal> = emptyList()

    /**
     * Complete the task with the given response and outcome.
     * This triggers the completion flow and notifies the session controller.
     */
    protected fun complete(
        response: TaskResponse?,
        outcome: TaskOutcome,
        followUpResult: FollowUpResult? = null,
    ) {
        viewModelScope.launch {
            val timeSpentMs = Clock.System.now().toEpochMilliseconds() - startedAt
            
            val result = TaskCompletionResult(
                taskId = task.id,
                outcome = outcome,
                response = response,
                followUpResult = followUpResult,
                timeSpentMs = timeSpentMs,
                signals = computeSignals(),
            )
            
            _completionResult.send(result)
        }
    }

    /**
     * Get elapsed time since task started, in milliseconds.
     */
    protected fun getElapsedMs(): Long {
        return Clock.System.now().toEpochMilliseconds() - startedAt
    }
}

/**
 * Marker interface for task screen state.
 */
interface TaskScreenState {
    val isLoading: Boolean
}
