package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.bounceClick
import com.dangerfield.libraries.ui.components.BadgedBox
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.button.ButtonSize
import com.dangerfield.libraries.ui.components.icon.CircleIcon
import com.dangerfield.libraries.ui.components.icon.Icon
import com.dangerfield.libraries.ui.components.icon.IconButton
import com.dangerfield.libraries.ui.components.icon.IconSize
import com.dangerfield.libraries.ui.components.icon.Icons
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.dashedBorder
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
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNewBillClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onPastTabClicked: (PastTabPreview) -> Unit = {},
    items: List<PastTabPreview>
) {

    Screen(
        modifier.padding(screenHorizontalInsets)
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = Dimension.D500),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onSettingsClicked,
                    icon = Icons.Settings(null)
                )

            }


            TipJarBanner(
                modifier = Modifier.fillMaxWidth(),
                onAction = {}
            )


            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        typography = AppTheme.typography.Display.D1000,
                        textAlign = TextAlign.Center,
                        text = "Did you know..."
                    )
                    VerticalSpacerD500()
                    Text(
                        typography = AppTheme.typography.Heading.H700,
                        textAlign = TextAlign.Center,
                        text = "People who cover the bill and organize the split are better than everyone else."
                    )
                }
            } else {
                LazyColumn {
                    items(items = items) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = Dimension.D100)
                                .bounceClick { onPastTabClicked(it) }
                                .background(AppTheme.colors.surfacePrimary, Radii.Card.shape)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = it.name ?: it.createdAt.toString(),
                                    typography = AppTheme.typography.Heading.H600
                                )
                                VerticalSpacerD500()
                                it.location?.let { location ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Location(null))
                                        HorizontalSpacerD200()
                                        Text(
                                            text = location,
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Person(null))
                                    HorizontalSpacerD200()
                                    Text(
                                        text = it.peopleCount.toString(),
                                        typography = AppTheme.typography.Heading.H600
                                    )
                                }

                            }

                            Spacer(Modifier.weight(1f))
                            Text("$${it.total.toString()}")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimension.D500),
                onClick = onNewBillClicked
            ) {
                Text("New Bill")
            }
        }
    }
}

@Composable
private fun TipJarBanner(
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    BadgedBox(
        badgeTranslation = DpOffset(x = -(Dimension.D800), y = Dimension.D300),
        contentRadius = Radii.Card,
        badge = {
            Row {
                CircleIcon(
                    padding = Dimension.D200,
                    iconSize = IconSize.Small,
                    onClick = {  },
                    icon = Icons.X("Close Banner")
                )
            }

        },
        content = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(Dimension.D100)
                    .dashedBorder(radius = Radii.Card)
                    .padding(Dimension.D800)
            ) {
                Text(
                    text = "Like the app?",
                    typography = AppTheme.typography.Display.D800
                )
                VerticalSpacerD500()
                Text(
                    text = "Pretty please give me money (a tip)",
                    typography = AppTheme.typography.Body.B500.Italic
                )
                VerticalSpacerD500()
                Row {
                    Button(
                        size = ButtonSize.ExtraSmall,
                        onClick = {}
                    ) {
                        Text("Leave a tip")
                    }
                }
            }
        }
    )
}

data class PastTabPreview(
    val name: String? = null,
    val createdAt: Instant,
    val total: Double = 0.0,
    val peopleCount: Int,
    val location: String? = null
)

@Preview
@Composable
fun HomeScreenPreview() {
    PreviewContent {
        HomeScreen(
            items = listOf(
                PastTabPreview(
                    name = null,
                    total = 123.45,
                    peopleCount = 2,
                    createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                ),
                PastTabPreview(
                    name = null,
                    location = "New York, NY",
                    total = 123.45,
                    peopleCount = 2,
                    createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                ),
                PastTabPreview(
                    name = null,
                    total = 123.45,
                    peopleCount = 2,
                    createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                ),
                PastTabPreview(
                    name = null,
                    total = 123.45,
                    peopleCount = 2,
                    createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                ),
            )
        )
    }
}

@Preview
@Composable
fun HomeScreenPreviewEmpty() {
    PreviewContent {
        HomeScreen(
            items = emptyList()
        )
    }
}