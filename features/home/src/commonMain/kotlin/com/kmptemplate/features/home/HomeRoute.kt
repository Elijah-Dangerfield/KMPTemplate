package com.kmptemplate.features.home

import com.kmptemplate.libraries.navigation.Route
import com.kmptemplate.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

@Serializable
class HomeRoute : Route()

@Serializable
data class SettingsRoute(
    val visitCount: Int = 1,
) : TrackableRoute("settingsVisits")

@Serializable
class FeedbackRoute : TrackableRoute("feedbackScreenOpens")
