package com.dangerfield.goodtimes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dangerfield.libraries.ui.PreviewAppState
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.SnackbarDuration
import com.dangerfield.libraries.ui.components.dialog.DialogHost
import com.dangerfield.libraries.ui.components.dialog.LocalDialogHostState
import com.dangerfield.libraries.ui.components.dialog.rememberDialogHostState
import com.dangerfield.libraries.ui.debug.RecompositionCounter
import com.dangerfield.libraries.ui.snackbar.PresenterSnackbarHost
import com.dangerfield.libraries.ui.snackbar.showDebugSnackBar
import com.dangerfield.libraries.ui.system.LocalAppState
import com.dangerfield.libraries.ui.system.LocalBuildInfo
import com.dangerfield.libraries.ui.system.LocalClock
import com.dangerfield.goodtimes.libraries.core.BuildInfo
import com.dangerfield.goodtimes.libraries.core.logging.KLog
import com.dangerfield.goodtimes.libraries.navigation.AnimationType
import com.dangerfield.goodtimes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.goodtimes.libraries.navigation.Route
import com.dangerfield.goodtimes.libraries.navigation.floatingwindow.FloatingWindowHost
import com.dangerfield.goodtimes.libraries.navigation.floatingwindow.FloatingWindowNavigator
import com.dangerfield.goodtimes.libraries.navigation.impl.DelegatingRouter
import com.dangerfield.goodtimes.libraries.navigation.serializableType
import com.dangerfield.goodtimes.libraries.navigation.toEnterTransition
import com.dangerfield.goodtimes.libraries.navigation.toExitTransition
import com.dangerfield.goodtimes.libraries.navigation.toRouteOrNull
import com.dangerfield.goodtimes.system.AppThemeProvider
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds

@Composable
fun App(appComponent: AppComponent) {
    val appViewModel: AppViewModel = viewModel { appComponent.appViewModel() }
    val state by appViewModel.stateFlow.collectAsStateWithLifecycle()
    val floatingWindowNavigator = remember { FloatingWindowNavigator() }
    val navController = rememberNavController(floatingWindowNavigator)
    val appRecomposeLogger = remember { KLog.withTag("AppRecompose") }
    val splashRoute = remember { SplashRoute() }
    val router = remember { appComponent.delegatingRouter }
    val dialogHostState = rememberDialogHostState()

    RecompositionCounter(
        tag = "App",
        logEvery = 1,
        rapidRecompositionThreshold = 6,
        rapidRecompositionWindow = 60.seconds,
        onRecompose = { count ->
            val message = if (count == 1L) {
                "App recomposed (this should be rare)"
            } else {
                "App recomposed $count times"
            }
            appRecomposeLogger.w { message }
        },
        onRapidRecomposition = { info ->
            appRecomposeLogger.e {
                "Rapid recompositions: ${info.countInWindow} in ${info.windowMillis}ms (total=${info.totalCount})"
            }
            showDebugSnackBar(
                title = "Performance hiccup",
                message = "App recomposed ${info.countInWindow}× in ${info.windowMillis}ms.",
                duration = SnackbarDuration.Long,
                withDismissAction = true,
            )
        }
    )

    CompositionLocalProvider(
        // TODO give real app state
        LocalAppState provides PreviewAppState,
        LocalClock provides appComponent.provideClock(),
        LocalBuildInfo provides BuildInfo,
        LocalDialogHostState provides dialogHostState
    ) {
        AppThemeProvider {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavigation(
                    navController = navController,
                    floatingWindowNavigator = floatingWindowNavigator,
                    featureEntryPoints = appComponent.featureEntryPoints,
                    startDestination = splashRoute,
                    router = router,
                )

                DialogHost(
                    modifier = Modifier.matchParentSize(),
                    hostState = dialogHostState
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(
    navController: NavHostController,
    floatingWindowNavigator: FloatingWindowNavigator,
    featureEntryPoints: Set<FeatureEntryPoint>,
    startDestination: Any,
    router: DelegatingRouter,
) {

    Screen(
        snackbarHost = {
            PresenterSnackbarHost()
        },
        content = {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                //To make this more readable consider Screens A and B
                enterTransition = {
                    // A -> B
                    // How should we animate the B screen?
                    // Enter animation should match B's Enter
                    val targetRoute = targetState.toRouteOrNull<Route>()
                    val (animationType, reason) = when {
                        targetRoute != null -> targetRoute.enter to "Using target route enter animation"
                        else -> AnimationType.None to "Target destination is not a Route; default to none"
                    }

                    animationType.toEnterTransition()
                },
                popEnterTransition = {
                    // Popping from B back to A
                    // How should we animate the A screen?
                    // Enter animation should match initials pop EXIT transition
                    // AKA if B slides out, A should slide IN
                    val initialRoute = initialState.toRouteOrNull<Route>()
                    val targetRoute = targetState.toRouteOrNull<Route>()
                    val (animationType, reason) = when {
                        initialRoute != null -> initialRoute.popExit.opposite() to "Mirroring initial popExit animation"
                        targetRoute != null -> targetRoute.enter to "Fallback to target route enter animation"
                        else -> AnimationType.None to "No route metadata; default to none"
                    }

                    animationType.toEnterTransition()
                },
                exitTransition = {
                    // A -> B
                    // Initial: A | Target B
                    // How should we animate the A screen
                    // Exit animation should match A's Exit
                    val initialRoute = initialState.toRouteOrNull<Route>()
                    val (animationType, reason) = when {
                        initialRoute != null -> initialRoute.exit to "Using initial route exit animation"
                        else -> AnimationType.None to "Initial destination is not a Route; default to none"
                    }

                    animationType.toExitTransition()
                },
                popExitTransition = {
                    // Popping from B back to A
                    // Initial: B | Target A
                    // How should we animate the B screen
                    // Exit animation should match B's pope Exit
                    val initialRoute = initialState.toRouteOrNull<Route>()

                    val (animationType, reason) = when {
                        initialRoute != null -> initialRoute.popExit to "Using initial route popExit animation"
                        else -> AnimationType.None to "Initial destination is not a Route; default to none"
                    }

                    animationType.toExitTransition()
                },
                typeMap = mapOf(
                    typeOf<AnimationType>() to serializableType<AnimationType>()
                )
            ) {
                featureEntryPoints.forEach { entryPoint ->
                    with(entryPoint) {
                        buildNavGraph(router)
                    }
                }
            }

            FloatingWindowHost(floatingWindowNavigator)

            router.Bind(navController)
        },
    )
}