package com.dangerfield.goodtimes.features.onboarding.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.goodtimes.system.typography.TypographyResource
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.text.Text
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val ANIMATION_DURATION_MS = 500
private const val MS_PER_CHARACTER = 30L // ~33 chars per second reading speed
private const val MIN_DELAY_MS = 600L // minimum delay between items
private const val MAX_DELAY_MS = 2500L // cap for long text

private fun calculateDelayForText(text: String): Long {
    val calculated = text.length * MS_PER_CHARACTER
    return calculated.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
}

@Composable
private fun StaggeredText(
    text: String,
    index: Int,
    visibleCount: Int,
    typography: TypographyResource,
    modifier: Modifier = Modifier
) {
    val isVisible = index < visibleCount
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(ANIMATION_DURATION_MS)
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(ANIMATION_DURATION_MS)
    )

    Text(
        text = text,
        typography = typography,
        modifier = modifier
            .alpha(alpha)
            .offset(y = offsetY.dp)
    )
}

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
    animationsEnabled: Boolean = true,
    onAnimationComplete: () -> Unit = {}
) {
    val texts = OnboardingCopy.introTexts
    var visibleCount by remember { mutableIntStateOf(if (animationsEnabled) 0 else texts.size) }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            onAnimationComplete()
            return@LaunchedEffect
        }
        // First item shows immediately
        visibleCount++
        // Subsequent items delay based on previous text length
        for (i in 0 until texts.size - 1) {
            delay(calculateDelayForText(texts[i]))
            visibleCount++
        }
        onAnimationComplete()
    }

    OnboardingPageContent {
        StaggeredText(
            text = texts[0],
            index = 0,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Display.D1100
        )
        VerticalSpacerD800()
        StaggeredText(
            text = texts[1],
            index = 1,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[2],
            index = 2,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[3],
            index = 3,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun WhatIKnowPage(
    animationsEnabled: Boolean = true,
    onAnimationComplete: () -> Unit = {}
) {
    val texts = OnboardingCopy.whatIKnowTexts
    var visibleCount by remember { mutableIntStateOf(if (animationsEnabled) 0 else texts.size) }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            onAnimationComplete()
            return@LaunchedEffect
        }
        visibleCount++
        for (i in 0 until texts.size - 1) {
            delay(calculateDelayForText(texts[i]))
            visibleCount++
        }
        onAnimationComplete()
    }

    OnboardingPageContent {
        StaggeredText(
            text = texts[0],
            index = 0,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Display.D1100
        )
        VerticalSpacerD800()
        StaggeredText(
            text = texts[1],
            index = 1,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[2],
            index = 2,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun UnderstandingYouPage(
    animationsEnabled: Boolean = true,
    onAnimationComplete: () -> Unit = {}
) {
    val texts = OnboardingCopy.understandingYouTexts
    var visibleCount by remember { mutableIntStateOf(if (animationsEnabled) 0 else texts.size) }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            onAnimationComplete()
            return@LaunchedEffect
        }
        visibleCount++
        for (i in 0 until texts.size - 1) {
            delay(calculateDelayForText(texts[i]))
            visibleCount++
        }
        onAnimationComplete()
    }

    OnboardingPageContent {
        StaggeredText(
            text = texts[0],
            index = 0,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Display.D1100
        )
        VerticalSpacerD800()
        StaggeredText(
            text = texts[1],
            index = 1,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun PagesPage(
    animationsEnabled: Boolean = true,
    onAnimationComplete: () -> Unit = {}
) {
    val texts = OnboardingCopy.pagesTexts
    var visibleCount by remember { mutableIntStateOf(if (animationsEnabled) 0 else texts.size) }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            onAnimationComplete()
            return@LaunchedEffect
        }
        visibleCount++
        for (i in 0 until texts.size - 1) {
            delay(calculateDelayForText(texts[i]))
            visibleCount++
        }
        onAnimationComplete()
    }

    OnboardingPageContent {
        StaggeredText(
            text = texts[0],
            index = 0,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Display.D1100
        )
        VerticalSpacerD800()
        StaggeredText(
            text = texts[1],
            index = 1,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[2],
            index = 2,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[3],
            index = 3,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[4],
            index = 4,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
    }
}

@Composable
internal fun PrivacyPage(
    animationsEnabled: Boolean = true,
    onAnimationComplete: () -> Unit = {}
) {
    val texts = OnboardingCopy.privacyTexts
    var visibleCount by remember { mutableIntStateOf(if (animationsEnabled) 0 else texts.size) }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            onAnimationComplete()
            return@LaunchedEffect
        }
        visibleCount++
        for (i in 0 until texts.size - 1) {
            delay(calculateDelayForText(texts[i]))
            visibleCount++
        }
        onAnimationComplete()
    }

    OnboardingPageContent {
        StaggeredText(
            text = texts[0],
            index = 0,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Display.D1100
        )
        VerticalSpacerD800()
        StaggeredText(
            text = texts[1],
            index = 1,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[2],
            index = 2,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
        VerticalSpacerD1000()
        StaggeredText(
            text = texts[3],
            index = 3,
            visibleCount = visibleCount,
            typography = AppTheme.typography.Body.B700
        )
    }
}

// Previews

@Preview
@Composable
private fun IntroPagePreview() {
    PreviewContent {
        IntroPage(animationsEnabled = false)
    }
}

@Preview
@Composable
private fun WhatIKnowPagePreview() {
    PreviewContent {
        WhatIKnowPage(animationsEnabled = false)
    }
}

@Preview
@Composable
private fun UnderstandingYouPagePreview() {
    PreviewContent {
        UnderstandingYouPage(animationsEnabled = false)
    }
}

@Preview
@Composable
private fun PagesPagePreview() {
    PreviewContent {
        PagesPage(animationsEnabled = false)
    }
}

@Preview
@Composable
private fun PrivacyPagePreview() {
    PreviewContent {
        PrivacyPage(animationsEnabled = false)
    }
}
