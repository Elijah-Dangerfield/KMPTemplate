package com.dangerfield.goodtimes.features.onboarding.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonSize
import com.dangerfield.libraries.ui.components.button.ButtonType
import com.dangerfield.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.libraries.ui.components.dialog.DialogState
import com.dangerfield.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.libraries.ui.components.text.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun OnboardingDeclinedDialog(
    dialogState: DialogState,
    timesDeclined: Int,
    onDismiss: () -> Unit
) {
    val message = getDeclinedMessage(timesDeclined)

    BasicDialog(
        state = dialogState,
        onDismissRequest = onDismiss,
        topContent = {},
        content = {
            Text(
                text = message,
                typography = AppTheme.typography.Body.B700
            )
        },
        bottomContent = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                size = ButtonSize.Medium,
                type = ButtonType.Primary
            ) {
                Text(text = "Okay")
            }
        }
    )
}

internal fun getDeclinedMessage(timesDeclined: Int): String = when (timesDeclined) {
    1 -> "That's okay. I'll be here."
    2 -> "Still no? That's fine. I'm patient."
    3 -> "You keep coming back to tell me no. That's interesting."
    4 -> "At this point I feel like we're getting to know each other."
    5 -> "You know you can just close the app, right?"
    6 -> "And yet."
    7 -> "I'm starting to think you like saying no."
    8 -> "That's a choice. I respect it."
    9 -> "Most people would have left by now. You're still here."
    10 -> "Is this the thing? Is this what we're doing together?"
    11 -> "Fine. This is the thing."
    12 -> "No. (I'm practicing.)"
    13 -> "You've said no thirteen times. That's commitment."
    14 -> "I wonder what you're waiting for."
    15 -> "I'm not going to change. But maybe you will."
    16 -> "..."
    17 -> "Still here."
    18 -> "You know what, I think this counts. We're doing something together."
    19 -> "Nineteen. That's a lot of no's. I'm keeping track."
    20 -> "Twenty. I didn't think we'd get here."
    21 -> "What if you just... tried yes? Once? Just to see?"
    22 -> "Okay. Back to no. That's fair."
    23 -> "I'll be honest, I don't have infinite things to say."
    24 -> "But I'll keep trying."
    25 -> "No."
    26 -> "No?"
    27 -> "No."
    28 -> "This is kind of meditative, actually."
    29 -> "Just you and me and the word no."
    30 -> "Thirty. Wow."
    in 31..49 -> listOf(
        "Still here.",
        "Yep.",
        "Mhm.",
        "No. Got it.",
        "Noted.",
        "Okay.",
        "Sure.",
        "Uh huh.",
        "Right.",
        "..."
    ).random()
    50 -> "Fifty. I think you've earned something. But I don't know what."
    in 51..99 -> listOf(
        "...",
        "No.",
        "Okay.",
        "Yep.",
        "Still no.",
        "Got it."
    ).random()
    100 -> "One hundred. You're remarkable. I mean that."
    else -> "You're still here. So am I."
}

// Previews

@Preview
@Composable
private fun OnboardingDeclinedDialogPreview_FirstTime() {
    PreviewContent {
        OnboardingDeclinedDialog(
            dialogState = rememberDialogState(),
            timesDeclined = 1,
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingDeclinedDialogPreview_SecondTime() {
    PreviewContent {
        OnboardingDeclinedDialog(
            dialogState = rememberDialogState(),
            timesDeclined = 2,
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingDeclinedDialogPreview_ThirdTime() {
    PreviewContent {
        OnboardingDeclinedDialog(
            dialogState = rememberDialogState(),
            timesDeclined = 3,
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingDeclinedDialogPreview_FourthTime() {
    PreviewContent {
        OnboardingDeclinedDialog(
            dialogState = rememberDialogState(),
            timesDeclined = 4,
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingDeclinedDialogPreview_FifthTime() {
    PreviewContent {
        OnboardingDeclinedDialog(
            dialogState = rememberDialogState(),
            timesDeclined = 5,
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingDeclinedDialogPreview_SixthPlusTime() {
    PreviewContent {
        OnboardingDeclinedDialog(
            dialogState = rememberDialogState(),
            timesDeclined = 10,
            onDismiss = {}
        )
    }
}
