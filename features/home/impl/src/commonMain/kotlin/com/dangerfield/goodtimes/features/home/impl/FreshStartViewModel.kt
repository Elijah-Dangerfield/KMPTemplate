package com.dangerfield.goodtimes.features.home.impl

import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import me.tatarka.inject.annotations.Inject

@Inject
class FreshStartViewModel(
    private val appCache: AppCache,
) : SEAViewModel<FreshStartState, FreshStartEvent, FreshStartAction>(
    initialStateArg = FreshStartState()
) {

    init {
        takeAction(FreshStartAction.Load)
    }

    override suspend fun handleAction(action: FreshStartAction) {
        when (action) {
            FreshStartAction.Load -> {
                val visitCount = appCache.get().getVisitCount("freshStartDialogOpens")
                action.updateState { it.copy(visitCount = visitCount) }
            }
            FreshStartAction.Confirm -> {
                // TODO: Implement fresh start logic
                // - delete all cache
                // - drop all tables
                // - navigate to onboarding popping back stack
                appCache.update { it.copy(freshStartsCompleted = it.freshStartsCompleted + 1) }
                sendEvent(FreshStartEvent.PerformFreshStart)
            }
        }
    }
}

data class FreshStartState(
    val visitCount: Int = 1,
) {
    val title: String = ScreenCopy.getFreshStartTitle(visitCount)
    val description: String = ScreenCopy.getFreshStartDescription(visitCount)
}

sealed interface FreshStartEvent {
    data object PerformFreshStart : FreshStartEvent
}

sealed interface FreshStartAction {
    data object Load : FreshStartAction
    data object Confirm : FreshStartAction
}
