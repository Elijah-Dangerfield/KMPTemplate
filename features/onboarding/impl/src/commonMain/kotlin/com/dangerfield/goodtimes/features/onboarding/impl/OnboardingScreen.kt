package com.dangerfield.goodtimes.features.onboarding.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.CircularProgressIndicator
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonSize
import com.dangerfield.libraries.ui.components.button.ButtonType
import com.dangerfield.libraries.ui.components.text.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun OnboardingScreen(
    state: State,
    onNextClicked: () -> Unit,
    onYesSelected: () -> Unit,
    onNoSelected: () -> Unit,
    onConfirmClicked: () -> Unit
) {
    Screen(modifier = Modifier.fillMaxSize()) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = state.currentPage,
                pageCount = { OnboardingPage.entries.size }
            )

            // Track which pages have completed their typewriter effect
            var isCurrentPageReady by remember { mutableStateOf(false) }

            // Reset readiness when page changes
            LaunchedEffect(state.currentPage) {
                isCurrentPageReady = false
                if (pagerState.currentPage != state.currentPage) {
                    pagerState.animateScrollToPage(state.currentPage)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimension.D800)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    userScrollEnabled = false
                ) { page ->
                    val onTypewriterComplete = {
                        if (page == state.currentPage) {
                            isCurrentPageReady = true
                        }
                    }

                    when (OnboardingPage.entries[page]) {
                        OnboardingPage.INTRO -> IntroPage(
                            onTypewriterComplete = onTypewriterComplete
                        )
                        OnboardingPage.WHAT_I_KNOW -> WhatIKnowPage(
                            onTypewriterComplete = onTypewriterComplete
                        )
                        OnboardingPage.UNDERSTANDING_YOU -> UnderstandingYouPage(
                            onTypewriterComplete = onTypewriterComplete
                        )
                        OnboardingPage.PAGES -> PagesPage(
                            onTypewriterComplete = onTypewriterComplete
                        )
                        OnboardingPage.PRIVACY -> PrivacyPage(
                            onTypewriterComplete = onTypewriterComplete
                        )
                        OnboardingPage.CONSENT -> ConsentPage(
                            selection = state.selection,
                            onYesSelected = onYesSelected,
                            onNoSelected = onNoSelected,
                            onTypewriterComplete = onTypewriterComplete
                        )
                    }
                }

                VerticalSpacerD800()

                // Bottom button - only show when:
                // - On non-final pages: after typewriter completes
                // - On final page: only after YES is selected
                val showButton = isCurrentPageReady && state.canProceed
                
                AnimatedVisibility(
                    visible = showButton,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            if (state.isOnFinalPage) {
                                onConfirmClicked()
                            } else {
                                onNextClicked()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.Large,
                        type = ButtonType.Primary
                    ) {
                        Text(text = if (state.isOnFinalPage) "Continue" else "Next →")
                    }
                }

                VerticalSpacerD1000()
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview_Loading() {
    PreviewContent {
        OnboardingScreen(
            state = State(isLoading = true),
            onNextClicked = {},
            onYesSelected = {},
            onNoSelected = {},
            onConfirmClicked = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview_Intro() {
    PreviewContent {
        OnboardingScreen(
            state = State(isLoading = false, currentPage = 0),
            onNextClicked = {},
            onYesSelected = {},
            onNoSelected = {},
            onConfirmClicked = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview_Consent_NoSelection() {
    PreviewContent {
        OnboardingScreen(
            state = State(
                isLoading = false,
                currentPage = OnboardingPage.CONSENT.ordinal,
                selection = null
            ),
            onNextClicked = {},
            onYesSelected = {},
            onNoSelected = {},
            onConfirmClicked = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview_Consent_YesSelected() {
    PreviewContent {
        OnboardingScreen(
            state = State(
                isLoading = false,
                currentPage = OnboardingPage.CONSENT.ordinal,
                selection = OnboardingSelection.YES
            ),
            onNextClicked = {},
            onYesSelected = {},
            onNoSelected = {},
            onConfirmClicked = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview_Consent_NoSelected() {
    PreviewContent {
        OnboardingScreen(
            state = State(
                isLoading = false,
                currentPage = OnboardingPage.CONSENT.ordinal,
                selection = OnboardingSelection.NO
            ),
            onNextClicked = {},
            onYesSelected = {},
            onNoSelected = {},
            onConfirmClicked = {}
        )
    }
}
