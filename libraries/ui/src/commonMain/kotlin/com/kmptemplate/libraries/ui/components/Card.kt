package com.kmptemplate.libraries.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kmptemplate.system.AppTheme
import com.kmptemplate.system.Dimension
import com.kmptemplate.system.Radii
import com.kmptemplate.libraries.ui.Elevation
import com.kmptemplate.libraries.ui.system.color.ColorResource

@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: ColorResource? = AppTheme.colors.surfacePrimary,
    contentColor: ColorResource = AppTheme.colors.onSurfacePrimary,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
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
