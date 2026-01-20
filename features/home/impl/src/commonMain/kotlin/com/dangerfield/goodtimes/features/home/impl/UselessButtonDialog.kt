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
fun UselessButtonDialog(
    clickCount: Int,
    dialogState: DialogState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, message) = getUselessButtonDialogCopy(clickCount)
    
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
                text = title,
                typography = AppTheme.typography.Heading.H700,
                textAlign = TextAlign.Center,
            )
            
            VerticalSpacerD500()
            
            Text(
                text = message,
                typography = AppTheme.typography.Body.B600,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.textSecondary,
            )
            
            VerticalSpacerD800()
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = getUselessButtonDismissText(clickCount))
            }
        }
    }
}

private fun getUselessButtonDialogCopy(clickCount: Int): Pair<String, String> {
    return when (clickCount) {
        1 -> "Got ya." to "That button doesn't do anything. Interesting that you clicked it though. I'll make note of that."
        
        2 -> "You again." to "Still doesn't do anything. But I appreciate the persistence. Noted."
        
        3 -> "Three times now." to "At this point I'm genuinely curious what you're expecting to happen. This tells me something about you."
        
        4 -> "Okay." to "Four clicks on a button that does nothing. You're either very thorough or very stubborn. Both are interesting."
        
        5 -> "Halfway there." to "To what? I don't know. But you're committed. I respect that. Adding this to your profile."
        
        6 -> "Six." to "Just... six clicks on a useless button. You know what? This is more revealing than any personality test."
        
        7 -> "Lucky number seven?" to "It's not. Nothing happened. But your dedication is genuinely impressive at this point."
        
        8 -> "Eight clicks." to "Most people gave up by now. You're not most people. That's going in the notes."
        
        9 -> "Nine." to "Just one more after this. Not that anything will happen. But you've come this far."
        
        10 -> "🏆 You Did It" to "Ten clicks on a useless button. Most people never even tried. You not only tried. You finished.\n\nI'm genuinely learning a lot about humans because of you."
        
        // After the button moves to settings (clickCount > 10)
        else -> "🏆 The Return" to "You found it in settings and clicked it again. The button is truly useless now, but your dedication is not. Say your goodbyes."
    }
}

private fun getUselessButtonDismissText(clickCount: Int): String {
    return when (clickCount) {
        1 -> "Okay then"
        2 -> "Sure, why not"
        3 -> "I see"
        4 -> "Fair enough"
        5 -> "Halfway to what?"
        6 -> "Noted"
        7 -> "Interesting"
        8 -> "You're committed"
        9 -> "One more..."
        10 -> "Goodbye"
        else -> "Worth it"
    }
}

// TODO: Use click count to update user personality profile
// Ideas:
// - clickCount >= 3: User is curious/thorough
// - clickCount >= 5: User is persistent  
// - clickCount >= 8: User is determined/stubborn
// - clickCount >= 10: User completed the useless button journey
// - clickCount > 10: User actively sought out the button - very curious personality

@Preview
@Composable
fun UselessButtonDialogPreview() {
    PreviewContent {
        UselessButtonDialog(
            clickCount = 1,
            dialogState = rememberDialogState(),
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun UselessButtonDialogPreviewFinal() {
    PreviewContent {
        UselessButtonDialog(
            clickCount = 10,
            dialogState = rememberDialogState(),
            onDismiss = {}
        )
    }
}
