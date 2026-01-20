package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonSize
import com.dangerfield.libraries.ui.components.chip.SelectChip
import com.dangerfield.libraries.ui.components.dialog.bottomsheet.BottomSheet
import com.dangerfield.libraries.ui.components.dialog.bottomsheet.BottomSheetState
import com.dangerfield.libraries.ui.components.dialog.bottomsheet.rememberBottomSheetState
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.system.LocalClock
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodScreen(
    state: MoodState,
    sheetState: BottomSheetState,
    onAction: (MoodAction) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = LocalClock.current
    val promptText = remember(state.dismissCount, state.sessionNumber, state.fromSettings, state.isFirstEverMoodPrompt) {
        if (state.fromSettings) {
            "How are you feeling right now?"
        } else {
            getMoodPromptText(
                dismissCount = state.dismissCount,
                sessionNumber = state.sessionNumber,
                isFirstEverMoodPrompt = state.isFirstEverMoodPrompt,
                clock = clock
            )
        }
    }

    val hasAnswered = state.selectedMood != null

    BottomSheet(
        onDismissRequest = { if (!hasAnswered) onDismissRequest() },
        state = sheetState,
        showDragHandle = true,
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = hasAnswered,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentAlignment = Alignment.Center,
        ) { showingResponse ->
            if (showingResponse && state.responseText != null) {
                // Response after mood selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimension.D800)
                        .padding(bottom = Dimension.D1000),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.responseText,
                        typography = AppTheme.typography.Heading.H700,
                        textAlign = TextAlign.Center,
                    )

                    VerticalSpacerD1000()

                    Button(
                        onClick = { onAction(MoodAction.Close) },
                        size = ButtonSize.Small,
                    ) {
                        Text(text = "Got it")
                    }
                }
            } else {
                // Mood selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimension.D800)
                        .padding(bottom = Dimension.D1000),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = promptText,
                        typography = AppTheme.typography.Heading.H700,
                        textAlign = TextAlign.Center,
                    )

                    VerticalSpacerD800()

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
                        verticalArrangement = Arrangement.spacedBy(Dimension.D400),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Mood.entries.forEach { mood ->
                            SelectChip(
                                label = mood.displayName(),
                                selected = false,
                                onClick = { onAction(MoodAction.SelectMood(mood)) }
                            )
                        }
                    }

                    if (!state.fromSettings) {
                        VerticalSpacerD1000()

                        Text(
                            text = "Don't ask me this",
                            typography = AppTheme.typography.Label.L500,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onAction(MoodAction.DontAskAgain) }
                        )
                    }
                }
            }
        }
    }
}

private fun Mood.displayName(): String = when (this) {
    Mood.GREAT -> "Great 😄"
    Mood.GOOD -> "Good 🙂"
    Mood.OKAY -> "Okay 😐"
    Mood.LOW -> "Low 😔"
    Mood.BAD -> "Bad 😢"
    Mood.COMPLICATED -> "Complicated 🤔"
}

@Preview
@Composable
fun MoodScreenPreview() {
    PreviewContent {
        MoodScreen(
            state = MoodState(dismissCount = 0, sessionNumber = 1),
            sheetState = rememberBottomSheetState(),
            onAction = {},
            onDismissRequest = {}
        )
    }
}
