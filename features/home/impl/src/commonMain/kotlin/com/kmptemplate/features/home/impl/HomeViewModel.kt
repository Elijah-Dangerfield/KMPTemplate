package com.kmptemplate.features.home.impl

import com.kmptemplate.libraries.flowroutines.SEAViewModel
import com.kmptemplate.libraries.kmptemplate.UserRepository
import me.tatarka.inject.annotations.Inject

@Inject
class HomeViewModel(
    private val userRepository: UserRepository,
) : SEAViewModel<HomeState, HomeEvent, HomeAction>(
    initialStateArg = HomeState()
) {
    
    init {
        takeAction(HomeAction.Load)
    }
    
    override suspend fun handleAction(action: HomeAction) {
        when (action) {
            is HomeAction.Load -> action.loadUser()
            is HomeAction.Refresh -> action.loadUser()
        }
    }
    
    private suspend fun HomeAction.loadUser() {
        val user = userRepository.getUser()
        updateState { it.copy(userName = user?.name) }
    }
}

data class HomeState(
    val userName: String? = null,
)

sealed interface HomeEvent

sealed interface HomeAction {
    data object Load : HomeAction
    data object Refresh : HomeAction
}
