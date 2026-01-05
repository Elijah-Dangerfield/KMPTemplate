package com.dangerfield.goodtimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavGraphBuilder
import com.dangerfield.goodtimes.libraries.navigation.AnimationType
import com.dangerfield.goodtimes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.goodtimes.libraries.navigation.Route
import com.dangerfield.goodtimes.libraries.navigation.Router
import com.dangerfield.goodtimes.libraries.navigation.screen
import com.dangerfield.goodtimes.libraries.navigation.serializableType
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.CircularProgressIndicator
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.libraries.ui.components.text.TypewriterTextEffect
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.ui.tooling.preview.Preview
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.typeOf

@Serializable
class SplashRoute : Route(
    enter = AnimationType.None,
    exit = AnimationType.None,
    popExit = AnimationType.None,
)

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class SplashScreenEntryPoint(
    private val splashCompletedCallback: SplashCompletedCallback,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<SplashRoute>(
            typeMap = mapOf(typeOf<AnimationType>() to serializableType<AnimationType>())
        ) {
            SplashScreen(
                onSplashAnimFinished = { splashCompletedCallback.onSplashCompleted() }
            )
        }
    }
}


@Composable
fun SplashScreen(
    onSplashAnimFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background.color),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        var shouldShowLoading by remember { mutableStateOf(false) }
        var isTypewriterComplete by remember { mutableStateOf(false) }

        // After typewriter completes, wait a moment then notify the ViewModel
        LaunchedEffect(isTypewriterComplete) {
            if (isTypewriterComplete) {
                delay(1000) // Let the text hang for a moment
                onSplashAnimFinished()
                delay(1000) // Give ViewModel time to navigate before showing loading
                shouldShowLoading = true
            }
        }

        if (shouldShowLoading) {
            CircularProgressIndicator()
        } else {
            TypewriterTextEffect(
                text = "The App of Good Times",
                minDelayInMillis = 50,
                maxDelayInMillis = 150,
                minCharacterChunk = 1,
                maxCharacterChunk = 2,
                onEffectCompleted = { isTypewriterComplete = true }
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = it,
                    typography = AppTheme.typography.Brand.B1300,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Preview
@Composable
private fun PreviewSplashScreen() {
    PreviewContent {
        SplashScreen(
            onSplashAnimFinished = {}
        )
    }
}