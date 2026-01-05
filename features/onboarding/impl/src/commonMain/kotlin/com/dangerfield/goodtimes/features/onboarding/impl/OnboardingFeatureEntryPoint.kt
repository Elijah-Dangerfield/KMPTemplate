package com.dangerfield.goodtimes.features.onboarding.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.dangerfield.goodtimes.features.home.HomeRoute
import com.dangerfield.goodtimes.features.onboarding.OnboardingDeclinedDialogRoute
import com.dangerfield.goodtimes.features.onboarding.OnboardingRoute
import com.dangerfield.goodtimes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.goodtimes.libraries.navigation.NavigationOptions
import com.dangerfield.goodtimes.libraries.navigation.Router
import com.dangerfield.goodtimes.libraries.navigation.dialog
import com.dangerfield.goodtimes.libraries.navigation.screen
import com.dangerfield.goodtimes.libraries.navigation.toRouteOrNull
import kotlinx.coroutines.flow.collectLatest
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class OnboardingFeatureEntryPoint(
    private val viewModelFactory: () -> OnboardingViewModel
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<OnboardingRoute> {
            val viewModel: OnboardingViewModel = viewModel { viewModelFactory() }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is Event.NavigateToHome -> {
                            router.navigate(
                                route = HomeRoute(),
                                options = NavigationOptions(
                                    clearBackStack = true,
                                    launchSingleTop = true
                                )
                            )
                        }
                        is Event.NavigateToDeclinedDialog -> {
                            router.navigate(
                                OnboardingDeclinedDialogRoute(timesDeclined = event.timesDeclined)
                            )
                        }
                    }
                }
            }

            OnboardingScreen(
                state = state,
                onNextClicked = { viewModel.takeAction(Action.NextPage) },
                onYesSelected = { viewModel.takeAction(Action.SelectYes) },
                onNoSelected = { viewModel.takeAction(Action.SelectNo) },
                onConfirmClicked = { viewModel.takeAction(Action.ConfirmOnboarding) }
            )
        }

        dialog<OnboardingDeclinedDialogRoute> { backStackEntry, dialogState ->
            val route = backStackEntry.toRouteOrNull<OnboardingDeclinedDialogRoute>()
            val timesDeclined = route?.timesDeclined ?: 1

            OnboardingDeclinedDialog(
                dialogState = dialogState,
                timesDeclined = timesDeclined,
                onDismiss = { router.goBack() }
            )
        }
    }
}