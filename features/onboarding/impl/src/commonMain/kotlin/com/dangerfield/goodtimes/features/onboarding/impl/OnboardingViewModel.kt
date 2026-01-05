package com.dangerfield.goodtimes.features.onboarding.impl

import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import me.tatarka.inject.annotations.Inject

class OnboardingViewModel @Inject constructor(
    private val appCache: AppCache
) : SEAViewModel<State, Event, Action>(initialStateArg = State()) {

    init {
        takeAction(Action.Load)
    }

    override suspend fun handleAction(action: Action) {
        when (action) {
            is Action.Load -> action.handle()
            is Action.NextPage -> action.handle()
            is Action.SelectYes -> action.handle()
            is Action.SelectNo -> action.handle()
            is Action.ConfirmOnboarding -> action.handle()
        }
    }

    private suspend fun Action.Load.handle() {
        val appData = appCache.get()
        updateState {
            it.copy(
                isLoading = false,
                timesDeclined = appData.numberOfTimesNoChecked
            )
        }
    }

    private suspend fun Action.NextPage.handle() {
        val currentPage = state.currentPage
        if (currentPage < OnboardingPage.entries.lastIndex) {
            updateState {
                it.copy(currentPage = currentPage + 1)
            }
        }
    }

    private suspend fun Action.SelectYes.handle() {
        updateState {
            it.copy(selection = OnboardingSelection.YES)
        }
    }

    private suspend fun Action.SelectNo.handle() {
        // Increment decline count and show dialog immediately
        val newDeclineCount = state.timesDeclined + 1
        appCache.update { it.copy(numberOfTimesNoChecked = newDeclineCount) }
        updateState { it.copy(timesDeclined = newDeclineCount) }
        sendEvent(Event.NavigateToDeclinedDialog(timesDeclined = newDeclineCount))
    }

    private suspend fun Action.ConfirmOnboarding.handle() {
        // Only called when YES is selected
        if (state.selection == OnboardingSelection.YES) {
            appCache.update { it.copy(hasUserOnboarded = true, numberOfTimesNoChecked = 0) }
            sendEvent(Event.NavigateToHome)
        }
    }
}

data class State(
    val isLoading: Boolean = true,
    val currentPage: Int = 0,
    val selection: OnboardingSelection? = null,
    val timesDeclined: Int = 0
) {
    val isOnFinalPage: Boolean get() = currentPage == OnboardingPage.entries.lastIndex
    val canProceed: Boolean get() = !isOnFinalPage || selection == OnboardingSelection.YES
}

enum class OnboardingPage {
    INTRO,
    WHAT_I_KNOW,
    UNDERSTANDING_YOU,
    PAGES,
    PRIVACY,
    CONSENT
}

enum class OnboardingSelection {
    YES, NO
}

sealed class Event {
    data object NavigateToHome : Event()
    data class NavigateToDeclinedDialog(val timesDeclined: Int) : Event()
}

sealed class Action {
    data object Load : Action()
    data object NextPage : Action()
    data object SelectYes : Action()
    data object SelectNo : Action()
    data object ConfirmOnboarding : Action()
}