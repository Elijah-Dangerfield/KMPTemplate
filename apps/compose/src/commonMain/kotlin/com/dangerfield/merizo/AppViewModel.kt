package com.dangerfield.merizo

import com.dangerfield.merizo.features.home.HomeRoute
import com.dangerfield.merizo.libraries.navigation.NavigationOptions
import com.dangerfield.merizo.libraries.navigation.Router
import com.dangerfield.merizo.libraries.flowroutines.SEAViewModel
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@Inject
class AppViewModel(
    private val router: Router,
) : SEAViewModel<Unit, Unit, Action>(Unit) {

    init {
        takeAction(Action.Load)
    }

    override suspend fun handleAction(action: Action) {
        when (action) {
            Action.Load -> {
                action.load()
            }
        }
    }

    private suspend fun Action.load() {
        delay(2.seconds)
        router.navigate(
            route = HomeRoute(),
            options = NavigationOptions(
                clearBackStack = true,
                launchSingleTop = true
            )
        )
    }
}


sealed class Action {
    object Load : Action()
}