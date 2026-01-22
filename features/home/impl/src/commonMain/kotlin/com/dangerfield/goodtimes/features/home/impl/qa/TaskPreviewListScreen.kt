package com.dangerfield.goodtimes.features.home.impl.qa

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.goodtimes.libraries.goodtimes.Difficulty
import com.dangerfield.goodtimes.libraries.goodtimes.ResponseStyle
import com.dangerfield.goodtimes.libraries.goodtimes.TaskCategory
import com.dangerfield.goodtimes.libraries.goodtimes.TaskType
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.ListSection
import com.dangerfield.libraries.ui.components.ListSectionItem
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.header.TopBar
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.fadingEdge
import com.dangerfield.libraries.ui.screenHorizontalInsets
import com.dangerfield.libraries.ui.scrollbar
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class TaskPreviewListState {
    data object Loading : TaskPreviewListState()
    data class Loaded(
        val totalCount: Int,
        val tasksByType: Map<TaskType, List<Task>>,
    ) : TaskPreviewListState()
}

@Composable
fun TaskPreviewListScreen(
    state: TaskPreviewListState,
    onBackClicked: () -> Unit,
    onTaskClicked: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    Screen(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(
                title = "Task Preview",
                onNavigateBack = onBackClicked
            )
        }
    ) { paddingValues ->
        when (state) {
            is TaskPreviewListState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading tasks...",
                        typography = AppTheme.typography.Body.B600,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            is TaskPreviewListState.Loaded -> {
                TaskList(
                    totalCount = state.totalCount,
                    tasksByType = state.tasksByType,
                    onTaskClicked = onTaskClicked,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun TaskList(
    totalCount: Int,
    tasksByType: Map<TaskType, List<Task>>,
    onTaskClicked: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .fadingEdge(listState)
            .scrollbar(listState, direction = Orientation.Vertical, indicatorThickness = 6.dp)
            .padding(screenHorizontalInsets)
    ) {
        item {
            VerticalSpacerD500()
            Text(
                text = "$totalCount tasks total",
                typography = AppTheme.typography.Body.B600,
                color = AppTheme.colors.textSecondary,
            )
            VerticalSpacerD500()
        }

        tasksByType.forEach { (type, tasksOfType) ->
            item(key = type.name) {
                VerticalSpacerD500()
                ListSection(
                    title = "${type.name} (${tasksOfType.size})",
                    items = tasksOfType.map { task ->
                        ListSectionItem(
                            headlineText = task.instruction.take(50) + if (task.instruction.length > 50) "..." else "",
                            supportingText = task.id,
                            onClick = { onTaskClicked(task) }
                        )
                    }
                )
            }
        }

        item {
            VerticalSpacerD500()
        }
    }
}

@Preview
@Composable
private fun TaskPreviewListScreenLoadingPreview() {
    PreviewContent {
        TaskPreviewListScreen(
            state = TaskPreviewListState.Loading,
            onBackClicked = {},
            onTaskClicked = {}
        )
    }
}

@Preview
@Composable
private fun TaskPreviewListScreenLoadedPreview() {
    PreviewContent {
        TaskPreviewListScreen(
            state = TaskPreviewListState.Loaded(
                totalCount = PreviewTasks.sampleTasks.size,
                tasksByType = PreviewTasks.sampleTasks.groupBy { it.type },
            ),
            onBackClicked = {},
            onTaskClicked = {}
        )
    }
}

private object PreviewTasks {
    private fun task(
        id: String,
        type: TaskType,
        instruction: String,
        category: TaskCategory = TaskCategory.REFLECTION,
    ) = Task(
        id = id,
        type = type,
        categories = listOf(category),
        difficulty = Difficulty.LIGHT,
        instruction = instruction,
        requiresSocial = false,
        bestForMoods = null,
        avoidForMoods = null,
        safeToReflect = true,
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

    val sampleTasks = listOf(
        task("prompt-1", TaskType.PROMPT, "What's something you used to believe?", TaskCategory.REFLECTION),
        task("prompt-2", TaskType.PROMPT, "Describe the last time you laughed really hard", TaskCategory.REFLECTION),
        task("instruction-1", TaskType.INSTRUCTION, "Give a genuine compliment to someone", TaskCategory.SOCIAL),
        task("instruction-2", TaskType.INSTRUCTION, "Take a photo of something blue", TaskCategory.PLAY),
        task("drawing-1", TaskType.DRAWING, "Draw what you see when you close your eyes", TaskCategory.PLAY),
        task("stillness-1", TaskType.STILLNESS, "Hold your phone perfectly still for 30 seconds", TaskCategory.STILLNESS),
    )
}
