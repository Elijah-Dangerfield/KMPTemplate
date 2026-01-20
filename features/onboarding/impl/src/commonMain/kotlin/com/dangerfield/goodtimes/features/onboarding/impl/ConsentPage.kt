package com.dangerfield.goodtimes.features.onboarding.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.checkbox.Checkbox
import com.dangerfield.libraries.ui.components.text.Text
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val ANIMATION_DURATION_MS = 500
private const val MIN_DELAY_MS = 600L
private const val MAX_DELAY_MS = 3500L
private const val MS_PER_CHARACTER = 30L

private fun calculateDelayForText(text: String): Long {
    val calculated = text.length * MS_PER_CHARACTER
    return calculated.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
}

@Composable
private fun StaggeredContent(
    index: Int,
    visibleCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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

    Box(
        modifier = modifier
            .alpha(alpha)
            .offset(y = offsetY.dp)
    ) {
        content()
    }
}

@Composable
internal fun ConsentPage(
    selection: OnboardingSelection?,
    animationsEnabled: Boolean = true,
    onYesSelected: () -> Unit,
    onNoSelected: () -> Unit,
    onAnimationComplete: () -> Unit = {}
) {
    val texts = listOf(
        OnboardingCopy.Consent.emoji,
        OnboardingCopy.Consent.question,
        "" // checkboxes
    )
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StaggeredContent(
                index = 0,
                visibleCount = visibleCount
            ) {
                Text(
                    text = texts[0],
                    typography = AppTheme.typography.Display.D1400
                )
            }
            VerticalSpacerD800()
            StaggeredContent(
                index = 1,
                visibleCount = visibleCount
            ) {
                Text(
                    text = texts[1],
                    typography = AppTheme.typography.Display.D1100
                )
            }
            VerticalSpacerD1000()

            StaggeredContent(
                index = 2,
                visibleCount = visibleCount
            ) {
                Column {
                    // Yes checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimension.D1000),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selection == OnboardingSelection.YES,
                            onCheckedChange = { if (it) onYesSelected() }
                        )
                        Spacer(modifier = Modifier.width(Dimension.D500))
                        Text(
                            text = OnboardingCopy.Consent.yes,
                            typography = AppTheme.typography.Body.B700
                        )
                    }

                    VerticalSpacerD500()

                    // No checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimension.D1000),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selection == OnboardingSelection.NO,
                            onCheckedChange = { if (it) onNoSelected() }
                        )
                        Spacer(modifier = Modifier.width(Dimension.D500))
                        Text(
                            text = OnboardingCopy.Consent.no,
                            typography = AppTheme.typography.Body.B700
                        )
                    }
                }
            }
        }
    }
}

// Previews

@Preview
@Composable
private fun ConsentPagePreview_NoSelection() {
    PreviewContent {
        ConsentPage(
            selection = null,
            animationsEnabled = false,
            onYesSelected = {},
            onNoSelected = {}
        )
    }
}

@Preview
@Composable
private fun ConsentPagePreview_YesSelected() {
    PreviewContent {
        ConsentPage(
            selection = OnboardingSelection.YES,
            animationsEnabled = false,
            onYesSelected = {},
            onNoSelected = {}
        )
    }
}

@Preview
@Composable
private fun ConsentPagePreview_NoSelected() {
    PreviewContent {
        ConsentPage(
            selection = OnboardingSelection.NO,
            animationsEnabled = false,
            onYesSelected = {},
            onNoSelected = {}
        )
    }
}
