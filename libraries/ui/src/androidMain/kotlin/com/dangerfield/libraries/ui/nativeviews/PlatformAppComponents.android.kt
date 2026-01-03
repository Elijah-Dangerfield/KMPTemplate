package com.dangerfield.libraries.ui.nativeviews

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.libraries.ui.system.color.ColorResource
import com.dangerfield.merizo.libraries.merizo.PlatformApp
import com.dangerfield.merizo.system.typography.TypographyResource

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun PlatformAppIcon(
    selection: PlatformApp,
    modifier: Modifier,
    backgroundColor: ColorResource
) {
    ComposeAppSelectionIcon(
        selection = selection,
        modifier = modifier,
        backgroundColor = backgroundColor
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun PlatformAppText(
    selection: PlatformApp,
    modifier: Modifier,
    backgroundColor: ColorResource,
    typography: TypographyResource
) {
    ComposeAppSelectionText(
        selection = selection,
        modifier = modifier,
        typography = typography
    )
}
