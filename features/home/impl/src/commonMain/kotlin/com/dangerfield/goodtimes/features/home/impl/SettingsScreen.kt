package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.goodtimes.system.VerticalSpacerD200
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.bounceClick
import com.dangerfield.libraries.ui.components.ListItemAccessory
import com.dangerfield.libraries.ui.components.ListSection
import com.dangerfield.libraries.ui.components.ListSectionItem
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.header.TopBar
import com.dangerfield.libraries.ui.components.icon.Icon
import com.dangerfield.libraries.ui.components.icon.IconButton
import com.dangerfield.libraries.ui.components.icon.IconResource
import com.dangerfield.libraries.ui.components.icon.IconSize
import com.dangerfield.libraries.ui.components.icon.Icons
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.fadingEdge
import com.dangerfield.libraries.ui.screenHorizontalInsets
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SettingsScreen(
    visitCount: Int,
    state: SettingsState,
    isDebug: Boolean,
    onAction: (SettingsAction) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Screen(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(
                title = ScreenCopy.getSettingsTitle(visitCount),
                onNavigateBack = onBackClicked
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fadingEdge(scrollState)
                .padding(paddingValues)
                .padding(screenHorizontalInsets)
        ) {


            VerticalSpacerD1000()

            ListSection(
                title = "General",
                items = buildList {
                    if (state.canAnswerMood && !state.moodBannerDisabled) {
                        add(
                            ListSectionItem(
                                headlineText = "How are you feeling?",
                                supportingText = getMoodPromptSupportingText(state.isFirstEverMoodPrompt),
                                leadingContent = { SettingsListIcon(Icons.Charity("Answer mood")) },
                                onClick = { onAction(SettingsAction.OpenMoodPrompt) }
                            ))
                    }
                    add(
                        ListSectionItem(
                            headlineText = "About you",
                            supportingText = ScreenCopy.getAboutYouSubtitle(state.aboutYouVisits),
                            leadingContent = { SettingsListIcon(Icons.Person("About you")) },
                            onClick = { onAction(SettingsAction.OpenAboutYou) }
                        ))
                    add(
                        ListSectionItem(
                            headlineText = "About me",
                            supportingText = ScreenCopy.getAboutMeSubtitle(state.aboutMeVisits),
                            leadingContent = { SettingsListIcon(Icons.Info("About me")) },
                            onClick = { onAction(SettingsAction.OpenAboutMe) }
                        ))
                    add(
                        ListSectionItem(
                            headlineText = "Mood Checks",
                            supportingText = getMoodToggleSupportingText(
                                isDisabled = state.moodBannerDisabled,
                                toggleCount = state.moodBannerToggleCount
                            ),
                            leadingContent = { SettingsListIcon(Icons.Charity("Mood tracking")) },
                            accessory = ListItemAccessory.Switch(
                                checked = !state.moodBannerDisabled,
                                onCheckedChange = { enabled ->
                                    onAction(SettingsAction.ToggleMoodBanner(enabled))
                                }
                            ),
                            onClick = null
                        ))
                    // Easter egg: Secret option that randomly appears
                    if (state.showSecretOption) {
                        add(
                            ListSectionItem(
                                headlineText = "Secret Option",
                                supportingText = "Wait, how did this get here?",
                                leadingContent = { SettingsListIcon(Icons.Question("Secret")) },
                                onClick = { onAction(SettingsAction.OpenSecretOption) }
                            ))
                    }
                    add(
                        ListSectionItem(
                            headlineText = "Fresh Start",
                            supportingText = ScreenCopy.getFreshStartSubtitle(state.freshStartVisits),
                            leadingContent = { SettingsListIcon(Icons.Refresh("Fresh Start")) },
                            onClick = { onAction(SettingsAction.OpenFreshStartDialog) }
                        ))
                }
            )

            VerticalSpacerD1000()

            ListSection(
                title = "Support",
                items = listOf(
                    ListSectionItem(
                        headlineText = "Report a Bug",
                        leadingContent = { SettingsListIcon(Icons.Bug("Report a Bug")) },
                        onClick = { onAction(SettingsAction.OpenBugReport) }
                    ),
                    ListSectionItem(
                        headlineText = "Give Feedback",
                        leadingContent = { SettingsListIcon(Icons.Chat("Give Feedback")) },
                        onClick = { onAction(SettingsAction.OpenFeedback) }
                    )
                )
            )

            if (isDebug) {
                VerticalSpacerD1000()

                ListSection(
                    title = "Developer",
                    items = listOf(
                        ListSectionItem(
                            headlineText = "QA Menu",
                            supportingText = "Testing tools and debug options",
                            leadingContent = { SettingsListIcon(Icons.Settings("QA Menu")) },
                            onClick = { onAction(SettingsAction.OpenQAMenu) }
                        )
                    )
                )
            }

            VerticalSpacerD1000()

            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.bounceClick(onClick = { onAction(SettingsAction.ClickMadeWithLove) }),

                    ) {
                    // Easter egg: Click 100 times to unlock persistence achievement
                    Text(
                        text = if (state.persistenceUnlocked) {
                            "Made with ❤️ and persistence."
                        } else {
                            "Made with ❤️. Or I guess \"born\" with it."
                        },
                        typography = AppTheme.typography.Body.B500,
                        color = AppTheme.colors.onSurfaceSecondary,
                    )

                    VerticalSpacerD500()

                    Text(
                        text = "The App of Good Times",
                        typography = AppTheme.typography.Body.B400,
                        color = AppTheme.colors.onSurfaceDisabled
                    )

                }

                VerticalSpacerD1000()

                // Show teasing hint messages
                AnimatedVisibility(
                    visible = state.madeWithLoveHint != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    state.madeWithLoveHint?.let { hint ->
                        VerticalSpacerD200()
                        Text(
                            text = hint,
                            typography = AppTheme.typography.Body.B400,
                            color = AppTheme.colors.accentPrimary
                        )
                    }
                }


                if (state.showUselessButton) {
                    VerticalSpacerD1000()

                    IconButton(
                        onClick = { onAction(SettingsAction.ClickUselessButton) },
                        icon = Icons.Dot(null)
                    )

                    VerticalSpacerD500()
                }
            }
        }
    }
}

@Composable
private fun SettingsListIcon(icon: IconResource) {
    Icon(
        icon = icon,
        color = AppTheme.colors.onSurfacePrimary,
        size = IconSize.Small
    )
}

private fun getMoodToggleSupportingText(isDisabled: Boolean, toggleCount: Int): String {
    return when {
        // High toggle counts - they keep flipping it back and forth
        toggleCount >= 10 -> if (isDisabled) {
            "At this point I feel like we're just hanging out."
        } else {
            "I've lost track of what you want. But I'll ask anyway."
        }

        toggleCount >= 8 -> if (isDisabled) {
            "You know what, I respect the commitment to uncertainty."
        } else {
            "Back again! I'm starting to think you just like toggling things."
        }

        toggleCount >= 6 -> if (isDisabled) {
            "This toggle has seen some things."
        } else {
            "The toggle returns. It always returns."
        }

        toggleCount >= 4 -> if (isDisabled) {
            "You keep toggling me. Very weird. But I'm not judging."
        } else {
            "Changed your mind again? That's okay, I change mine too."
        }

        toggleCount >= 2 -> if (isDisabled) {
            "Okay okay, I'll stop asking. For real this time. Maybe."
        } else {
            "Welcome back! Missed you. Well, missed asking you things."
        }
        // First toggle or normal state
        else -> if (isDisabled) {
            "I won't ask. But I'm still curious."
        } else {
            "I'll check in at the start of each session. This helps me understand you a bit better. Which is kinda my whole goal."
        }
    }
}

private fun getMoodPromptSupportingText(isFirstEver: Boolean): String {
    return if (isFirstEver) {
        "I'd like to know"
    } else {
        "You haven't told me this session yet"
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    PreviewContent {
        SettingsScreen(
            visitCount = 1,
            state = SettingsState(
                isLoading = false,
                moodBannerDisabled = false,
                showUselessButton = true
            ),
            isDebug = true,
            onAction = {},
            onBackClicked = {}
        )
    }
}
