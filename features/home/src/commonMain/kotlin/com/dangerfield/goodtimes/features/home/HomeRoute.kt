package com.dangerfield.goodtimes.features.home

import com.dangerfield.goodtimes.libraries.navigation.Route
import com.dangerfield.goodtimes.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

@Serializable
class HomeRoute : Route()

@Serializable
data class SettingsRoute(
    val visitCount: Int = 1,
) : TrackableRoute("settingsVisits")

@Serializable
class AboutYouRoute : TrackableRoute("aboutYouScreenOpens")

@Serializable
class AboutMeRoute : TrackableRoute("aboutMeScreenOpens")

@Serializable
class FreshStartDialogRoute : TrackableRoute("freshStartDialogOpens")

@Serializable
class FeedbackRoute : TrackableRoute("feedbackScreenOpens")

@Serializable
data class MoodRoute(
    val dismissCount: Int,
    val sessionNumber: Int,
    val isFirstEverMoodPrompt: Boolean = false,
    val fromSettings: Boolean = false,
) : Route()

@Serializable
data class UselessButtonDialogRoute(
    val clickCount: Int,
) : Route()

@Serializable
class SecretOptionDialogRoute : Route()

@Serializable
data class PersistenceUnlockedDialogRoute(
    val visitCount: Int = 1,
) : Route()

@Serializable
class QAMenuRoute : Route()
