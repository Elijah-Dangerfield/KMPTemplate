package com.kmptemplate.features.profile

import com.kmptemplate.libraries.navigation.NavigableWhileBlocked
import com.kmptemplate.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

@Serializable
data class BugReportRoute(
	val logId: String? = null,
	val errorCode: Int? = null,
	val contextMessage: String? = null,
) : TrackableRoute("bugReportScreenOpens"), NavigableWhileBlocked
