package com.dangerfield.goodtimes.features.onboarding

import com.dangerfield.goodtimes.libraries.navigation.AnimationType
import com.dangerfield.goodtimes.libraries.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingDeclinedDialogRoute(
    val timesDeclined: Int
) : Route(
    enter = AnimationType.SlideUp,
    exit = AnimationType.SlideDown,
    popExit = AnimationType.SlideDown,
)
