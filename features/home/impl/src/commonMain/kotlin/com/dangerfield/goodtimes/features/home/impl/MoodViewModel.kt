package com.dangerfield.goodtimes.features.home.impl

import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.SessionRepository
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

class MoodViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    @Assisted private val dismissCount: Int,
    @Assisted private val sessionNumber: Int,
    @Assisted private val isFirstEverMoodPrompt: Boolean,
    @Assisted private val fromSettings: Boolean,
) : SEAViewModel<MoodState, MoodEvent, MoodAction>(
    initialStateArg = MoodState(
        dismissCount = dismissCount,
        sessionNumber = sessionNumber,
        isFirstEverMoodPrompt = isFirstEverMoodPrompt,
        fromSettings = fromSettings,
    )
) {

    override suspend fun handleAction(action: MoodAction) {
        when (action) {
            is MoodAction.SelectMood -> action.handle()
            is MoodAction.Dismiss -> action.handle()
            is MoodAction.DontAskAgain -> action.handle()
            is MoodAction.Close -> sendEvent(MoodEvent.Close)
        }
    }

    private suspend fun MoodAction.SelectMood.handle() {
        sessionRepository.setMood(mood)
        val response = getMoodResponseText(mood)
        updateState { it.copy(selectedMood = mood, responseText = response) }
    }

    private suspend fun MoodAction.Dismiss.handle() {
        sessionRepository.dismissMood()
        sendEvent(MoodEvent.Close)
    }

    private suspend fun MoodAction.DontAskAgain.handle() {
        sessionRepository.disableMoodBannerPermanently()
        sendEvent(MoodEvent.Close)
    }
}

data class MoodState(
    val dismissCount: Int,
    val sessionNumber: Int,
    val isFirstEverMoodPrompt: Boolean = false,
    val fromSettings: Boolean = false,
    val selectedMood: Mood? = null,
    val responseText: String? = null,
)

sealed class MoodEvent {
    data object Close : MoodEvent()
}

sealed class MoodAction {
    data class SelectMood(val mood: Mood) : MoodAction()
    data object Dismiss : MoodAction()
    data object DontAskAgain : MoodAction()
    data object Close : MoodAction()
}
