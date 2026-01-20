package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.VerticalSpacerD1000
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.header.TopBar
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.fadingEdge
import com.dangerfield.libraries.ui.screenHorizontalInsets
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AboutMeScreen(
    state: AboutMeState,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    
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
            
            Text(
                text = state.content,
                typography = AppTheme.typography.Body.B600,
                color = AppTheme.colors.text,
            )
            
            VerticalSpacerD1000()
        }
    }
}

@Preview
@Composable
fun AboutMeScreenPreview() {
    PreviewContent {
        AboutMeScreen(
            state = AboutMeState(),
            onBackClicked = {}
        )
    }
}
