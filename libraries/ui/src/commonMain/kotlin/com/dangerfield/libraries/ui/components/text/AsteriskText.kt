package com.dangerfield.libraries.ui.components.text

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.HorizontalSpacerD200
import com.dangerfield.libraries.ui.system.color.ColorResource

@Composable
fun AsteriskText(text: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        text()
        HorizontalSpacerD200()
        Text(
            text = "*",
            typography = AppTheme.typography.Display.D800,
            color = ColorResource.Red500
        )
    }
}