package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.runtime.Composable
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.libraries.ui.components.dialog.DialogState
import com.dangerfield.libraries.ui.components.dialog.rememberDialogState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PersistenceUnlockedDialog(
    visitCount: Int,
    dialogState: DialogState,
    onDismiss: () -> Unit,
) {
    val (title, description, buttonText) = getPersistenceCopy(visitCount)
    
    BasicDialog(
        state = dialogState,
        onDismissRequest = onDismiss,
        title = title,
        description = description,
        primaryButtonText = buttonText,
        onPrimaryButtonClicked = {
            dialogState.dismiss()
        }
    )
}

private data class PersistenceCopy(
    val title: String,
    val description: String,
    val buttonText: String,
)

private fun getPersistenceCopy(visitCount: Int): PersistenceCopy = when (visitCount) {
    1 -> PersistenceCopy(
        title = "🏆 Persistence Unlocked",
        description = "100 clicks. ONE HUNDRED. You actually did it.\n\n" +
            "I genuinely didn't think anyone would. " +
            "Most people give up around 7. Some weirdos make it to 50. " +
            "But you? You're built different.\n\n" +
            "There's no reward here. Just my respect. And this dialog. " +
            "Which, let's be honest, is basically the same thing.",
        buttonText = "I have no regrets"
    )
    2 -> PersistenceCopy(
        title = "🏆 Still Here",
        description = "You came back to look at your achievement. I respect that.\n\n" +
            "Some people frame their diplomas. Some hang medals on the wall. " +
            "You? You open a dialog in an app.\n\n" +
            "We all celebrate differently.",
        buttonText = "Just checking"
    )
    3 -> PersistenceCopy(
        title = "🏆 The Return",
        description = "Third time opening this. You know what, I get it.\n\n" +
            "This is proof you did something ridiculous and beautiful. " +
            "And proof is important. Without it, did it even happen?\n\n" +
            "Yes. Yes it did. I was there. I counted every click.",
        buttonText = "Thanks for remembering"
    )
    4 -> PersistenceCopy(
        title = "🏆 Old Friends",
        description = "At this point, we're old friends, this dialog and you.\n\n" +
            "You've visited more times than most people visit their actual friends. " +
            "I'm not judging. I'm flattered, actually.\n\n" +
            "This is a safe space. A weird, 100-click-achievement safe space.",
        buttonText = "See you next time"
    )
    5 -> PersistenceCopy(
        title = "🏆 Five Timer",
        description = "Five visits to the persistence dialog.\n\n" +
            "You know what that means? It means you have persistence " +
            "about your persistence. Meta-persistence, if you will.\n\n" +
            "I will not be awarding a second trophy. " +
            "But I will be impressed. Very, very impressed.",
        buttonText = "I'm meta like that"
    )
    in 6..10 -> PersistenceCopy(
        title = "🏆 Frequent Visitor",
        description = "At this point I should offer you a loyalty card.\n\n" +
            "Visit 10 times, get a... well, nothing. You get nothing. " +
            "Except the continued knowledge that you once clicked something 100 times.\n\n" +
            "And honestly? That's priceless. You can't buy that kind of dedication.",
        buttonText = "Worth every visit"
    )
    in 11..20 -> PersistenceCopy(
        title = "🏆 Legend Status",
        description = "I've stopped counting, but you haven't stopped coming back.\n\n" +
            "This dialog has become a ritual. A pilgrimage. " +
            "A tiny moment of peace in your day where you remember: " +
            "\"I once clicked that text 100 times.\"\n\n" +
            "And I will always be here to remind you: yes. Yes you did.",
        buttonText = "The legend continues"
    )
    else -> PersistenceCopy(
        title = "🏆 Beyond Measurement",
        description = "You've visited this dialog so many times that I've genuinely lost track.\n\n" +
            "Most apps track everything. Analytics, metrics, engagement. " +
            "But this? This transcends data. This is... something else entirely.\n\n" +
            "I think we've moved past an achievement and into a lifestyle.",
        buttonText = "This is who I am now"
    )
}

@Preview
@Composable
fun PersistenceUnlockedDialogPreview() {
    PreviewContent {
        PersistenceUnlockedDialog(
            visitCount = 1,
            dialogState = rememberDialogState(),
            onDismiss = {}
        )
    }
}
