package com.dangerfield.goodtimes

import com.dangerfield.goodtimes.features.home.HomeRoute
import com.dangerfield.goodtimes.features.onboarding.OnboardingRoute
import com.dangerfield.goodtimes.libraries.navigation.NavigationOptions
import com.dangerfield.goodtimes.libraries.navigation.Router
import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@Inject
class AppViewModel(
    private val router: Router,
    private val appCache: AppCache,
    private val splashCompletedCallback: SplashCompletedCallback,
) : SEAViewModel<Unit, Unit, Action>(Unit) {

    private val hasUserOnboarded = CompletableDeferred<Boolean>()

    init {
        splashCompletedCallback.register { takeAction(Action.SplashFinished) }
        takeAction(Action.Load)
    }

    override suspend fun handleAction(action: Action) {
        when (action) {
            Action.Load -> { hasUserOnboarded.complete(appCache.get().hasUserOnboarded) }
            Action.SplashFinished -> {
                val hasUserOnboarded = hasUserOnboarded.await()

                if (hasUserOnboarded) {
                    router.navigate(
                        route = HomeRoute(),
                        options = NavigationOptions(
                            clearBackStack = true,
                            launchSingleTop = true
                        )
                    )
                } else {
                    router.navigate(
                        route = OnboardingRoute(),
                        options = NavigationOptions(
                            clearBackStack = true,
                            launchSingleTop = true
                        )
                    )
                }
            }
        }
    }

    private suspend fun Action.load() {

    }
}


sealed class Action {
    object Load : Action()
    object SplashFinished: Action()
}