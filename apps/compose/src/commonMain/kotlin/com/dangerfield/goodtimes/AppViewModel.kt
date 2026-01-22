package com.dangerfield.goodtimes

import com.dangerfield.goodtimes.features.home.HomeRoute
import com.dangerfield.goodtimes.features.onboarding.OnboardingRoute
import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * App-level ViewModel that handles initial routing decisions.
 * 
 * Emits [AppEvent.NavigateTo] once it determines where the user should go.
 * The SplashScreen observes this and handles navigation timing:
 * - On iOS: waits for typewriter animation, then navigates
 * - On Android: navigates immediately (native splash already showed)
 * 
 * Scoped as singleton so Android's splash screen condition and Compose share
 * the same instance.
 */
@SingleIn(AppScope::class)
@Inject
class AppViewModel(
    private val appCache: AppCache,
) : SEAViewModel<AppState, AppEvent, AppAction>(AppState()) {

    private val _isReady = MutableStateFlow(false)
    
    /** 
     * Exposed to Android's splash screen API for keepOnScreenCondition.
     * True once we've determined where to navigate.
     */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        takeAction(AppAction.DetermineStartDestination)
    }

    override suspend fun handleAction(action: AppAction) {
        when (action) {
            AppAction.DetermineStartDestination -> {
                val hasUserOnboarded = appCache.get().hasUserOnboarded
                
                val destination: Route = if (hasUserOnboarded) {
                    HomeRoute()
                } else {
                    OnboardingRoute()
                }
                
                action.updateState { it.copy(startDestination = destination) }
                _isReady.value = true
                sendEvent(AppEvent.NavigateTo(destination))
            }
        }
    }
}

data class AppState(
    val startDestination: Route? = null,
)

sealed class AppEvent {
    /** Navigate to the given route, clearing the back stack */
    data class NavigateTo(val route: Route) : AppEvent()
}

sealed class AppAction {
    data object DetermineStartDestination : AppAction()
}