package com.kmptemplate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kmptemplate.libraries.core.Catching
import com.kmptemplate.libraries.core.logOnFailure
import com.kmptemplate.libraries.core.BuildInfo
import com.kmptemplate.libraries.core.Platform
import com.kmptemplate.libraries.core.logging.KLog
import com.kmptemplate.libraries.navigation.AccessDeniedRoute
import com.kmptemplate.libraries.navigation.AnimationType
import com.kmptemplate.libraries.navigation.FeatureEntryPoint
import com.kmptemplate.libraries.navigation.NavigationOptions
import com.kmptemplate.libraries.navigation.Route
import com.kmptemplate.libraries.navigation.SessionExpiredRoute
import com.kmptemplate.libraries.navigation.floatingwindow.FloatingWindowHost
import com.kmptemplate.libraries.navigation.floatingwindow.FloatingWindowNavigator
import com.kmptemplate.libraries.navigation.impl.DelegatingRouter
import com.kmptemplate.libraries.navigation.serializableType
import com.kmptemplate.libraries.navigation.toEnterTransition
import com.kmptemplate.libraries.navigation.toExitTransition
import com.kmptemplate.libraries.navigation.toRouteOrNull
import com.kmptemplate.libraries.ui.components.Screen
import com.kmptemplate.libraries.ui.components.SnackbarDuration
import com.kmptemplate.libraries.ui.components.dialog.DialogHost
import com.kmptemplate.libraries.ui.components.dialog.LocalDialogHostState
import com.kmptemplate.libraries.ui.components.dialog.rememberDialogHostState
import com.kmptemplate.libraries.ui.debug.RecompositionCounter
import com.kmptemplate.libraries.ui.snackbar.PresenterSnackbarHost
import com.kmptemplate.libraries.ui.snackbar.showDebugSnackBar
import com.kmptemplate.libraries.ui.system.LocalAppState
import com.kmptemplate.libraries.ui.system.LocalBuildInfo
import com.kmptemplate.libraries.ui.system.LocalClock
import com.kmptemplate.system.AppThemeProvider
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds

@Composable
fun App(appComponent: AppComponent) {
    val appViewModel = appComponent.appViewModel
    val floatingWindowNavigator = remember { FloatingWindowNavigator() }
    val navController = rememberNavController(floatingWindowNavigator)
    val appRecomposeLogger = remember { KLog.withTag("AppRecompose") }
    val router = remember { appComponent.delegatingRouter }
    val dialogHostState = rememberDialogHostState()

    val shakeHandler = remember { appComponent.shakeHandler }
    val deepLinkBridge = remember { appComponent.deepLinkBridge }

    // Boot-warm every @AutoInit singleton. Resolving the Set forces each
    // contributor to construct, running their `init {}` blocks —
    // AppEventDispatcher attaches its lifecycle observer, connectivity
    // watchers arm, etc. Wrapped in `remember` so this resolves exactly once
    // per composition lifetime, even though App() recomposes. See [AutoInit]
    // for the contract. (Android also resolves this in Application.onCreate,
    // before the first Activity; resolving twice is a no-op.)
    remember { appComponent.autoInits }

    DisposableEffect(shakeHandler) {
        shakeHandler.start()
        onDispose {
            shakeHandler.stop()
        }
    }

    val authRepository = remember { appComponent.authRepository }

    LaunchedEffect(navController, deepLinkBridge) {
        deepLinkBridge.urls.collect { url ->
            // The browser OAuth return trip (`…://login-callback#...`) carries
            // a Supabase session in its fragment, not a navigable destination —
            // hand it to supabase-kt to import instead of the nav graph. Every
            // other link routes as before. The repo emits the new AuthState on
            // success, which the app's auth collectors react to.
            if (authRepository.isOAuthRedirect(url)) {
                Catching { authRepository.completeOAuthRedirect(url) }
                    .logOnFailure { "Failed to complete OAuth redirect" }
            } else {
                Catching {
                    val request = NavDeepLinkRequest.Builder.fromUri(NavUri(url)).build()
                    navController.handleDeepLink(request)
                }.logOnFailure { "Failed to handle deep link: $url" }
            }
        }
    }

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

    val appState = remember { appComponent.appState }

    CompositionLocalProvider(
        LocalAppState provides appState,
        LocalClock provides appComponent.provideClock(),
        LocalBuildInfo provides BuildInfo,
        LocalDialogHostState provides dialogHostState
    ) {
        AppThemeProvider {
            Box(modifier = Modifier.fillMaxSize()) {
                // Null until the async AppData read resolves — the platform
                // splash (keyed on appViewModel.isReady) covers the gap, so
                // nothing renders behind it prematurely.
                val startDestination by appViewModel.startDestination.collectAsState()
                startDestination?.let { route ->
                    AppNavigation(
                        navController = navController,
                        floatingWindowNavigator = floatingWindowNavigator,
                        featureEntryPoints = appComponent.featureEntryPoints,
                        startDestination = route,
                        router = router,
                    )
                }

                SplashGate()

                // The auth server rejected our session mid-run: push a blocking
                // SessionExpired screen (kept on top, stack intact) that owns
                // "sign in again" (claimed) vs "start fresh" (guest). An ambient
                // network event can't present this from a feature screen, so it
                // routes here. The screen picks its copy off wasAnonymous.
                LaunchedEffect(Unit) {
                    appViewModel.sessionExpired.collect { event ->
                        router.navigate(
                            SessionExpiredRoute(wasAnonymous = event.wasAnonymous),
                            NavigationOptions(launchSingleTop = true),
                        )
                    }
                }

                // Server returned the locked `403` access-denied envelope: push
                // the blocking AccessDenied screen. Same launchSingleTop pattern
                // as SessionExpired — a burst of denied calls collapses to one
                // screen on top. The screen keys title/body off `reason` and
                // surfaces the optional lift date + appeal link.
                LaunchedEffect(Unit) {
                    appViewModel.accessDenied.collect { denial ->
                        router.navigate(
                            AccessDeniedRoute(
                                reason = denial.reason,
                                until = denial.until,
                                appealUrl = denial.appealUrl,
                            ),
                            NavigationOptions(launchSingleTop = true),
                        )
                    }
                }

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
    startDestination: Route,
    router: DelegatingRouter,
) {
    // Remember the graph-builder lambda so recompositions of AppNavigation
    // hand NavHost the SAME builder instance. A fresh lambda each pass makes
    // NavHost treat the graph as changed and re-push the start destination
    // onto the back stack.
    val graph: NavGraphBuilder.() -> Unit = remember(featureEntryPoints, router) {
        {
            featureEntryPoints.forEach { entryPoint ->
                with(entryPoint) {
                    buildNavGraph(router)
                }
            }
        }
    }

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
                ),
                builder = graph
            )

            FloatingWindowHost(floatingWindowNavigator)

            router.Bind(navController)
        },
    )
}

/**
 * Isolates the splash-overlay's `hasShownSplash` state read into its own composable
 * so that flipping it cannot recompose `App` and, in particular, cannot cause
 * `AppNavigation` / `NavHost` to rebuild its graph — which was previously pushing
 * a second copy of the start destination onto the back stack.
 *
 * Only renders on iOS. Android uses the native splash API instead.
 */
@Composable
private fun SplashGate() {
    if (BuildInfo.platform != Platform.iOS) return
    var hasShownSplash by rememberSaveable { mutableStateOf(false) }
    if (!hasShownSplash) {
        SplashOverlay(onComplete = { hasShownSplash = true })
    }
}
