package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.dialog.Dialog
import com.dangerfield.libraries.ui.components.dialog.DialogState
import com.dangerfield.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.libraries.ui.components.text.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FakeSkipDialog(
    dialogState: DialogState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        state = dialogState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimension.D800),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nice try.",
                typography = AppTheme.typography.Heading.H700,
                textAlign = TextAlign.Center,
            )
            
            VerticalSpacerD500()
            
            Text(
                text = "One of the cool parts about me not being a book anymore: you can't skip pages.\n\nInteresting to know that you would skip this task though... that tells me... something.",
                typography = AppTheme.typography.Body.B600,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.textSecondary,
            )
            
            VerticalSpacerD800()
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Fair enough")
            }
        }
    }
}

@Preview
@Composable
fun FakeSkipDialogPreview() {
    PreviewContent {
        FakeSkipDialog(
            dialogState = rememberDialogState(),
            onDismiss = {}
        )
    }
}
