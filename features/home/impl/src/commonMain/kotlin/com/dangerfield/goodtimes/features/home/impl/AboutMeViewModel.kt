package com.dangerfield.goodtimes.features.home.impl

import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import me.tatarka.inject.annotations.Inject

@Inject
class AboutMeViewModel(
    private val appCache: AppCache,
) : SEAViewModel<AboutMeState, Unit, AboutMeAction>(
    initialStateArg = AboutMeState()
) {

    init {
        takeAction(AboutMeAction.Load)
    }

    override suspend fun handleAction(action: AboutMeAction) {
        when (action) {
            AboutMeAction.Load -> {
                val visitCount = appCache.get().getVisitCount("aboutMeScreenOpens")
                action.updateState { it.copy(visitCount = visitCount) }
            }
        }
    }
}

data class AboutMeState(
    val visitCount: Int = 1,
) {
    // TODO: Make this dynamic based on visit count and app personality
    val title: String = ScreenCopy.getAboutMeTitle(visitCount)
    val content: String = ScreenCopy.getAboutMeContent(visitCount)
}

sealed interface AboutMeAction {
    data object Load : AboutMeAction
}
