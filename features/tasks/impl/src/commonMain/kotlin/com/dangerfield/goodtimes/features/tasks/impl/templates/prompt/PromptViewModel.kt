package com.dangerfield.goodtimes.features.tasks.impl.templates.prompt

import com.dangerfield.goodtimes.libraries.goodtimes.MediaType
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import com.dangerfield.goodtimes.libraries.goodtimes.TaskResponse
import com.dangerfield.goodtimes.libraries.goodtimes.TaskSignal
import com.dangerfield.goodtimes.features.tasks.impl.base.BaseTaskViewModel
import com.dangerfield.goodtimes.features.tasks.impl.base.TaskScreenState
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import kotlin.time.Clock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class PromptViewModel(
    @Assisted task: Task,
) : BaseTaskViewModel<PromptState, PromptAction>(
    task = task,
    initialState = PromptState(
        instruction = task.instruction,
        placeholder = task.placeholder ?: "",
        allowsPhoto = task.responseStyle.allowsPhoto,
        allowsAudio = task.responseStyle.allowsAudio,
        requiresDepth = task.requiresDepth,
        minCharacters = task.minCharacters ?: if (task.requiresDepth) DEFAULT_MIN_CHARACTERS else 0,
        isIntroTask = task.isIntroTask,
    )
) {
    
    private var firstKeystrokeAt: Long? = null
    private var deleteCount: Int = 0
    private var pasteDetected: Boolean = false
    private var depthPromptDismissed: Boolean = false
    
    override fun onAction(action: PromptAction) {
        when (action) {
            is PromptAction.UpdateText -> handleTextUpdate(action.text)
            is PromptAction.Submit -> handleSubmit()
            is PromptAction.AttachPhoto -> handleAttachPhoto(action.path)
            is PromptAction.RemovePhoto -> handleRemovePhoto()
            is PromptAction.DismissDepthPrompt -> handleDismissDepthPrompt(action.continueAnyway)
        }
    }
    
    private fun handleTextUpdate(newText: String) {
        val oldText = currentState.text
        
        // Track first keystroke for hesitation signal
        if (firstKeystrokeAt == null && newText.isNotEmpty()) {
            firstKeystrokeAt = Clock.System.now().toEpochMilliseconds()
        }
        
        // Track deletions (potential signal for uncertainty)
        if (newText.length < oldText.length) {
            deleteCount++
        }
        
        // Detect paste (large text jump)
        if (newText.length - oldText.length > 10) {
            pasteDetected = true
        }
        
        updateState { it.copy(text = newText, showDepthPrompt = false) }
    }
    
    private fun handleSubmit() {
        val text = currentState.text.trim()
        if (text.isEmpty() && currentState.photoPath == null) return
        
        // Check if response is too short for a task that requires depth
        val needsMore = currentState.requiresDepth && 
            !depthPromptDismissed &&
            text.length < currentState.minCharacters &&
            currentState.photoPath == null
        
        if (needsMore) {
            // Show the depth prompt
            updateState { it.copy(showDepthPrompt = true, depthPromptMessage = getDepthPromptMessage()) }
            return
        }
        
        val response = if (currentState.photoPath != null) {
            TaskResponse.Compound(
                text = text.takeIf { it.isNotEmpty() },
                media = listOf(TaskResponse.Media(MediaType.PHOTO, currentState.photoPath!!))
            )
        } else {
            TaskResponse.Text(text)
        }
        
        complete(response, TaskOutcome.COMPLETED)
    }
    
    private fun handleDismissDepthPrompt(continueAnyway: Boolean) {
        updateState { it.copy(showDepthPrompt = false) }
        if (continueAnyway) {
            depthPromptDismissed = true
            handleSubmit() // Re-submit, this time it will go through
        }
        // If not continuing, just close the prompt and let them edit
    }
    
    private fun handleAttachPhoto(path: String) {
        updateState { it.copy(photoPath = path) }
    }
    
    private fun handleRemovePhoto() {
        updateState { it.copy(photoPath = null) }
    }
    
    private fun getDepthPromptMessage(): String {
        val messages = listOf(
            "Just that? I'm curious to hear more if you're up for it.",
            "Okay, but walk me through it a little?",
            "That's a start. What made you say that?",
            "Could you tell me more? Even a few more words help.",
            "I want to understand. Can you expand on that?",
        )
        return messages.random()
    }
    
    override fun computeSignals(): List<TaskSignal> = buildList {
        val text = currentState.text
        val hesitationMs = firstKeystrokeAt?.let { getElapsedMs() - (it - getElapsedMs()) }
        val expected = task.responseStyle.expectedLength
        
        // Writing affinity - calibrated to what the task expects
        // A one-word answer to "favorite word" is perfect, but lazy for "describe the sky"
        when {
            text.length >= expected.greatChars -> add(TaskSignal("WRITING_AFFINITY", +3, "Wrote extensively"))
            text.length >= expected.goodChars -> add(TaskSignal("WRITING_AFFINITY", +1, "Solid response"))
            text.length < expected.minChars && text.isNotEmpty() -> add(TaskSignal("WRITING_AFFINITY", -1, "Brief response"))
        }
        
        // Thoughtfulness - hesitation before writing can indicate reflection
        if (hesitationMs != null && hesitationMs > 30_000) {
            add(TaskSignal("REFLECTION_DEPTH", +1, "Paused before responding"))
        }
        
        // Uncertainty signal from excessive editing
        if (deleteCount > 10) {
            add(TaskSignal("HESITANCY", +1, "Edited response multiple times"))
        }
        
        // Speed signal - very fast completion might indicate low engagement
        val totalTimeMs = getElapsedMs()
        if (totalTimeMs < 5_000 && text.length > 50) {
            // Fast but substantial - could be pasted or very confident
            if (pasteDetected) {
                add(TaskSignal("ENGAGEMENT", -1, "Response appeared to be pasted"))
            }
        }
    }
    
    companion object {
        private const val DEFAULT_MIN_CHARACTERS = 20
    }
}

data class PromptState(
    val instruction: String,
    val placeholder: String,
    val text: String = "",
    val photoPath: String? = null,
    val allowsPhoto: Boolean = false,
    val allowsAudio: Boolean = false,
    val requiresDepth: Boolean = false,
    val minCharacters: Int = 0,
    val isIntroTask: Boolean = false,
    val showDepthPrompt: Boolean = false,
    val depthPromptMessage: String = "",
    override val isLoading: Boolean = false,
) : TaskScreenState

sealed class PromptAction {
    data class UpdateText(val text: String) : PromptAction()
    data object Submit : PromptAction()
    data class AttachPhoto(val path: String) : PromptAction()
    data object RemovePhoto : PromptAction()
    data class DismissDepthPrompt(val continueAnyway: Boolean) : PromptAction()
}
