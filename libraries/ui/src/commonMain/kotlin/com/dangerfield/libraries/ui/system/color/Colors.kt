package com.dangerfield.merizo.system.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangerfield.libraries.ui.system.LocalContentColor
import com.dangerfield.libraries.ui.system.color.ColorCard
import com.dangerfield.libraries.ui.system.color.ColorResource
import com.dangerfield.libraries.ui.system.color.toHexString
import com.dangerfield.merizo.system.Dimension
import com.dangerfield.merizo.system.Radii
import org.jetbrains.compose.ui.tooling.preview.Preview

@Immutable
@Suppress("LongParameterList")
interface Colors {

    val accentPrimary: ColorResource
    val onAccentPrimary: ColorResource
    val accentSecondary: ColorResource
    val onAccentSecondary: ColorResource

    @Deprecated("Use accentPrimary instead", ReplaceWith("accentPrimary"))
    val accent: ColorResource get() = accentPrimary

    @Deprecated("Use onAccentPrimary instead", ReplaceWith("onAccentPrimary"))
    val onAccent: ColorResource get() = onAccentPrimary

    /* Backgrounds */
    val shadow: ColorResource
    val background: ColorResource
    val backgroundOverlay: ColorResource
    val onBackground: ColorResource
    val border: ColorResource

    val borderSecondary: ColorResource
    val borderDisabled: ColorResource

    /* Texts */
    val text: ColorResource
    val textSecondary: ColorResource
    val textDisabled: ColorResource
    val danger: ColorResource

    val status: StatusColor

    /* Surfaces */
    val surfacePrimary: ColorResource
    val onSurfacePrimary: ColorResource
    val surfaceSecondary: ColorResource
    val onSurfaceSecondary: ColorResource
    val surfaceTertiary: ColorResource
    val onSurfaceTertiary: ColorResource

    val surfaceDisabled: ColorResource
    val onSurfaceDisabled: ColorResource

}

interface StatusColor {
    val okay: ColorResource
    val warning: ColorResource
    val bad: ColorResource
}

val defaultColors = object : Colors {
    override val accentPrimary = ColorResource.Lumen500
    override val onAccentPrimary = ColorResource.Frost50
    override val accentSecondary = ColorResource.Catalyst400
    override val onAccentSecondary = ColorResource.Obsidian950

    override val shadow = ColorResource.Black900_A_70
    override val textDisabled = ColorResource.Frost400
    override val danger = ColorResource.Red500
    override val surfacePrimary = ColorResource.Slate600
    override val surfaceDisabled = ColorResource.Slate200
    override val onSurfacePrimary = ColorResource.Frost50
    override val surfaceSecondary = ColorResource.Graphite600
    override val onSurfaceSecondary = ColorResource.Frost100
    override val surfaceTertiary = ColorResource.Aurora400
    override val onSurfaceTertiary = ColorResource.Frost200
    override val onSurfaceDisabled = ColorResource.Frost400
    override val background = ColorResource.Obsidian900
    override val onBackground = ColorResource.Frost50
    override val border = ColorResource.Graphite500
    override val borderSecondary = ColorResource.Graphite400
    override val borderDisabled = ColorResource.Slate100
    override val text = ColorResource.Frost50
    override val backgroundOverlay = ColorResource.Obsidian900_A_80
    override val textSecondary = ColorResource.Frost200

    override val status = object : StatusColor {
        override val okay = ColorResource.Green500
        override val warning = ColorResource.Gold500
        override val bad = ColorResource.Red500
    }
}

@Composable
private fun SectionTitle(text: String, colors: Colors) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary.color,
        modifier = Modifier.padding(bottom = Dimension.D400)
    )
}

@Composable
private fun HeroPanel(colors: Colors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.Card.shape)
            .background(colors.surfacePrimary.color)
            .border(1.dp, colors.border.color, Radii.Card.shape)
            .padding(Dimension.D700)
    ) {
        Text(
            text = "Virtu palette",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfacePrimary.color
        )
        Text(
            text = "Discipline-first dark theme",
            fontSize = 14.sp,
            color = colors.textSecondary.color,
            modifier = Modifier.padding(top = Dimension.D200)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimension.D600),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.Card.shape)
                    .background(colors.surfaceSecondary.color)
                    .padding(Dimension.D500)
            ) {
                Text(
                    text = "Focus session",
                    fontSize = 14.sp,
                    color = colors.onSurfaceSecondary.color
                )
                Text(
                    text = "42m remaining",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceSecondary.color,
                    modifier = Modifier.padding(top = Dimension.D200)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.Card.shape)
                    .background(colors.backgroundOverlay.color)
                    .padding(Dimension.D500)
            ) {
                Text(
                    text = "Next violation",
                    fontSize = 14.sp,
                    color = colors.onBackground.color
                )
                Text(
                    text = "$25 donation",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentSecondary.color,
                    modifier = Modifier.padding(top = Dimension.D200)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimension.D600),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Apps unblocked in the last hour",
                fontSize = 14.sp,
                color = colors.textSecondary.color
            )
            Box(
                modifier = Modifier
                    .clip(Radii.Button.shape)
                    .background(colors.accentPrimary.color)
                    .padding(horizontal = Dimension.D800, vertical = Dimension.D400)
            ) {
                Text(
                    text = "Review rules",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onAccentPrimary.color
                )
            }
        }
    }
}

@Composable
private fun AccentPalette(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Accent stack", colors)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            AccentChip(
                label = "Primary",
                background = colors.accentPrimary,
                foreground = colors.onAccentPrimary,
                supporting = colors.accentPrimary.toHexString()
            )
            AccentChip(
                label = "Secondary",
                background = colors.accentSecondary,
                foreground = colors.onAccentSecondary,
                supporting = colors.accentSecondary.toHexString()
            )
        }
    }
}

@Composable
private fun RowScope.AccentChip(
    label: String,
    background: ColorResource,
    foreground: ColorResource,
    supporting: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(Radii.Card.shape)
            .background(background.color.copy(alpha = 0.15f))
            .border(1.dp, background.color, Radii.Card.shape)
            .padding(Dimension.D500)
    ) {
        Box(
            modifier = Modifier
                .clip(Radii.Button.shape)
                .background(background.color)
                .padding(horizontal = Dimension.D800, vertical = Dimension.D400)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = foreground.color
            )
        }
        Text(
            text = background.designSystemName,
            fontSize = 12.sp,
            color = background.color,
            modifier = Modifier.padding(top = Dimension.D300)
        )
        Text(
            text = supporting,
            fontSize = 10.sp,
            color = foreground.color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SurfaceStack(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Surface ladder", colors)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            SurfaceCard(
                title = "Primary",
                background = colors.surfacePrimary,
                foreground = colors.onSurfacePrimary,
                border = colors.border,
                supporting = colors.surfacePrimary.toHexString()
            )
            SurfaceCard(
                title = "Secondary",
                background = colors.surfaceSecondary,
                foreground = colors.onSurfaceSecondary,
                border = colors.border,
                supporting = colors.surfaceSecondary.toHexString()
            )
            SurfaceCard(
                title = "Tertiary",
                background = colors.surfaceTertiary,
                foreground = colors.onSurfaceTertiary,
                border = colors.border,
                supporting = colors.surfaceTertiary.toHexString()
            )
        }
    }
}

@Composable
private fun RowScope.SurfaceCard(
    title: String,
    background: ColorResource,
    foreground: ColorResource,
    border: ColorResource,
    supporting: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(Radii.Card.shape)
            .background(background.color)
            .border(1.dp, border.color, Radii.Card.shape)
            .padding(Dimension.D500)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = foreground.color.copy(alpha = 0.9f)
        )
        Text(
            text = "Task summary",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground.color,
            modifier = Modifier.padding(top = Dimension.D200)
        )
        Text(
            text = supporting,
            fontSize = 10.sp,
            color = foreground.color.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = Dimension.D300)
        )
    }
}

@Composable
private fun TextHierarchy(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Typography contrast", colors)
        Column(
            modifier = Modifier
                .clip(Radii.Card.shape)
                .background(colors.background.color)
                .border(1.dp, colors.border.color, Radii.Card.shape)
                .padding(Dimension.D600),
            verticalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            TextSample("Primary", colors.text, colors.text)
            TextSample("Secondary", colors.textSecondary, colors.textSecondary)
            TextSample("Disabled", colors.textDisabled, colors.textDisabled)
            TextSample("Warning", colors.danger, colors.danger)
        }
    }
}

@Composable
private fun TextSample(label: String, swatch: ColorResource, hexColor: ColorResource) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = swatch.color
        )
        Text(
            text = hexColor.toHexString(),
            fontSize = 11.sp,
            color = swatch.color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SemanticStrip(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("System states", colors)
        Row(
            modifier = Modifier
                .clip(Radii.Card.shape)
                .border(1.dp, colors.border.color, Radii.Card.shape)
                .background(colors.surfaceSecondary.color)
                .padding(Dimension.D400),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D400)
        ) {
            SemanticBadge("Background", colors.background, colors.onBackground)
            SemanticBadge("Overlay", colors.backgroundOverlay, colors.onBackground)
            SemanticBadge("Shadow", colors.shadow, colors.onBackground)
            SemanticBadge("Warning", colors.danger, colors.onAccentSecondary)
        }
    }
}

@Composable
private fun RowScope.SemanticBadge(
    label: String,
    background: ColorResource,
    content: ColorResource
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(Radii.Card.shape)
            .background(background.color)
            .padding(Dimension.D400)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = content.color.copy(alpha = 0.8f)
        )
        Text(
            text = background.designSystemName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = content.color,
            modifier = Modifier.padding(top = Dimension.D200)
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PaletteGridSection(colors: Colors) {
    val palette = listOf(
        colors.background,
        colors.backgroundOverlay,
        colors.onBackground,
        colors.surfacePrimary,
        colors.onSurfacePrimary,
        colors.surfaceSecondary,
        colors.onSurfaceSecondary,
        colors.surfaceTertiary,
        colors.onSurfaceTertiary,
        colors.surfaceDisabled,
        colors.onSurfaceDisabled,
        colors.accentPrimary,
        colors.onAccentPrimary,
        colors.accentSecondary,
        colors.onAccentSecondary,
        colors.text,
        colors.textSecondary,
        colors.textDisabled,
        colors.danger,
        colors.border,
        colors.borderDisabled,
        colors.shadow
    ).distinctBy { it.designSystemName }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Palette grid", colors)
        Box(
            modifier = Modifier
                .clip(Radii.Card.shape)
                .background(colors.surfaceSecondary.color)
                .border(1.dp, colors.border.color, Radii.Card.shape)
                .padding(Dimension.D300)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
                verticalArrangement = Arrangement.spacedBy(Dimension.D300)
            ) {
                palette.forEach { swatch ->
                    ColorCard(
                        colorResource = swatch,
                        title = swatch.designSystemName,
                        description = swatch.toHexString()
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewColorSwatch(colors: Colors) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.color)
            .padding(horizontal = Dimension.D800, vertical = Dimension.D600),
        verticalArrangement = Arrangement.spacedBy(Dimension.D700)
    ) {
        item { HeroPanel(colors) }
        item { AccentPalette(colors) }
        item { SurfaceStack(colors) }
        item { TextHierarchy(colors) }
        item { SemanticStrip(colors) }
        item { PaletteGridSection(colors) }
    }
}

@Preview(widthDp = 600, heightDp = 2000, )
@Composable
private fun PreviewDefaultColors() {
    PreviewColorSwatch(defaultColors)
}

@Composable
fun ProvideContentColor(color: ColorResource, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides color,
        androidx.compose.material3.LocalContentColor provides color.color,
        content = content
    )
}