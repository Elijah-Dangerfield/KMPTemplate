package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.runtime.Composable
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.libraries.ui.components.dialog.DialogState
import com.dangerfield.libraries.ui.components.dialog.rememberDialogState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FreshStartDialog(
    state: FreshStartState,
    dialogState: DialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        state = dialogState,
        onDismissRequest = onDismiss,
        title = state.title,
        description = state.description,
        primaryButtonText = "Reset",
        secondaryButtonText = "Nevermind",
        onPrimaryButtonClicked = {
            onConfirm()
            dialogState.dismiss()
        },
        onSecondaryButtonClicked = {
            dialogState.dismiss()
        }
    )
}

@Preview
@Composable
fun FreshStartDialogPreview() {
    PreviewContent {
        FreshStartDialog(
            state = FreshStartState(),
            dialogState = rememberDialogState(),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
