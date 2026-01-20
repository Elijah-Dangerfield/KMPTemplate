package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.goodtimes.features.tasks.impl.TaskHost
import com.dangerfield.goodtimes.features.tasks.impl.TaskViewModelFactory
import com.dangerfield.goodtimes.libraries.goodtimes.Reaction
import com.dangerfield.goodtimes.libraries.goodtimes.ReactionStyle
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.HorizontalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.icon.IconButton
import com.dangerfield.libraries.ui.components.icon.Icons
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.screenHorizontalInsets
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeState = HomeState(),
    viewModelFactory: TaskViewModelFactory,
    onAction: (HomeAction) -> Unit = {},
    onSettingsClicked: () -> Unit = {},
) {
    Screen(
        modifier.padding(screenHorizontalInsets)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = Dimension.D500),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                if (state.showUselessButton) {
                    IconButton(
                        onClick = { onAction(HomeAction.ClickUselessButton) },
                        icon = Icons.Dot(null)
                    )
                    HorizontalSpacerD500()
                }
                IconButton(
                    onClick = onSettingsClicked,
                    icon = Icons.Settings(null)
                )
            }

            AnimatedContent(
                targetState = state.taskFlowState,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { flowState ->
                when (flowState) {
                    is TaskFlowState.Loading -> {
                        ThinkingContent(message = flowState.thinkingMessage)
                    }
                    is TaskFlowState.ShowingTask -> {
                        TaskHost(
                            task = flowState.task,
                            viewModelFactory = viewModelFactory,
                            onTaskCompleted = { result ->
                                onAction(HomeAction.TaskCompleted(result))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    is TaskFlowState.ShowingReaction -> {
                        ReactionContent(
                            reaction = flowState.reaction,
                            onDismiss = { onAction(HomeAction.ReactionDismissed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionContent(
    reaction: Reaction,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(Dimension.D800),
        ) {
            Text(
                text = reaction.message,
                typography = AppTheme.typography.Heading.H700,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.text,
            )
            
            VerticalSpacerD800()
            
            Text(
                text = "(tap to continue)",
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

/**
 * Displays the "thinking" message during task transitions.
 * The message is centered and subtle - it shouldn't feel like a loading screen,
 * more like a pause between thoughts.
 */
@Composable
private fun ThinkingContent(
    message: String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            typography = AppTheme.typography.Body.B600,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.textSecondary,
        )
    }
}

// ============ Previews ============

@Preview
@Composable
private fun HomeScreenPreview_Loading() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                taskFlowState = TaskFlowState.Loading("..."),
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview_Loading_WithMessage() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                taskFlowState = TaskFlowState.Loading("Let me think..."),
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview_Reaction_Quip() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                taskFlowState = TaskFlowState.ShowingReaction(
                    Reaction(
                        message = "A person of few words.",
                        style = ReactionStyle.QUIP,
                    )
                ),
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview_Reaction_Observation() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                taskFlowState = TaskFlowState.ShowingReaction(
                    Reaction(
                        message = "You think before you write. I like that.",
                        style = ReactionStyle.OBSERVATION,
                    )
                ),
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview_Reaction_Question() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                taskFlowState = TaskFlowState.ShowingReaction(
                    Reaction(
                        message = "Is that how you do everything?",
                        style = ReactionStyle.QUESTION,
                    )
                ),
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview_Reaction_Acknowledgment() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                taskFlowState = TaskFlowState.ShowingReaction(
                    Reaction(
                        message = "That one felt different, didn't it?",
                        style = ReactionStyle.ACKNOWLEDGMENT,
                    )
                ),
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview_WithUselessButton() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                showUselessButton = true,
                taskFlowState = TaskFlowState.Loading("..."),
            )
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview_WithoutUselessButton() {
    PreviewContent {
        HomeScreenContentPreview(
            state = HomeState(
                showUselessButton = false,
                taskFlowState = TaskFlowState.Loading("..."),
            )
        )
    }
}

@Preview
@Composable
private fun ThinkingContentPreview() {
    PreviewContent {
        ThinkingContent(message = "Let me think...")
    }
}

@Preview
@Composable
private fun ReactionContentPreview() {
    PreviewContent {
        ReactionContent(
            reaction = Reaction(
                message = "You took your time with that one.",
                style = ReactionStyle.OBSERVATION,
            ),
            onDismiss = {},
        )
    }
}

/**
 * Preview helper that renders HomeScreen content without requiring TaskViewModelFactory.
 * This shows the loading/reaction states directly.
 */
@Composable
private fun HomeScreenContentPreview(
    state: HomeState,
) {
    Screen(
        Modifier.padding(screenHorizontalInsets)
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = Dimension.D500),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                if (state.showUselessButton) {
                    IconButton(
                        onClick = { },
                        icon = Icons.Dot(null)
                    )
                    HorizontalSpacerD500()
                }
                IconButton(
                    onClick = { },
                    icon = Icons.Settings(null)
                )
            }

            AnimatedContent(
                targetState = state.taskFlowState,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { flowState ->
                when (flowState) {
                    is TaskFlowState.Loading -> {
                        ThinkingContent(message = flowState.thinkingMessage)
                    }
                    is TaskFlowState.ShowingTask -> {
                        // Can't show task in preview without viewModelFactory
                        ThinkingContent(message = "Task: ${flowState.task.instruction}")
                    }
                    is TaskFlowState.ShowingReaction -> {
                        ReactionContent(
                            reaction = flowState.reaction,
                            onDismiss = { },
                        )
                    }
                }
            }
        }
    }
}