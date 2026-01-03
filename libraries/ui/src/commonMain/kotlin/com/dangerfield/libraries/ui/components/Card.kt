package com.dangerfield.libraries.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.merizo.system.AppTheme
import com.dangerfield.merizo.system.Dimension
import com.dangerfield.merizo.system.Radii
import com.dangerfield.libraries.ui.Elevation

@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
        onClick = onClick ?: {},
        bounceScale = if (onClick != null) 0.9f else 1f,
        elevation = Elevation.Button,
        radius = Radii.Card,
        contentPadding = PaddingValues(Dimension.D1000)
    ) {
        content()
    }
}


@Composable
fun CardSecondary(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = AppTheme.colors.surfaceSecondary,
        contentColor = AppTheme.colors.onSurfaceSecondary,
        onClick = onClick ?: {},
        bounceScale = if (onClick != null) 0.9f else 1f,
        elevation = Elevation.Button,
        radius = Radii.Card,
        contentPadding = PaddingValues(Dimension.D1000)
    ) {
        content()
    }
}
