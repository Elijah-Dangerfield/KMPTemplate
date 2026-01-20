package com.dangerfield.goodtimes.features.tasks.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.goodtimes.libraries.goodtimes.TaskCompletionResult
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import com.dangerfield.goodtimes.features.tasks.impl.templates.instruction.InstructionScreen
import com.dangerfield.goodtimes.features.tasks.impl.templates.instruction.InstructionViewModel
import com.dangerfield.goodtimes.features.tasks.impl.templates.prompt.PromptScreen
import com.dangerfield.goodtimes.features.tasks.impl.templates.prompt.PromptViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.Difficulty
import com.dangerfield.goodtimes.libraries.goodtimes.ResponseStyle
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.goodtimes.libraries.goodtimes.TaskCategory
import com.dangerfield.goodtimes.libraries.goodtimes.TaskType
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.text.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Host composable that renders the appropriate screen for the current task.
 * Uses AnimatedContent for smooth transitions between tasks.
 *
 * @param task The current task to display, or null if loading
 * @param onTaskCompleted Callback when a task is completed
 * @param viewModelFactory Factory to create the appropriate ViewModel for each task
 */
@Composable
fun TaskHost(
    task: Task?,
    onTaskCompleted: (TaskCompletionResult) -> Unit,
    viewModelFactory: TaskViewModelFactory,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = task,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        contentKey = { it?.id },
    ) { currentTask ->
        if (currentTask == null) {
            TaskLoadingState()
        } else {
            // Key forces recomposition when task changes
            // QUESTION: is the key thing valid? Cause I figured it would recompose when
            // the task changed anyway?
            key(currentTask.id) {
                TaskContent(
                    task = currentTask,
                    viewModelFactory = viewModelFactory,
                    onTaskCompleted = onTaskCompleted,
                )
            }
        }
    }
}

@Composable
private fun TaskContent(
    task: Task,
    viewModelFactory: TaskViewModelFactory,
    onTaskCompleted: (TaskCompletionResult) -> Unit,
) {
    when (task.type) {
        TaskType.PROMPT -> {
            val viewModel = remember(task.id) { viewModelFactory.createPromptViewModel(task) }
            val state by viewModel.state.collectAsState()
            
            LaunchedEffect(viewModel) {
                viewModel.completionResult.collect { result ->
                    onTaskCompleted(result)
                }
            }
            
            PromptScreen(
                state = state,
                onAction = viewModel::onAction,
            )
        }
        
        TaskType.INSTRUCTION -> {
            val viewModel = remember(task.id) { viewModelFactory.createInstructionViewModel(task) }
            val state by viewModel.state.collectAsState()
            
            LaunchedEffect(viewModel) {
                viewModel.completionResult.collect { result ->
                    onTaskCompleted(result)
                }
            }
            
            InstructionScreen(
                state = state,
                onAction = viewModel::onAction,
            )
        }
        
        // Fallback for unimplemented task types
        /*
        TODO seems like this is where we might want a more extensive when clause
        cause the TaskTypes are much longer than these two
        and idk exactly how ill end up handling them all.

        in my head it really seems easiest to do a nav router and just use the task id
        otherwise idk how to picture it. Cause some of these will have their own one offs I think
        like not everything will have a shared set up. And I want a nice and easy organization to the
        tasks.
         */
        else -> {
            PlaceholderTaskScreen(
                task = task,
                onComplete = {
                    onTaskCompleted(
                        TaskCompletionResult(
                            taskId = task.id,
                            outcome = TaskOutcome.COMPLETED,
                            response = null,
                            followUpResult = null,
                            timeSpentMs = 0,
                            signals = emptyList(),
                        )
                    )
                },
                onSkip = { permanent ->
                    onTaskCompleted(
                        TaskCompletionResult(
                            taskId = task.id,
                            outcome = if (permanent) TaskOutcome.SKIPPED_PERMANENT else TaskOutcome.SKIPPED_RESCHEDULE,
                            response = null,
                            followUpResult = null,
                            timeSpentMs = 0,
                            signals = emptyList(),
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun TaskLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "...",
            typography = AppTheme.typography.Display.D1000,
            color = AppTheme.colors.textSecondary,
        )
    }
}

/**
 * Fallback screen for task types that don't have implementations yet.
 * Shows the task instruction and allows completion/skipping.
 * 
 * This is intentionally ugly/obvious so you know a real screen needs to be built.
 */
@Composable
private fun PlaceholderTaskScreen(
    task: Task,
    onComplete: () -> Unit,
    onSkip: (permanent: Boolean) -> Unit,
) {
    Screen { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimension.D600),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            
            // Big ugly warning banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFFF6B6B),
                        shape = RoundedCornerShape(Dimension.D400)
                    )
                    .padding(Dimension.D500),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️ DEV PLACEHOLDER ⚠️",
                        typography = AppTheme.typography.Label.L700,
                    )
                    Text(
                        text = "Build a screen for ${task.type.name}",
                        typography = AppTheme.typography.Body.B500,
                    )
                }
            }
            
            VerticalSpacerD800()
            
            // Task type badge
            Text(
                text = task.type.name.replace("_", " "),
                typography = AppTheme.typography.Label.L600,
                color = AppTheme.colors.textSecondary,
            )
            
            VerticalSpacerD500()
            
            // Main instruction
            Text(
                text = task.instruction,
                typography = AppTheme.typography.Heading.H600,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.text,
            )
            
            VerticalSpacerD500()
            
            // Task ID for debugging
            Text(
                text = "(${task.id})",
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
            )
            
            Spacer(Modifier.weight(1f))
            
            // Complete button
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Skip (Placeholder)")
            }
            
            VerticalSpacerD800()
        }
    }
}

/**
 * Factory interface for creating task ViewModels.
 * Implementations should be provided via DI.
 */
interface TaskViewModelFactory {
    fun createPromptViewModel(task: Task): PromptViewModel
    fun createInstructionViewModel(task: Task): InstructionViewModel
}

// ============ Previews ============

/**
 * Sample tasks for previews
 */
private object PreviewTasks {
    val promptTask = Task(
        id = "preview-prompt",
        type = TaskType.PROMPT,
        categories = listOf(TaskCategory.REFLECTION),
        difficulty = Difficulty.LIGHT,
        instruction = "What's something you used to believe that you don't anymore?",
        requiresSocial = false,
        bestForMoods = null,
        avoidForMoods = null,
        safeToReflect = true,
        responseStyle = ResponseStyle(allowsText = true, allowsPhoto = true),
        conditions = null,
        assets = null,
        followUp = null,
        placeholder = "I used to think...",
        durationSeconds = null,
        selectionOptions = null,
        minSelections = null,
        maxSelections = null,
        routingOptions = null,
        requireFrontCamera = null,
    )
    
    val instructionTask = Task(
        id = "preview-instruction",
        type = TaskType.INSTRUCTION,
        categories = listOf(TaskCategory.SOCIAL),
        difficulty = Difficulty.MEDIUM,
        instruction = "Give a genuine compliment to someone you don't know. It doesn't have to be big. It just has to be true.",
        requiresSocial = true,
        bestForMoods = null,
        avoidForMoods = null,
        safeToReflect = false,
        responseStyle = ResponseStyle(),
        conditions = null,
        assets = null,
        followUp = null,
        placeholder = null,
        durationSeconds = null,
        selectionOptions = null,
        minSelections = null,
        maxSelections = null,
        routingOptions = null,
        requireFrontCamera = null,
    )
    
    val drawingTask = Task(
        id = "preview-drawing",
        type = TaskType.DRAWING,
        categories = listOf(TaskCategory.PLAY),
        difficulty = Difficulty.LIGHT,
        instruction = "Draw what you see when you close your eyes right now.",
        requiresSocial = false,
        bestForMoods = null,
        avoidForMoods = null,
        safeToReflect = true,
        responseStyle = ResponseStyle(allowsDrawing = true),
        conditions = null,
        assets = null,
        followUp = null,
        placeholder = null,
        durationSeconds = null,
        selectionOptions = null,
        minSelections = null,
        maxSelections = null,
        routingOptions = null,
        requireFrontCamera = null,
    )
    
    val stillnessTask = Task(
        id = "preview-stillness",
        type = TaskType.STILLNESS,
        categories = listOf(TaskCategory.STILLNESS),
        difficulty = Difficulty.HEAVY,
        instruction = "Hold your phone perfectly still for 30 seconds. No cheating.",
        requiresSocial = false,
        bestForMoods = null,
        avoidForMoods = null,
        safeToReflect = true,
        responseStyle = ResponseStyle(),
        conditions = null,
        assets = null,
        followUp = null,
        placeholder = null,
        durationSeconds = 30,
        selectionOptions = null,
        minSelections = null,
        maxSelections = null,
        routingOptions = null,
        requireFrontCamera = null,
    )
}

@Preview
@Composable
private fun TaskLoadingStatePreview() {
    PreviewContent {
        TaskLoadingState()
    }
}

@Preview
@Composable
private fun PlaceholderTaskScreen_PromptPreview() {
    PreviewContent {
        PlaceholderTaskScreen(
            task = PreviewTasks.promptTask,
            onComplete = {},
            onSkip = {},
        )
    }
}

@Preview
@Composable
private fun PlaceholderTaskScreen_InstructionPreview() {
    PreviewContent {
        PlaceholderTaskScreen(
            task = PreviewTasks.instructionTask,
            onComplete = {},
            onSkip = {},
        )
    }
}

@Preview
@Composable
private fun PlaceholderTaskScreen_DrawingPreview() {
    PreviewContent {
        PlaceholderTaskScreen(
            task = PreviewTasks.drawingTask,
            onComplete = {},
            onSkip = {},
        )
    }
}

@Preview
@Composable
private fun PlaceholderTaskScreen_StillnessPreview() {
    PreviewContent {
        PlaceholderTaskScreen(
            task = PreviewTasks.stillnessTask,
            onComplete = {},
            onSkip = {},
        )
    }
}