package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonSize
import com.dangerfield.libraries.ui.components.button.ButtonStyle
import com.dangerfield.libraries.ui.components.icon.Icon
import com.dangerfield.libraries.ui.components.icon.IconButton
import com.dangerfield.libraries.ui.components.icon.Icons
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.screenHorizontalInsets
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.HorizontalSpacerD200
import com.dangerfield.goodtimes.system.Radii
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.goodtimes.system.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Instant

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    Screen(
        modifier.padding(screenHorizontalInsets)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding()
        ) {

            Text("Settings")
        }
    }
}

