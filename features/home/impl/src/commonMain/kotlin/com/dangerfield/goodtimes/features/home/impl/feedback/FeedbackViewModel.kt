package com.dangerfield.goodtimes.features.home.impl.feedback

import com.dangerfield.goodtimes.features.home.impl.ScreenCopy
import com.dangerfield.goodtimes.libraries.core.eitherWay
import com.dangerfield.goodtimes.libraries.flowroutines.SEAViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.navigation.Router
import com.dangerfield.libraries.ui.snackbar.showSnackBar
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@Inject
class FeedbackViewModel(
    private val repository: FeedbackRepository,
    private val router: Router,
    private val appCache: AppCache,
) : SEAViewModel<FeedbackState, Unit, FeedbackAction>(
    initialStateArg = FeedbackState()
) {

    init {
        takeAction(FeedbackAction.Load)
    }

    override suspend fun handleAction(action: FeedbackAction) {
        when (action) {
            FeedbackAction.Load -> {
                val visitCount = appCache.get().getVisitCount("feedbackScreenOpens")
                action.updateState { it.copy(visitCount = visitCount) }
            }
            FeedbackAction.Back -> router.goBack()
            is FeedbackAction.MessageChanged -> action.updateMessage()
            FeedbackAction.Submit -> action.submitFeedback()
        }
    }

    private suspend fun FeedbackAction.MessageChanged.updateMessage() {
        val updated = value
        updateState { it.copy(message = updated, errorMessage = null) }
    }

    private suspend fun FeedbackAction.submitFeedback() {
        val current = state
        if (current.message.isBlank()) {
            updateState { it.copy(errorMessage = "Add a quick note before sending.") }
            return
        }
        updateState { it.copy(isSubmitting = true, errorMessage = null) }
        repository.submitFeedback(
            message = current.message.trim(),
            isBugReport = false
        ).eitherWay {
            appCache.update { it.copy(feedbacksGiven = it.feedbacksGiven + 1) }
            updateState { it.copy(isSubmitting = false) }
            showSnackBar(message = "Got it. Thank you for teaching me.", delayBy = 1.seconds)
            router.goBack()
        }
    }
}

data class FeedbackState(
    val message: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val visitCount: Int = 1,
) {
    val title: String
        get() = ScreenCopy.getFeedbackTitle(visitCount)

    val helperText: String
        get() = ScreenCopy.getFeedbackHelperText(visitCount)
}

sealed interface FeedbackAction {
    data object Load : FeedbackAction
    data object Back : FeedbackAction
    data class MessageChanged(val value: String) : FeedbackAction
    data object Submit : FeedbackAction
}
