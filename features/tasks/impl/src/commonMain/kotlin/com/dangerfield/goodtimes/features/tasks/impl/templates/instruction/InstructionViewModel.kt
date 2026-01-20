package com.dangerfield.goodtimes.features.tasks.impl.templates.instruction

import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import com.dangerfield.goodtimes.libraries.goodtimes.TaskResponse
import com.dangerfield.goodtimes.libraries.goodtimes.TaskSignal
import com.dangerfield.goodtimes.features.tasks.impl.base.BaseTaskViewModel
import com.dangerfield.goodtimes.features.tasks.impl.base.TaskScreenState
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpConfig
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpResult
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpType
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

/**
 * ViewModel for INSTRUCTION tasks.
 * These are "go do this" tasks where the user marks completion.
 * May have follow-up flows to gather context about the experience.
 */
@Inject
class InstructionViewModel(
    @Assisted task: Task,
) : BaseTaskViewModel<InstructionState, InstructionAction>(
    task = task,
    initialState = InstructionState(
        instruction = task.instruction,
        hasFollowUp = task.followUp != null,
        followUpConfig = task.followUp,
    )
) {
    
    override fun onAction(action: InstructionAction) {
        when (action) {
            is InstructionAction.MarkDone -> handleMarkDone()
            is InstructionAction.SubmitFollowUp -> handleFollowUpSubmit(action.result)
            is InstructionAction.DismissFollowUp -> handleFollowUpDismiss()
        }
    }
    
    private fun handleMarkDone() {
        val followUp = currentState.followUpConfig
        
        if (followUp != null && shouldShowFollowUp(followUp)) {
            // Show follow-up flow
            updateState { it.copy(showFollowUp = true, currentFollowUpType = followUp.type) }
        } else {
            // No follow-up, complete immediately
            complete(TaskResponse.None, TaskOutcome.COMPLETED)
        }
    }
    
    private fun shouldShowFollowUp(config: FollowUpConfig): Boolean {
        // If required, always show
        if (config.required) return true
        
        // Otherwise, decide based on context
        // For now, show if completion was fast (might not have done it)
        val elapsedMs = getElapsedMs()
        return elapsedMs < 10_000 // Less than 10 seconds - probably should ask
    }
    
    private fun handleFollowUpSubmit(result: FollowUpResult) {
        val outcome = when {
            result.didComplete == false && result.selectedOptionId != null -> {
                // User said they didn't do it and selected an option
                val option = currentState.followUpConfig?.options?.find { it.id == result.selectedOptionId }
                when {
                    option?.reschedule == true -> TaskOutcome.SKIPPED_RESCHEDULE
                    option?.skipPermanent == true -> TaskOutcome.SKIPPED_PERMANENT
                    else -> TaskOutcome.COMPLETED
                }
            }
            else -> TaskOutcome.COMPLETED
        }
        
        complete(TaskResponse.None, outcome, result)
    }
    
    private fun handleFollowUpDismiss() {
        // User dismissed follow-up, complete without follow-up data
        complete(TaskResponse.None, TaskOutcome.COMPLETED)
    }
    
    override fun computeSignals(): List<TaskSignal> = buildList {
        val elapsedMs = getElapsedMs()
        
        // Trust signal - very fast completion might indicate they didn't do it
        when {
            elapsedMs < 5_000 -> add(TaskSignal("TASK_HONESTY", -1, "Completed very quickly"))
            elapsedMs > 60_000 -> add(TaskSignal("TASK_HONESTY", +1, "Took time with task"))
        }
        
        // If it's a social task and they completed it, positive social signal
        if (task.requiresSocial) {
            add(TaskSignal("SOCIAL_COMFORT", +1, "Completed social task"))
        }
        
        // Difficulty acceptance
        when (task.difficulty) {
            com.dangerfield.goodtimes.libraries.goodtimes.Difficulty.HEAVY -> {
                add(TaskSignal("DISCOMFORT_TOLERANCE", +2, "Completed difficult task"))
            }
            com.dangerfield.goodtimes.libraries.goodtimes.Difficulty.MEDIUM -> {
                add(TaskSignal("DISCOMFORT_TOLERANCE", +1, "Completed moderate task"))
            }
            else -> {}
        }
    }
}

data class InstructionState(
    val instruction: String,
    val hasFollowUp: Boolean,
    val followUpConfig: FollowUpConfig? = null,
    val showFollowUp: Boolean = false,
    val currentFollowUpType: FollowUpType? = null,
    override val isLoading: Boolean = false,
) : TaskScreenState

sealed class InstructionAction {
    data object MarkDone : InstructionAction()
    data class SubmitFollowUp(val result: FollowUpResult) : InstructionAction()
    data object DismissFollowUp : InstructionAction()
}
