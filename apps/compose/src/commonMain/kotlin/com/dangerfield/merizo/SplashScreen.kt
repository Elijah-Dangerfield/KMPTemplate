package com.dangerfield.merizo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.CircularProgressIndicator
import com.dangerfield.merizo.libraries.navigation.AnimationType
import com.dangerfield.merizo.libraries.navigation.FeatureEntryPoint
import com.dangerfield.merizo.libraries.navigation.Route
import com.dangerfield.merizo.libraries.navigation.Router
import com.dangerfield.merizo.libraries.navigation.screen
import com.dangerfield.merizo.libraries.navigation.serializableType
import com.dangerfield.merizo.system.AppTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import merizo.libraries.resources.generated.resources.Res
import merizo.libraries.resources.generated.resources.merizo_logo_1024
import org.jetbrains.compose.resources.painterResource
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
class SplashScreenEntryPoint : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<SplashRoute>(
            typeMap = mapOf(typeOf<AnimationType>() to serializableType<AnimationType>())
        ) {
            SplashScreen()
        }
    }
}


@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background.color),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        var shouldShowLoading by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(3000)
            shouldShowLoading = true
        }

        if (shouldShowLoading) {
            CircularProgressIndicator()
        } else {
            Image(
                modifier = Modifier.size(150.dp),
                painter = painterResource(Res.drawable.merizo_logo_1024),
                contentDescription = null
            )
        }
    }
}


@Preview
@Composable
private fun PreviewSplashScreen() {
    PreviewContent {
        SplashScreen()
    }
}