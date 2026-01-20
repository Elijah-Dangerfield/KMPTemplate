package com.dangerfield.goodtimes.features.tasks.impl.templates.prompt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.VerticalSpacerD800
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonStyle
import com.dangerfield.libraries.ui.components.icon.IconButton
import com.dangerfield.libraries.ui.components.icon.Icons
import com.dangerfield.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.screenContentPadding
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PromptScreen(
    state: PromptState,
    onAction: (PromptAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val canSubmit = state.text.isNotBlank() || state.photoPath != null
    
    Screen(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .screenContentPadding(
                    paddingValues = paddingValues,
                    includeImePadding = true
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VerticalSpacerD1000()
            
            // Instruction
            Text(
                text = state.instruction,
                typography = AppTheme.typography.Heading.H700,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.text,
            )
            
            VerticalSpacerD800()
            
            // Text input area
            OutlinedTextField(
                value = state.text,
                onValueChange = { onAction(PromptAction.UpdateText(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimension.D1900),
                placeholder = { Text(state.placeholder.ifEmpty { "Write something..." }) },
                singleLine = false,
                minLines = 6,
                maxLines = 10,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus(force = true)
                    }
                )
            )
            
            // Optional photo attachment indicator
            if (state.allowsPhoto) {
                VerticalSpacerD500()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.photoPath != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(Dimension.D300))
                                .background(AppTheme.colors.surfaceSecondary.color),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(
                                onClick = { onAction(PromptAction.RemovePhoto) },
                                icon = Icons.Close(null),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { /* TODO: Launch camera/gallery picker */ },
                            icon = Icons.Camera(null),
                        )
                    }
                }
            }
            
            VerticalSpacerD800()
            
            // Submit button
            Button(
                onClick = {
                    focusManager.clearFocus(force = true)
                    onAction(PromptAction.Submit)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit,
            ) {
                Text("Done")
            }
            
            VerticalSpacerD800()
        }
    }
    
    // Depth prompt dialog - shown when response is too short
    if (state.showDepthPrompt) {
        AlertDialog(
            onDismissRequest = { onAction(PromptAction.DismissDepthPrompt(continueAnyway = false)) },
            title = null,
            text = {
                Text(
                    text = state.depthPromptMessage,
                    typography = AppTheme.typography.Body.B600,
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAction(PromptAction.DismissDepthPrompt(continueAnyway = false)) },
                    style = ButtonStyle.Outlined,
                ) {
                    Text("I'll add more")
                }
            },
            dismissButton = {
                Button(
                    onClick = { onAction(PromptAction.DismissDepthPrompt(continueAnyway = true)) },
                    style = ButtonStyle.Text,
                ) {
                    Text("Keep it short")
                }
            },
            containerColor = AppTheme.colors.surfacePrimary.color,
        )
    }
}

@Preview
@Composable
private fun PromptScreenPreview() {
    PreviewContent {
        PromptScreen(
            state = PromptState(
                instruction = "What's something you used to believe that you don't anymore?",
                placeholder = "I used to think...",
                allowsPhoto = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PromptScreenWithTextPreview() {
    PreviewContent {
        PromptScreen(
            state = PromptState(
                instruction = "Write about a moment that changed you.",
                placeholder = "",
                text = "There was this one time when I realized that everything I thought I knew about myself was wrong...",
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PromptScreenDepthPromptPreview() {
    PreviewContent {
        PromptScreen(
            state = PromptState(
                instruction = "What's a belief you've changed your mind about?",
                placeholder = "",
                text = "Nothing really",
                requiresDepth = true,
                minCharacters = 20,
                showDepthPrompt = true,
                depthPromptMessage = "Just that? I'm curious to hear more if you're up for it.",
            ),
            onAction = {},
        )
    }
}
