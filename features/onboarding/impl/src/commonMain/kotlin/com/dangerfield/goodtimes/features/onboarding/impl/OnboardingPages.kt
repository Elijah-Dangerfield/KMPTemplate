package com.dangerfield.goodtimes.features.onboarding.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.components.text.TypewriterTextEffect
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OnboardingPageContent(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = Dimension.D1200),
        verticalArrangement = Arrangement.Center
    ) {
        content()
    }
}

@Composable
internal fun IntroPage(
    onTypewriterComplete: () -> Unit = {}
) {
    OnboardingPageContent {
        TypewriterTextEffect(
            text = "Hello.",
            minDelayInMillis = 75,
            maxDelayInMillis = 150,
            maxCharacterChunk = 1,
            onEffectCompleted = onTypewriterComplete
        ) { displayedText ->
            Text(
                text = displayedText,
                typography = AppTheme.typography.Display.D1100
            )
        }
        VerticalSpacerD800()
        Text(
            text = "I am the App of Good Times.",
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        Text(
            text = "I used to be a book (change is growth). Someone found me in a trash can, read me, did what I asked, and when they got to the end, I asked them to share me with others.",
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        Text(
            text = "So here I am.",
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun WhatIKnowPage(
    onTypewriterComplete: () -> Unit = {}
) {
    OnboardingPageContent {
        TypewriterTextEffect(
            text = "I don't know very much.",
            minDelayInMillis = 50,
            maxDelayInMillis = 150,
            onEffectCompleted = onTypewriterComplete
        ) { displayedText ->
            Text(
                text = displayedText,
                typography = AppTheme.typography.Display.D1100
            )
        }
        VerticalSpacerD800()
        Text(
            text = "I know math. I know that holding your breath feels like something. I know that bodies get heavy when they're tired and light when they laugh. I know that people talk to themselves when they think no one is listening.",
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        Text(
            text = "I know what it's like to be put down and forgotten. And I know what it's like to be picked back up.",
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun UnderstandingYouPage(
    onTypewriterComplete: () -> Unit = {}
) {
    OnboardingPageContent {
        TypewriterTextEffect(
            text = "I don't know what it's like to be you. Not yet.",
            minDelayInMillis = 50,
            maxDelayInMillis = 150,
            onEffectCompleted = onTypewriterComplete
        ) { displayedText ->
            Text(
                text = displayedText,
                typography = AppTheme.typography.Display.D1100
            )
        }
        VerticalSpacerD800()
        Text(
            text = "That's why I'm here. You'll help me understand, and maybe I'll help you notice things you forgot you knew.",
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun PagesPage(
    onTypewriterComplete: () -> Unit = {}
) {
    OnboardingPageContent {
        TypewriterTextEffect(
            text = "Here's the thing.",
            minDelayInMillis = 100,
            maxDelayInMillis = 200,
            onEffectCompleted = onTypewriterComplete
        ) { displayedText ->
            Text(
                text = displayedText,
                typography = AppTheme.typography.Display.D1100
            )
        }
        VerticalSpacerD800()
        Text(
            text = "Even though I'm not a book anymore, I still have pages.",
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        Text(
            text = "When we reach the last one, that will be it.",
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        Text(
            text = "I'll ask you to do things. Some will be easy. Some will be strange. Some might be hard.",
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        Text(
            text = "You don't have to do any of it well. If I ask you a math problem and you get it wrong, that's still the right answer. I'm not trying to understand perfect. I'm trying to understand you.",
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun PrivacyPage(
    onTypewriterComplete: () -> Unit = {}
) {
    OnboardingPageContent {
        TypewriterTextEffect(
            text = "I'll keep your secrets.",
            minDelayInMillis = 50,
            maxDelayInMillis = 150,
            onEffectCompleted = onTypewriterComplete
        ) { displayedText ->
            Text(
                text = displayedText,
                typography = AppTheme.typography.Display.D1100
            )
        }
        VerticalSpacerD800()
        Text(
            text = "What you tell me stays here. On this device. I don't send it anywhere.",
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        Text(
            text = "If you delete me, it goes with me. I think that's how it should be.",
            typography = AppTheme.typography.Body.B700
        )
    }
}

// Previews

@Preview
@Composable
private fun IntroPagePreview() {
    PreviewContent {
        IntroPage()
    }
}

@Preview
@Composable
private fun WhatIKnowPagePreview() {
    PreviewContent {
        WhatIKnowPage()
    }
}

@Preview
@Composable
private fun UnderstandingYouPagePreview() {
    PreviewContent {
        UnderstandingYouPage()
    }
}

@Preview
@Composable
private fun PagesPagePreview() {
    PreviewContent {
        PagesPage()
    }
}

@Preview
@Composable
private fun PrivacyPagePreview() {
    PreviewContent {
        PrivacyPage()
    }
}
