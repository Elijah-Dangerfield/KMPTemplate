package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.libraries.goodtimes.Observation
import com.dangerfield.goodtimes.libraries.goodtimes.ObservationCategory
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonStyle
import com.dangerfield.libraries.ui.components.header.TopBar
import com.dangerfield.libraries.ui.components.icon.Icon
import com.dangerfield.libraries.ui.components.icon.IconSize
import com.dangerfield.libraries.ui.components.icon.Icons
import com.dangerfield.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.fadingEdge
import com.dangerfield.libraries.ui.screenHorizontalInsets
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AboutYouScreen(
    state: AboutYouState,
    onBackClicked: () -> Unit,
    onAction: (AboutYouAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var isEditingName by remember { mutableStateOf(false) }
    // Track if the user has a saved name (from the initial load, not typing)
    val hasSavedName = state.savedName.isNotBlank()
    
    Screen(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(
                title = state.title,
                onNavigateBack = onBackClicked
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .fadingEdge(scrollState)
                .padding(paddingValues)
                .padding(screenHorizontalInsets)
        ) {
            VerticalSpacerD1000()
            
            // Name section - show editable form or the name observation
            AnimatedContent(
                targetState = isEditingName || !hasSavedName,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "name_section"
            ) { showEditForm ->
                if (showEditForm) {
                    NameEditSection(
                        name = state.name,
                        hasName = hasSavedName,
                        onNameChanged = { onAction(AboutYouAction.NameChanged(it)) },
                        onSave = {
                            onAction(AboutYouAction.SaveName)
                            isEditingName = false
                        }
                    )
                } else {
                    NameDisplaySection(
                        name = state.savedName,
                        onEditClick = { isEditingName = true }
                    )
                }
            }
            
            VerticalSpacerD1000()
            VerticalSpacerD1000()
            
            // Observations section
            Text(
                text = "What I've noticed",
                typography = AppTheme.typography.Heading.H600,
            )
            
            VerticalSpacerD500()
            
            AnimatedVisibility(
                visible = !state.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    if (state.hasObservations) {
                        state.observations.forEach { observation ->
                            ObservationItem(observation = observation)
                            VerticalSpacerD800()
                        }
                    } else {
                        EmptyObservationsMessage()
                    }
                }
            }
            
            VerticalSpacerD1000()
            
            Text(
                text = "As I learn more about you—your patterns, your preferences, what makes a good time for you—I'll add it here. This is your space to see what I see.",
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
            )
            
            VerticalSpacerD1000()
        }
    }
}

@Composable
private fun NameEditSection(
    name: String,
    hasName: Boolean,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = if (hasName) "What should I call you instead?" else "What should I call you?",
            typography = AppTheme.typography.Heading.H700,
        )
        
        VerticalSpacerD500()

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            placeholder = { Text("Your name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        
        VerticalSpacerD500()
        
        Button(
            onClick = onSave,
            style = ButtonStyle.Outlined,
            enabled = name.isNotBlank(),
        ) {
            Text(text = "Save")
        }
    }
}

@Composable
private fun NameDisplaySection(
    name: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "I call you",
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
            )
            Text(
                text = name,
                typography = AppTheme.typography.Heading.H700,
            )
        }
        Icon(
            icon = Icons.Pencil("Edit name"),
            color = AppTheme.colors.textSecondary,
            size = IconSize.Small,
        )
    }
}

@Composable
private fun ObservationItem(
    observation: Observation,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = observation.message,
            typography = AppTheme.typography.Body.B600,
        )
    }
}

@Composable
private fun EmptyObservationsMessage(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Not much yet. We're just getting started.",
        typography = AppTheme.typography.Body.B600,
        color = AppTheme.colors.textSecondary,
        modifier = modifier,
    )
}

@Preview
@Composable
fun AboutYouScreenPreview() {
    PreviewContent {
        AboutYouScreen(
            state = AboutYouState(isLoading = false),
            onBackClicked = {},
            onAction = {}
        )
    }
}

@Preview
@Composable
fun AboutYouScreenWithNamePreview() {
    PreviewContent {
        AboutYouScreen(
            state = AboutYouState(savedName = "Alex", name = "Alex", isLoading = false),
            onBackClicked = {},
            onAction = {}
        )
    }
}

@Preview
@Composable
fun AboutYouScreenWithObservationsPreview() {
    PreviewContent {
        AboutYouScreen(
            state = AboutYouState(
                savedName = "Alex",
                name = "Alex",
                isLoading = false,
                observations = listOf(
                    Observation(
                        id = "name_known",
                        message = "Alex. I remember.",
                        category = ObservationCategory.IDENTITY,
                        priority = 8,
                    ),
                    Observation(
                        id = "night_owl",
                        message = "I've noticed you often come by after midnight. About 45% of our time together is in the quiet hours. Night owl?",
                        category = ObservationCategory.TIMING,
                        priority = 8,
                    ),
                    Observation(
                        id = "committed",
                        message = "You finish what you start—85% completion rate. That's not nothing.",
                        category = ObservationCategory.ENGAGEMENT,
                        priority = 9,
                    ),
                    Observation(
                        id = "wordsmith",
                        message = "You have a lot to say—your responses average 180 characters. Words seem to come naturally to you.",
                        category = ObservationCategory.EXPRESSION,
                        priority = 8,
                    ),
                )
            ),
            onBackClicked = {},
            onAction = {}
        )
    }
}
