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
import com.dangerfield.merizo.system.Dimension
import com.dangerfield.merizo.system.Radii
import com.dangerfield.merizo.system.VerticalSpacerD100
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Suppress("ClassNaming")
@Deprecated("AVOID USSING COLOR RESOURCES DIRECTLY. Instead opt for AppTheme.colors.xyz for semantic colors")
@Stable
sealed class ColorResource(val color: Color, val designSystemName: String) {
    object Unspecified : ColorResource(Color.Unspecified, "unspecified")

    // Lumen - Primary Accent Ramp (purposeful blurple)
    object Lumen700 : ColorResource(Color(0xFF1A114D), "lumen-700")
    object Lumen600 : ColorResource(Color(0xFF2F1F83), "lumen-600")
    object Lumen500 : ColorResource(Color(0xFF624CFF), "lumen-500")
    object Lumen400 : ColorResource(Color(0xFF8D7FFF), "lumen-400")
    object Lumen300 : ColorResource(Color(0xFFB9ACFF), "lumen-300")

    // Catalyst - Secondary Accent (accountability ember)
    object Catalyst600 : ColorResource(Color(0xFF9E2A00), "catalyst-600")
    object Catalyst500 : ColorResource(Color(0xFFEA4B1D), "catalyst-500")
    object Catalyst400 : ColorResource(Color(0xFFFF7A4F), "catalyst-400")
    object Catalyst300 : ColorResource(Color(0xFFFFA68A), "catalyst-300")

    object Ice500 : ColorResource(Color(0xFF2E70CB), "ice-500")

    // Obsidian - Deep background ramp
    object Obsidian950 : ColorResource(Color(0xFF03050A), "obsidian-950")
    object Obsidian900 : ColorResource(Color(0xFF050812), "obsidian-900")
    object Obsidian850 : ColorResource(Color(0xFF070E1C), "obsidian-850")
    object Obsidian800 : ColorResource(Color(0xFF0B1526), "obsidian-800")
    object Obsidian700 : ColorResource(Color(0xFF111D34), "obsidian-700")
    object Obsidian900_A_70 :
        ColorResource(Color(0xFF050812).copy(alpha = 0.7f), "obsidian-900-a-70")

    object Obsidian900_A_80 :
        ColorResource(Color(0xFF050812).copy(alpha = 0.8f), "obsidian-900-a-80")

    // Slate - Structured surface ramp
    object Slate600 : ColorResource(Color(0xFF151D29), "slate-600")
    object Slate500 : ColorResource(Color(0xFF1C2635), "slate-500")
    object Slate400 : ColorResource(Color(0xFF263040), "slate-400")
    object Slate300 : ColorResource(Color(0xFF303B4C), "slate-300")
    object Slate200 : ColorResource(Color(0xFF3D4858), "slate-200")
    object Slate100 : ColorResource(Color(0xFF4B5768), "slate-100")

    // Frost - Quiet text ramp
    object Frost50 : ColorResource(Color(0xFFF5F7FF), "frost-50")
    object Frost100 : ColorResource(Color(0xFFE6EBFF), "frost-100")
    object Frost200 : ColorResource(Color(0xFFCBD5F6), "frost-200")
    object Frost300 : ColorResource(Color(0xFFAEB8DA), "frost-300")
    object Frost400 : ColorResource(Color(0xFF8892AE), "frost-400")
    object Frost500 : ColorResource(Color(0xFF69718A), "frost-500")

    // Legacy Aurora ramp retained for compatibility
    object Aurora700 : ColorResource(Color(0xFF1A1447), "aurora-700")
    object Aurora600 : ColorResource(Color(0xFF2B1D69), "aurora-600")
    object Aurora500 : ColorResource(Color(0xFF48319B), "aurora-500")
    object Aurora400 : ColorResource(Color(0xFF6244D4), "aurora-400")
    object Aurora300 : ColorResource(Color(0xFF8A6BFF), "aurora-300")
    object Aurora200 : ColorResource(Color(0xFFB19BFF), "aurora-200")
    object Aurora100 : ColorResource(Color(0xFFE0DAFF), "aurora-100")

    // Legacy Pulse ramp retained for compatibility
    object Pulse600 : ColorResource(Color(0xFF6F1F16), "pulse-600")
    object Pulse500 : ColorResource(Color(0xFFD2542D), "pulse-500")
    object Pulse400 : ColorResource(Color(0xFFFFA364), "pulse-400")

    // Legacy Midnight ramp retained for compatibility
    object Midnight950 : ColorResource(Color(0xFF04030C), "midnight-950")
    object Midnight900 : ColorResource(Color(0xFF070617), "midnight-900")
    object Midnight800 : ColorResource(Color(0xFF0B0B22), "midnight-800")
    object Midnight700 : ColorResource(Color(0xFF111030), "midnight-700")
    object Midnight600 : ColorResource(Color(0xFF19183F), "midnight-600")
    object Midnight900_A_80 :
        ColorResource(Color(0xFF070617).copy(alpha = 0.82f), "midnight-900-a-80")

    // Legacy Mist ramp retained for compatibility
    object Mist50 : ColorResource(Color(0xFFF9F7FF), "mist-50")
    object Mist100 : ColorResource(Color(0xFFEFE9FF), "mist-100")
    object Mist200 : ColorResource(Color(0xFFDFD6FF), "mist-200")

    // Legacy Graphite ramp retained for compatibility
    object Graphite800 : ColorResource(Color(0xFF21232B), "graphite-800")
    object Graphite700 : ColorResource(Color(0xFF27283A), "graphite-700")
    object Graphite600 : ColorResource(Color(0xFF343651), "graphite-600")
    object Graphite500 : ColorResource(Color(0xFF4A4C68), "graphite-500")
    object Graphite400 : ColorResource(Color(0xFF626482), "graphite-400")
    object Graphite300 : ColorResource(Color(0xFF8688A8), "graphite-300")
    object Graphite200 : ColorResource(Color(0xFFB2B4CB), "graphite-200")
    object Graphite100 : ColorResource(Color(0xFFE0E1EF), "graphite-100")

    // Legacy palettes retained for compatibility
    //  Green - Primary Brand Color
    object Green700 : ColorResource(Color(0xFF46A302), "green-700")
    object Green600 : ColorResource(Color(0xFF50B003), "green-600")
    object Green500 : ColorResource(Color(0xFF58CC02), "green-500")
    object Green400 : ColorResource(Color(0xFF73E005), "green-400")
    object Green300 : ColorResource(Color(0xFF8EF308), "green-300")
    object Green200 : ColorResource(Color(0xFFB0FF66), "green-200")
    object Green100 : ColorResource(Color(0xFFD7FFB8), "green-100")

    //  Blue/Cyan - Secondary
    object Cyan500 : ColorResource(Color(0xFF1CB0F6), "cyan-500")
    object Cyan400 : ColorResource(Color(0xFF4FC3F7), "cyan-400")
    object Cyan300 : ColorResource(Color(0xFF88D5FA), "cyan-300")

    //  Navy/Dark Blue - Backgrounds
    object NavyGray900 : ColorResource(Color(0xFF1A1E26), "navy-900")

    object Navy900 : ColorResource(Color(0xFF0A1628), "navy-900")
    object Navy800 : ColorResource(Color(0xFF131F33), "navy-800")
    object Navy700 : ColorResource(Color(0xFF1C2938), "navy-700")
    object Navy600 : ColorResource(Color(0xFF2E3F54), "navy-600")
    object Navy900_A_85 : ColorResource(Color(0xFF0A1628).copy(alpha = 0.85f), "navy-900-a-85")

    //  Red - Error/Warning
    object Red600 : ColorResource(Color(0xFFE2445C), "red-600")
    object Red500 : ColorResource(Color(0xFFFF5C7C), "red-500")
    object Red400 : ColorResource(Color(0xFFFF8BA0), "red-400")

    //  Yellow/Gold - Accents
    object Gold600 : ColorResource(Color(0xFFE5A000), "gold-600")
    object Gold500 : ColorResource(Color(0xFFFFC800), "gold-500")
    object Gold400 : ColorResource(Color(0xFFFFD633), "gold-400")

    //  Orange - Secondary Accent
    object Orange500 : ColorResource(Color(0xFFFF9600), "orange-500")
    object Orange400 : ColorResource(Color(0xFFFFA533), "orange-400")

    // Grays
    object Gray700 : ColorResource(Color(0xFF3C3C3C), "gray-700")
    object Gray600 : ColorResource(Color(0xFF555555), "gray-600")
    object Gray500 : ColorResource(Color(0xFF777777), "gray-500")
    object Gray400 : ColorResource(Color(0xFF999999), "gray-400")
    object Gray300 : ColorResource(Color(0xFFBBBBBB), "gray-300")
    object Gray200 : ColorResource(Color(0xFFDDDDDD), "gray-200")
    object Gray100 : ColorResource(Color(0xFFF0F0F0), "gray-100")

    // Keep legacy colors for backwards compatibility
    object Black900 : ColorResource(Color(0xFF000000), "black-900")
    object Black900_A_70 :
        ColorResource(Color(0xFF000000).copy(alpha = 0.7f), "black-900-a-70")
    object Black900_A_30 :
        ColorResource(Color(0xFF000000).copy(alpha = 0.3f), "black-900-a-30")
    object Black500 : ColorResource(Color(0xFFAAAAAA), "black-500")

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
    ColorResource.Lumen700,
    ColorResource.Lumen600,
    ColorResource.Lumen500,
    ColorResource.Lumen400,
    ColorResource.Lumen300,
    ColorResource.Catalyst600,
    ColorResource.Catalyst500,
    ColorResource.Catalyst400,
    ColorResource.Catalyst300,
    ColorResource.Obsidian950,
    ColorResource.Obsidian900,
    ColorResource.Obsidian850,
    ColorResource.Obsidian800,
    ColorResource.Obsidian700,
    ColorResource.Obsidian900_A_70,
    ColorResource.Obsidian900_A_80,
    ColorResource.Slate600,
    ColorResource.Slate500,
    ColorResource.Slate400,
    ColorResource.Slate300,
    ColorResource.Slate200,
    ColorResource.Slate100,
    ColorResource.Frost50,
    ColorResource.Frost100,
    ColorResource.Frost200,
    ColorResource.Frost300,
    ColorResource.Frost400,
    ColorResource.Frost500,
    ColorResource.Aurora700,
    ColorResource.Aurora600,
    ColorResource.Aurora500,
    ColorResource.Aurora400,
    ColorResource.Aurora300,
    ColorResource.Aurora200,
    ColorResource.Aurora100,
    ColorResource.Pulse600,
    ColorResource.Pulse500,
    ColorResource.Pulse400,
    ColorResource.Midnight950,
    ColorResource.Midnight900,
    ColorResource.Midnight800,
    ColorResource.Midnight700,
    ColorResource.Midnight600,
    ColorResource.Midnight900_A_80,
    ColorResource.Mist50,
    ColorResource.Mist100,
    ColorResource.Mist200,
    ColorResource.Graphite800,
    ColorResource.Graphite700,
    ColorResource.Graphite600,
    ColorResource.Graphite500,
    ColorResource.Graphite400,
    ColorResource.Graphite300,
    ColorResource.Graphite200,
    ColorResource.Graphite100,
    ColorResource.Green700,
    ColorResource.Green600,
    ColorResource.Green500,
    ColorResource.Green400,
    ColorResource.Green300,
    ColorResource.Green200,
    ColorResource.Green100,
    ColorResource.Cyan500,
    ColorResource.Cyan400,
    ColorResource.Cyan300,
    ColorResource.Navy900,
    ColorResource.Navy800,
    ColorResource.Navy700,
    ColorResource.Navy600,
    ColorResource.Red600,
    ColorResource.Red500,
    ColorResource.Red400,
    ColorResource.Gold600,
    ColorResource.Gold500,
    ColorResource.Gold400,
    ColorResource.Orange500,
    ColorResource.Orange400,
    ColorResource.Gray700,
    ColorResource.Gray600,
    ColorResource.Gray500,
    ColorResource.Gray400,
    ColorResource.Gray300,
    ColorResource.Gray200,
    ColorResource.Gray100,
    ColorResource.White900,
    ColorResource.Ice500
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