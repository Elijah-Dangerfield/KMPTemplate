package com.dangerfield.libraries.ui.system.color

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.Radii
import com.dangerfield.goodtimes.system.VerticalSpacerD100
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Suppress("ClassNaming")
@Deprecated("AVOID USSING COLOR RESOURCES DIRECTLY. Instead opt for AppTheme.colors.xyz for semantic colors")
@Stable
sealed class ColorResource(val color: Color, val designSystemName: String) {
    object Unspecified : ColorResource(Color.Unspecified, "unspecified")

    // Parchment - Warm background ramp (book-like)
    object Parchment50 : ColorResource(Color(0xFFFFFBF5), "parchment-50")
    object Parchment100 : ColorResource(Color(0xFFFAF6EF), "parchment-100")
    object Parchment200 : ColorResource(Color(0xFFF5EDE0), "parchment-200")
    object Parchment300 : ColorResource(Color(0xFFE8DCC8), "parchment-300")
    object Parchment400 : ColorResource(Color(0xFFD9C9A8), "parchment-400")
    object Parchment500 : ColorResource(Color(0xFFC4B48E), "parchment-500")

    // Sepia - Warm accent tones
    object Sepia600 : ColorResource(Color(0xFF5C4A32), "sepia-600")
    object Sepia500 : ColorResource(Color(0xFF8B7355), "sepia-500")
    object Sepia400 : ColorResource(Color(0xFFA68B5B), "sepia-400")
    object Sepia300 : ColorResource(Color(0xFFC4A574), "sepia-300")

    // Leather - Rich brown accent (bookbinding)
    object Leather700 : ColorResource(Color(0xFF3D2817), "leather-700")
    object Leather600 : ColorResource(Color(0xFF5D3A1A), "leather-600")
    object Leather500 : ColorResource(Color(0xFF8B5A2B), "leather-500")
    object Leather400 : ColorResource(Color(0xFFAA7744), "leather-400")

    // Mahogany - Deep accent (classic book covers)
    object Mahogany700 : ColorResource(Color(0xFF4A1C1C), "mahogany-700")
    object Mahogany600 : ColorResource(Color(0xFF6B2D2D), "mahogany-600")
    object Mahogany500 : ColorResource(Color(0xFF8B4545), "mahogany-500")
    object Mahogany400 : ColorResource(Color(0xFFA65D5D), "mahogany-400")

    // Ink - Dark text colors
    object Ink900 : ColorResource(Color(0xFF1A1A18), "ink-900")
    object Ink800 : ColorResource(Color(0xFF2D2D2A), "ink-800")
    object Ink700 : ColorResource(Color(0xFF3D3D38), "ink-700")
    object Ink600 : ColorResource(Color(0xFF4D4D47), "ink-600")
    object Ink500 : ColorResource(Color(0xFF6B6B62), "ink-500")
    object Ink400 : ColorResource(Color(0xFF8A8A7F), "ink-400")

    // Forest - Secondary accent (nature/vintage)
    object Forest600 : ColorResource(Color(0xFF2D4A3E), "forest-600")
    object Forest500 : ColorResource(Color(0xFF3D6B54), "forest-500")
    object Forest400 : ColorResource(Color(0xFF5A8F72), "forest-400")

    // Red - Error states (used in AsteriskText)
    object Red500 : ColorResource(Color(0xFFFF5C7C), "red-500")

    // Gold - Warning status
    object Gold600 : ColorResource(Color(0xFFE5A000), "gold-600")

    // Utility colors
    object Black900 : ColorResource(Color(0xFF000000), "black-900")
    object Black900_A_70 :
        ColorResource(Color(0xFF000000).copy(alpha = 0.7f), "black-900-a-70")
    object Black900_A_30 :
        ColorResource(Color(0xFF000000).copy(alpha = 0.3f), "black-900-a-30")

    object White900 : ColorResource(Color(0xFFFFFFFF), "white-900")

    class FromColor(color: Color, name: String) : ColorResource(color, name)

    val onColor: ColorResource
        get() {
            return if (color.luminance() > 0.4) Black900 else White900
        }

    fun withAlpha(alpha: Float) = FromColor(this.color.copy(alpha = alpha), this.designSystemName + "_a_${alpha}")
}

@Composable
fun animateColorResourceAsState(
    targetValue: ColorResource,
    animationSpec: AnimationSpec<ColorResource> = spring(),
    label: String = "ColorAnimation",
    finishedListener: ((ColorResource) -> Unit)? = null,
): State<ColorResource> {
    val converter: TwoWayConverter<ColorResource, AnimationVector4D> = remember(targetValue) {
        val colorConverter = (Color.VectorConverter)(targetValue.color.colorSpace)
        TwoWayConverter(convertToVector = { token: ColorResource ->
            colorConverter.convertToVector(token.color)
        }, convertFromVector = { vector ->
            ColorResource.FromColor(
                color = colorConverter.convertFromVector(vector),
                name = targetValue.designSystemName
            )
        })
    }

    return animateValueAsState(
        targetValue = targetValue,
        typeConverter = converter,
        animationSpec = animationSpec,
        label = label,
        finishedListener = finishedListener
    )
}

private val colors = listOf(
    // Parchment (warm backgrounds)
    ColorResource.Parchment50,
    ColorResource.Parchment100,
    ColorResource.Parchment200,
    ColorResource.Parchment300,
    ColorResource.Parchment400,
    ColorResource.Parchment500,
    // Sepia (warm accents)
    ColorResource.Sepia600,
    ColorResource.Sepia500,
    ColorResource.Sepia400,
    ColorResource.Sepia300,
    // Leather (rich brown)
    ColorResource.Leather700,
    ColorResource.Leather600,
    ColorResource.Leather500,
    ColorResource.Leather400,
    // Mahogany (deep accent)
    ColorResource.Mahogany700,
    ColorResource.Mahogany600,
    ColorResource.Mahogany500,
    ColorResource.Mahogany400,
    // Ink (dark text)
    ColorResource.Ink900,
    ColorResource.Ink800,
    ColorResource.Ink700,
    ColorResource.Ink600,
    ColorResource.Ink500,
    ColorResource.Ink400,
    // Forest (secondary accent)
    ColorResource.Forest600,
    ColorResource.Forest500,
    ColorResource.Forest400,
    // Status colors
    ColorResource.Red500,
    ColorResource.Gold600,
    // Utilities
    ColorResource.Black900,
    ColorResource.White900
)

@Preview(widthDp = 2000, heightDp = 10000, showBackground = false)
@Composable
private fun PreviewColorSwatch() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(10)
    ) {
        items(colors) { colorResource ->
            ColorCard(
                colorResource,
                title = colorResource.designSystemName,
                description = colorResource.toHexString()
            )
        }
    }
}


@Composable
internal fun ColorCard(
    colorResource: ColorResource,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.Companion.padding(Dimension.D100)
            .background(colorResource.color, shape = Radii.Card.shape)
            .height(150.dp)
            .width(120.dp)
            .clip(Radii.Card.shape),
        contentAlignment = Alignment.BottomCenter
    ) {


        Column {
            if (colorResource.color.luminance() > 0.5f) {
                HorizontalDivider(
                    color = Color.DarkGray,
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(Dimension.D500)
            ) {

                Text(
                    text = title,
                    color = Color.Black
                )

                VerticalSpacerD100()

                Text(
                    text = description,
                    color = Color.Black
                )
            }
        }

    }
}

/**
 * Extension function to convert a Color object to a hexadecimal string representation.
 * Includes the alpha value by default but can be omitted.
 *
 * @param includeAlpha whether to include the alpha value in the hex string.
 * @return A hex string representation of the color (e.g., "#FFFFFFFF" or "#FFFFFF" if alpha is omitted).
 */
fun ColorResource.toHexString(includeAlpha: Boolean = true): String {
    val color = this.color

    // Handle unspecified color explicitly
    if (color == Color.Unspecified) return "unspecified"

    fun componentToHex(component: Float): String {
        val intVal = (component * 255f).coerceIn(0f, 255f).roundToInt()
        return intVal.toString(16).uppercase().padStart(2, '0')
    }

    val alpha = if (includeAlpha) componentToHex(color.alpha) else ""
    val red = componentToHex(color.red)
    val green = componentToHex(color.green)
    val blue = componentToHex(color.blue)
    return "#${alpha}${red}${green}${blue}"
}