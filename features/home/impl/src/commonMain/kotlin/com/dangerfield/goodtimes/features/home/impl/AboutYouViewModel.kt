package com.dangerfield.goodtimes.features.home.impl

import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.GetUserObservationsUseCase
import com.dangerfield.goodtimes.libraries.goodtimes.Observation
import com.dangerfield.goodtimes.libraries.goodtimes.UserCache
import me.tatarka.inject.annotations.Inject

@Inject
class AboutYouViewModel(
    private val appCache: AppCache,
    private val userCache: UserCache,
    private val getUserObservations: GetUserObservationsUseCase,
) : SEAViewModel<AboutYouState, Unit, AboutYouAction>(
    initialStateArg = AboutYouState()
) {

    init {
        takeAction(AboutYouAction.Load)
    }

    override suspend fun handleAction(action: AboutYouAction) {
        when (action) {
            AboutYouAction.Load -> {
                val visitCount = appCache.get().getVisitCount("aboutYouScreenOpens")
                val userData = userCache.get()
                val observations = getUserObservations()
                val savedName = userData.name ?: ""
                
                action.updateState {
                    it.copy(
                        visitCount = visitCount,
                        savedName = savedName,
                        name = savedName,
                        observations = observations,
                        isLoading = false,
                    ) 
                }
            }
            is AboutYouAction.NameChanged -> {
                action.updateState { it.copy(name = action.name) }
            }
            AboutYouAction.SaveName -> {
                val currentName = state.name.trim()
                userCache.update { it.copy(name = currentName.ifEmpty { null }) }
                // Reload observations to get updated name observation
                val observations = getUserObservations()
                action.updateState { 
                    it.copy(
                        savedName = currentName,
                        observations = observations
                    ) 
                }
            }
        }
    }
}

data class AboutYouState(
    val visitCount: Int = 1,
    val savedName: String = "",  // The persisted name
    val name: String = "",       // The name being edited (may differ while typing)
    val observations: List<Observation> = emptyList(),
    val isLoading: Boolean = true,
) {
    val title: String = ScreenCopy.getAboutYouTitle(visitCount)
    val hasName: Boolean = savedName.isNotBlank()
    val hasObservations: Boolean = observations.isNotEmpty()
}

sealed interface AboutYouAction {
    data object Load : AboutYouAction
    data class NameChanged(val name: String) : AboutYouAction
    data object SaveName : AboutYouAction
}
