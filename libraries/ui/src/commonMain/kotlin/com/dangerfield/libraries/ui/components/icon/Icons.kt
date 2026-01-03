package com.dangerfield.libraries.ui.components.icon

import BootstrapFillBarChart
import BootstrapOutlineBarChart
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.icon.icons.App_shortcut
import com.dangerfield.libraries.ui.components.icon.icons.HomeFilled
import com.dangerfield.libraries.ui.components.icon.icons.TipJarIcon
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.merizo.system.HorizontalSpacerD800
import com.dangerfield.libraries.ui.system.LocalBuildInfo
import com.dangerfield.merizo.system.VerticalSpacerD500
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Design‑system icon resource, similar to `ColorResource`/`TypographyResource`.
 *
 * Icons are always referenced through this type so callers cannot
 * reach directly for raw Material icons.
 */
@Immutable
data class IconResource(
    val imageVector: ImageVector,
    val contentDescription: String?,
    val identifier: String? = null,
)

enum class IconStroke {
    Thin,
    Regular,
    Thick,
}

enum class Icons(
    internal val default: ImageVector,
    internal val filled: ImageVector? = null,
    internal val outlined: ImageVector? = null ,
    internal val outlinedThin: ImageVector? = null ,
    internal val outlinedThick: ImageVector? = null ,
    internal val twoTone: ImageVector? = null,
) {

    Home(
        default = com.dangerfield.libraries.ui.components.icon.icons.Home,
        filled = HomeFilled,
        outlined = com.dangerfield.libraries.ui.components.icon.icons.Home,
    ),

    Time(
        default = androidx.compose.material.icons.Icons.Filled.AccessTime,
        filled = androidx.compose.material.icons.Icons.Filled.AccessTime,
        outlined = androidx.compose.material.icons.Icons.Outlined.AccessTime,
    ),

    Chart(
        default = BootstrapFillBarChart,
        filled = BootstrapFillBarChart,
        outlined = BootstrapOutlineBarChart,
    ),

    Tools(
        default = androidx.compose.material.icons.Icons.Default.Build,
    ),

    Pencil(
        default = androidx.compose.material.icons.Icons.Rounded.Edit,
    ),

    Refresh(
        default = androidx.compose.material.icons.Icons.Rounded.Refresh,
    ),

    Warning(
        default = androidx.compose.material.icons.Icons.Default.Warning,
    ),

    TipJar(
        default = TipJarIcon,
    ),

    Charity(
        default = androidx.compose.material.icons.Icons.Rounded.VolunteerActivism,
    ),

    Lock(
        default = androidx.compose.material.icons.Icons.Rounded.Lock,
    ),

    ThumbsUp(
        default = androidx.compose.material.icons.Icons.Rounded.ThumbUp,
    ),

    Question(
        default = androidx.compose.material.icons.Icons.Rounded.QuestionMark,
    ),

    Chat(
        default = androidx.compose.material.icons.Icons.Rounded.ChatBubble,
    ),

    Bug(
        default = androidx.compose.material.icons.Icons.Rounded.BugReport,
    ),

    ThumbsDown(
        default = androidx.compose.material.icons.Icons.Rounded.ThumbDown,
    ),


    AppShortCut(
        default = App_shortcut
    ),


    Plus(
        default = androidx.compose.material.icons.Icons.Default.Add,
    ),

    X(androidx.compose.material.icons.Icons.Rounded.Close),

    Check(androidx.compose.material.icons.Icons.Rounded.Check),

    SnowFlake(com.dangerfield.libraries.ui.components.icon.icons.SnowFlake),

    Person(
        androidx.compose.material.icons.Icons.Rounded.Person,
        androidx.compose.material.icons.Icons.Rounded.Person,
         androidx.compose.material.icons.Icons.Outlined.Person,
        ),

    Info(
        androidx.compose.material.icons.Icons.Outlined.Info,
        androidx.compose.material.icons.Icons.Rounded.Info,
        androidx.compose.material.icons.Icons.Outlined.Info
    ),

    Settings(androidx.compose.material.icons.Icons.Rounded.Settings),

    ChevronLeft(androidx.compose.material.icons.Icons.Rounded.ChevronLeft),

    ChevronRight(androidx.compose.material.icons.Icons.Rounded.ChevronRight),


    ArrowBack(
        default = androidx.compose.material.icons.Icons.AutoMirrored.Default.ArrowBack,
    )

    ;

    operator fun invoke(contentDescription: String?): IconResource {
        return IconResource(
            imageVector = default,
            contentDescription = contentDescription,
            identifier = name,
        )
    }

}

@Composable
fun Icons.Filled(contentDescription: String?): IconResource {
    return IconResource(
        imageVector = filled ?: run {
            if (LocalBuildInfo.current.isDebug) throw IllegalStateException("Missing filled icon for $name") else default
        },
        contentDescription = contentDescription,
    )
}

@Composable
fun Icons.Outlined(contentDescription: String?, stroke: IconStroke = IconStroke.Regular): IconResource {
    return IconResource(
        imageVector = when (stroke) {
            IconStroke.Thin -> outlinedThin
            IconStroke.Regular -> outlined
            IconStroke.Thick -> outlinedThick
        } ?: run {
            if (LocalBuildInfo.current.isDebug) throw IllegalStateException("Missing outline ${stroke.name} icon for $name") else (outlined ?: default)
        },

        contentDescription = contentDescription,
    )
}

@Composable
fun Icons.TwoTone(contentDescription: String?): IconResource {
    return IconResource(
        imageVector = twoTone ?: run {
            if (LocalBuildInfo.current.isDebug) throw IllegalStateException("Missing two tone icon for $name") else default
        },
        contentDescription = contentDescription,
    )
}
@Preview(heightDp = 1500, widthDp = 700)
@Composable
private fun IconPreviewGrid() {
    PreviewContent {
        Column {
            Icons.entries.forEach { icon ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.border(1.dp, Color.Gray).padding(vertical = 32.dp)
                ) {
                    @Composable
                    fun IconCell(
                        modifier: Modifier = Modifier,
                        label: String,
                        image: ImageVector?
                    ) {
                        Column(
                            modifier,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            if (image != null) {
                                androidx.compose.material3.Icon(
                                    imageVector = image,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(1.dp, Color.Gray)
                                ) {
                                    Text("?")
                                }
                            }
                            
                            VerticalSpacerD500()

                            Text(
                                text = label,
                                maxLines = 1
                            )
                        }
                    }

                    val default = icon.default
                    val filled = icon.filled
                    val outlined = icon.outlined
                    val twoTone = icon.twoTone

                    IconCell(modifier = Modifier.weight(1f),icon.name, default)
                    HorizontalSpacerD800()
                    IconCell(modifier = Modifier.weight(1f),"Filled", filled)
                    HorizontalSpacerD800()
                    IconCell(modifier = Modifier.weight(1f),"Outlined", outlined)
                    HorizontalSpacerD800()
                    IconCell(modifier = Modifier.weight(1f),"TwoTone", twoTone)
                }
            }
        }
    }
}




















