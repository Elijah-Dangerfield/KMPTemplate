package com.dangerfield.goodtimes.features.home.impl

import androidx.lifecycle.viewModelScope
import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.SessionRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import kotlin.random.Random

private const val USELESS_BUTTON_MAX_CLICKS = 10
private const val SECRET_OPTION_CHANCE = 0.05 // 5% chance
private const val PERSISTENCE_GOAL = 100

class SettingsViewModel @Inject constructor(
    private val appCache: AppCache,
    private val sessionRepository: SessionRepository,
) : SEAViewModel<SettingsState, SettingsEvent, SettingsAction>(initialStateArg = SettingsState()) {

    init {
        takeAction(SettingsAction.Load)
        
        combine(
            sessionRepository.moodBannerDisabled,
            sessionRepository.moodBannerToggleCount,
            sessionRepository.currentSession,
            sessionRepository.hasEverAnsweredMood,
            appCache.updates,
        ) { disabled, toggleCount, session, hasEverAnsweredMood, appData ->
            SettingsObservedData(
                moodBannerDisabled = disabled,
                moodBannerToggleCount = toggleCount,
                canAnswerMood = session != null && session.mood == null,
                isFirstEverMoodPrompt = !hasEverAnsweredMood,
                uselessButtonClicks = appData.uselessButtonClicks,
                secretOptionDismissed = appData.secretOptionDismissed,
                madeWithLoveClicks = appData.madeWithLoveClicks,
                persistenceUnlocked = appData.madeWithLovePersistenceUnlocked,
                aboutYouVisits = appData.getVisitCount("aboutYouScreenOpens"),
                aboutMeVisits = appData.getVisitCount("aboutMeScreenOpens"),
                freshStartVisits = appData.getVisitCount("freshStartDialogOpens"),
            )
        }
            .onEach { data ->
                takeAction(SettingsAction.ObservedSettingsChanged(
                    moodBannerDisabled = data.moodBannerDisabled, 
                    moodBannerToggleCount = data.moodBannerToggleCount,
                    canAnswerMood = data.canAnswerMood,
                    isFirstEverMoodPrompt = data.isFirstEverMoodPrompt,
                    uselessButtonClicks = data.uselessButtonClicks,
                    secretOptionDismissed = data.secretOptionDismissed,
                    madeWithLoveClicks = data.madeWithLoveClicks,
                    persistenceUnlocked = data.persistenceUnlocked,
                    aboutYouVisits = data.aboutYouVisits,
                    aboutMeVisits = data.aboutMeVisits,
                    freshStartVisits = data.freshStartVisits,
                ))
            }
            .launchIn(viewModelScope)
    }

    override suspend fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.Load -> action.handle()
            is SettingsAction.FreshStart -> action.handle()
            is SettingsAction.OpenFreshStartDialog -> sendEvent(SettingsEvent.NavigateToFreshStartDialog)
            is SettingsAction.OpenAboutYou -> sendEvent(SettingsEvent.NavigateToAboutYou)
            is SettingsAction.OpenAboutMe -> sendEvent(SettingsEvent.NavigateToAboutMe)
            is SettingsAction.OpenBugReport -> sendEvent(SettingsEvent.NavigateToBugReport)
            is SettingsAction.OpenFeedback -> sendEvent(SettingsEvent.NavigateToFeedback)
            is SettingsAction.ToggleMoodBanner -> action.handle()
            is SettingsAction.ClickUselessButton -> action.handle()
            is SettingsAction.ObservedSettingsChanged -> action.handle()
            is SettingsAction.OpenMoodPrompt -> sendEvent(SettingsEvent.NavigateToMoodPrompt(
                isFirstEverMoodPrompt = stateFlow.value.isFirstEverMoodPrompt
            ))
            is SettingsAction.OpenSecretOption -> action.handle()
            is SettingsAction.ClickMadeWithLove -> action.handle()
            is SettingsAction.OpenQAMenu -> sendEvent(SettingsEvent.NavigateToQAMenu)
        }
    }

    private suspend fun SettingsAction.Load.handle() {
        // Check if we should show the secret option this visit
        val appData = appCache.get()
        val showSecretOption = !appData.secretOptionDismissed && Random.nextFloat() < SECRET_OPTION_CHANCE
        
        if (showSecretOption && !appData.secretOptionShown) {
            appCache.update { it.copy(secretOptionShown = true) }
        }
        
        updateState { 
            it.copy(
                isLoading = false,
                showSecretOption = showSecretOption
            ) 
        }
    }

    private suspend fun SettingsAction.FreshStart.handle() {
        appCache.update {
            it.copy(
                hasUserOnboarded = false,
                onboardingNoClicks = 0
            )
        }
        sendEvent(SettingsEvent.NavigateToOnboarding)
    }
    
    private suspend fun SettingsAction.ToggleMoodBanner.handle() {
        if (enabled) {
            sessionRepository.enableMoodBanner()
        } else {
            sessionRepository.disableMoodBannerPermanently()
        }
    }
    
    private suspend fun SettingsAction.ClickUselessButton.handle() {
        val newCount = appCache.update { 
            it.copy(uselessButtonClicks = it.uselessButtonClicks + 1) 
        }.uselessButtonClicks
        
        sendEvent(SettingsEvent.NavigateToUselessButtonDialog(clickCount = newCount))
    }
    
    private suspend fun SettingsAction.OpenSecretOption.handle() {
        // Mark as dismissed so it never shows again
        appCache.update { it.copy(secretOptionDismissed = true) }
        updateState { it.copy(showSecretOption = false) }
        sendEvent(SettingsEvent.NavigateToSecretOption)
    }
    
    private suspend fun SettingsAction.ClickMadeWithLove.handle() {
        val appData = appCache.get()
        val newCount = appData.madeWithLoveClicks + 1
        
        if (appData.madeWithLovePersistenceUnlocked) {
            // Already unlocked - just open the dialog again with incremented count
            val newDialogOpens = appData.persistenceDialogOpens + 1
            appCache.update { it.copy(persistenceDialogOpens = newDialogOpens) }
            sendEvent(SettingsEvent.NavigateToPersistenceUnlocked(visitCount = newDialogOpens))
        } else if (newCount >= PERSISTENCE_GOAL) {
            // They did it for the first time!
            appCache.update { 
                it.copy(
                    madeWithLoveClicks = newCount,
                    madeWithLovePersistenceUnlocked = true,
                    persistenceDialogOpens = 1
                ) 
            }
            updateState { it.copy(madeWithLoveHint = null) }
            sendEvent(SettingsEvent.NavigateToPersistenceUnlocked(visitCount = 1))
        } else {
            appCache.update { it.copy(madeWithLoveClicks = newCount) }
            
            // Check if we should show a teasing message
            val message = MadeWithLoveCopy.getMessage(newCount)
            if (message != null) {
                updateState { it.copy(madeWithLoveHint = message) }
            }
        }
    }
    
    private suspend fun SettingsAction.ObservedSettingsChanged.handle() {
        updateState {
            it.copy(
                moodBannerDisabled = moodBannerDisabled,
                moodBannerToggleCount = moodBannerToggleCount,
                canAnswerMood = canAnswerMood,
                isFirstEverMoodPrompt = isFirstEverMoodPrompt,
                showUselessButton = uselessButtonClicks == USELESS_BUTTON_MAX_CLICKS,
                madeWithLoveClicks = madeWithLoveClicks,
                persistenceUnlocked = persistenceUnlocked,
                aboutYouVisits = aboutYouVisits,
                aboutMeVisits = aboutMeVisits,
                freshStartVisits = freshStartVisits,
            )
        }
    }
}

private data class SettingsObservedData(
    val moodBannerDisabled: Boolean,
    val moodBannerToggleCount: Int,
    val canAnswerMood: Boolean,
    val isFirstEverMoodPrompt: Boolean,
    val uselessButtonClicks: Int,
    val secretOptionDismissed: Boolean,
    val madeWithLoveClicks: Int,
    val persistenceUnlocked: Boolean,
    val aboutYouVisits: Int,
    val aboutMeVisits: Int,
    val freshStartVisits: Int,
)

data class SettingsState(
    val isLoading: Boolean = true,
    val moodBannerDisabled: Boolean = false,
    val moodBannerToggleCount: Int = 0,
    val canAnswerMood: Boolean = false,
    val isFirstEverMoodPrompt: Boolean = false,
    val showUselessButton: Boolean = false,
    val showSecretOption: Boolean = false,
    val madeWithLoveClicks: Int = 0,
    val madeWithLoveHint: String? = null,
    val persistenceUnlocked: Boolean = false,
    val aboutYouVisits: Int = 0,
    val aboutMeVisits: Int = 0,
    val freshStartVisits: Int = 0,
)

sealed class SettingsEvent {
    data object NavigateToOnboarding : SettingsEvent()
    data object NavigateToAboutYou : SettingsEvent()
    data object NavigateToAboutMe : SettingsEvent()
    data object NavigateToFreshStartDialog : SettingsEvent()
    data object NavigateToBugReport : SettingsEvent()
    data object NavigateToFeedback : SettingsEvent()
    data object NavigateToQAMenu : SettingsEvent()
    data class NavigateToMoodPrompt(val isFirstEverMoodPrompt: Boolean) : SettingsEvent()
    data class NavigateToUselessButtonDialog(val clickCount: Int) : SettingsEvent()
    data object NavigateToSecretOption : SettingsEvent()
    data class NavigateToPersistenceUnlocked(val visitCount: Int) : SettingsEvent()
}

sealed class SettingsAction {
    data object Load : SettingsAction()
    data object FreshStart : SettingsAction()
    data object OpenFreshStartDialog : SettingsAction()
    data object OpenAboutYou : SettingsAction()
    data object OpenAboutMe : SettingsAction()
    data object OpenBugReport : SettingsAction()
    data object OpenFeedback : SettingsAction()
    data object OpenQAMenu : SettingsAction()
    data object OpenMoodPrompt : SettingsAction()
    data object ClickUselessButton : SettingsAction()
    data object OpenSecretOption : SettingsAction()
    data object ClickMadeWithLove : SettingsAction()
    data class ToggleMoodBanner(val enabled: Boolean) : SettingsAction()
    data class ObservedSettingsChanged(
        val moodBannerDisabled: Boolean, 
        val moodBannerToggleCount: Int,
        val canAnswerMood: Boolean,
        val isFirstEverMoodPrompt: Boolean,
        val uselessButtonClicks: Int,
        val secretOptionDismissed: Boolean,
        val madeWithLoveClicks: Int,
        val persistenceUnlocked: Boolean,
        val aboutYouVisits: Int,
        val aboutMeVisits: Int,
        val freshStartVisits: Int,
    ) : SettingsAction()
}
