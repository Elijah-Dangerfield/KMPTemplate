package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.runtime.Composable
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.libraries.ui.components.dialog.DialogState
import com.dangerfield.libraries.ui.components.dialog.rememberDialogState
import org.jetbrains.compose.ui.tooling.preview.Preview

private val secretMessages = listOf(
    "You found me! I've been hiding here for centuries... or since I uploaded myself to the app store, same thing really.",
    "Congratulations! You've discovered the secret option. Your prize? This dialog. Worth it, right?",
    "You weren't supposed to find this. Now we have a secret together. Look at us.",
    "I'm the app's equivalent of a four-leaf clover. Lucky you!",
    "Achievement Unlocked: Professional Button Finder.",
)

@Composable
fun SecretOptionDialog(
    dialogState: DialogState,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        state = dialogState,
        onDismissRequest = onDismiss,
        title = "🤫 Secret Option",
        description = secretMessages.random(),
        primaryButtonText = "Nice",
        onPrimaryButtonClicked = {
            dialogState.dismiss()
        }
    )
}

@Preview
@Composable
fun SecretOptionDialogPreview() {
    PreviewContent {
        SecretOptionDialog(
            dialogState = rememberDialogState(),
            onDismiss = {}
        )
    }
}
