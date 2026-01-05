package com.dangerfield.goodtimes.features.onboarding.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.checkbox.Checkbox
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.components.text.TypewriterTextEffect
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ConsentPage(
    selection: OnboardingSelection?,
    onYesSelected: () -> Unit,
    onNoSelected: () -> Unit,
    onTypewriterComplete: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠️",
                typography = AppTheme.typography.Display.D1400
            )
            VerticalSpacerD800()
            TypewriterTextEffect(
                text = "Do you want to do this?",
                minDelayInMillis = 40,
                maxDelayInMillis = 100,
                onEffectCompleted = onTypewriterComplete
            ) { displayedText ->
                Text(
                    text = displayedText,
                    typography = AppTheme.typography.Display.D1100
                )
            }
            VerticalSpacerD1000()

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
                    text = "Yes",
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
                    text = "No",
                    typography = AppTheme.typography.Body.B700
                )
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
            onYesSelected = {},
            onNoSelected = {}
        )
    }
}
