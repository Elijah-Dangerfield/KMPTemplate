package com.dangerfield.goodtimes.features.tasks.impl.templates.instruction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpResult
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpType
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonStyle
import com.dangerfield.libraries.ui.components.text.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun InstructionScreen(
    state: InstructionState,
    onAction: (InstructionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Screen(modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimension.D600),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            
            // Main instruction
            Text(
                text = state.instruction,
                typography = AppTheme.typography.Heading.H600,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.text,
            )
            
            Spacer(Modifier.weight(1f))
            
            // Mark done button
            Button(
                onClick = { onAction(InstructionAction.MarkDone) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
            
            VerticalSpacerD800()
        }
    }
    
    // Follow-up dialogs
    if (state.showFollowUp && state.currentFollowUpType != null) {
        when (state.currentFollowUpType) {
            FollowUpType.DID_YOU -> DidYouFollowUpDialog(
                config = state.followUpConfig,
                onSubmit = { result -> onAction(InstructionAction.SubmitFollowUp(result)) },
                onDismiss = { onAction(InstructionAction.DismissFollowUp) },
            )
            FollowUpType.FEELING -> FeelingFollowUpDialog(
                onSubmit = { result -> onAction(InstructionAction.SubmitFollowUp(result)) },
                onDismiss = { onAction(InstructionAction.DismissFollowUp) },
            )
            FollowUpType.REFLECTION -> ReflectionFollowUpDialog(
                onSubmit = { result -> onAction(InstructionAction.SubmitFollowUp(result)) },
                onDismiss = { onAction(InstructionAction.DismissFollowUp) },
            )
            FollowUpType.CUSTOM -> CustomFollowUpDialog(
                config = state.followUpConfig,
                onSubmit = { result -> onAction(InstructionAction.SubmitFollowUp(result)) },
                onDismiss = { onAction(InstructionAction.DismissFollowUp) },
            )
        }
    }
}

@Composable
private fun DidYouFollowUpDialog(
    config: com.dangerfield.goodtimes.libraries.goodtimes.FollowUpConfig?,
    onSubmit: (FollowUpResult) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Did you do it?",
                typography = AppTheme.typography.Heading.H700,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimension.D300),
            ) {
                // "No" options from config  
                config?.options?.forEach { option ->
                    Button(
                        onClick = { 
                            onSubmit(FollowUpResult(
                                didComplete = false,
                                selectedOptionId = option.id,
                            )) 
                        },
                        style = ButtonStyle.Text,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option.text)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(FollowUpResult(didComplete = true)) },
                style = ButtonStyle.Filled,
            ) {
                Text("Yes")
            }
        },
        containerColor = AppTheme.colors.surfacePrimary.color,
    )
}

@Composable
private fun FeelingFollowUpDialog(
    onSubmit: (FollowUpResult) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "How did that feel?",
                typography = AppTheme.typography.Heading.H700,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimension.D400),
            ) {
                // Simple 1-5 scale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    (1..5).forEach { score ->
                        Button(
                            onClick = { onSubmit(FollowUpResult(feelingScore = score)) },
                            style = ButtonStyle.Outlined,
                        ) {
                            Text(score.toString())
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Hard",
                        typography = AppTheme.typography.Body.B500,
                        color = AppTheme.colors.textSecondary,
                    )
                    Text(
                        text = "Easy",
                        typography = AppTheme.typography.Body.B500,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
        },
        confirmButton = {},  // Selection is the confirmation
        containerColor = AppTheme.colors.surfacePrimary.color,
    )
}

@Composable
private fun ReflectionFollowUpDialog(
    onSubmit: (FollowUpResult) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Any thoughts?",
                typography = AppTheme.typography.Heading.H700,
            )
        },
        text = {
            // TODO: Add text input for reflection
            Text(
                text = "What came up for you?",
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(FollowUpResult()) },
                style = ButtonStyle.Filled,
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            Button(
                onClick = { onSubmit(FollowUpResult()) },
                style = ButtonStyle.Text,
            ) {
                Text("Skip")
            }
        },
        containerColor = AppTheme.colors.surfacePrimary.color,
    )
}

@Composable
private fun CustomFollowUpDialog(
    config: com.dangerfield.goodtimes.libraries.goodtimes.FollowUpConfig?,
    onSubmit: (FollowUpResult) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimension.D400),
            ) {
                config?.options?.forEach { option ->
                    Button(
                        onClick = { onSubmit(FollowUpResult(selectedOptionId = option.id)) },
                        style = ButtonStyle.Outlined,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option.text)
                    }
                }
            }
        },
        confirmButton = {},  // Options are the confirmation
        containerColor = AppTheme.colors.surfacePrimary.color,
    )
}

// ============ Previews ============

@Preview
@Composable
private fun InstructionScreenPreview() {
    PreviewContent {
        InstructionScreen(
            state = InstructionState(
                instruction = "Give a genuine compliment to someone you don't know. It doesn't have to be big. It just has to be true.",
                hasFollowUp = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun InstructionScreenWithDidYouDialogPreview() {
    PreviewContent {
        InstructionScreen(
            state = InstructionState(
                instruction = "Give a genuine compliment to someone you don't know.",
                hasFollowUp = true,
                showFollowUp = true,
                currentFollowUpType = FollowUpType.DID_YOU,
                followUpConfig = com.dangerfield.goodtimes.libraries.goodtimes.FollowUpConfig(
                    type = FollowUpType.DID_YOU,
                    options = listOf(
                        com.dangerfield.goodtimes.libraries.goodtimes.FollowUpOption("not_ready", "Not today", reschedule = true),
                        com.dangerfield.goodtimes.libraries.goodtimes.FollowUpOption("not_for_me", "Not my thing", skipPermanent = true),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun InstructionScreenWithFeelingDialogPreview() {
    PreviewContent {
        InstructionScreen(
            state = InstructionState(
                instruction = "Sit in silence for 2 minutes.",
                hasFollowUp = true,
                showFollowUp = true,
                currentFollowUpType = FollowUpType.FEELING,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun InstructionScreenWithReflectionDialogPreview() {
    PreviewContent {
        InstructionScreen(
            state = InstructionState(
                instruction = "Take a walk without your phone.",
                hasFollowUp = true,
                showFollowUp = true,
                currentFollowUpType = FollowUpType.REFLECTION,
            ),
            onAction = {},
        )
    }
}
