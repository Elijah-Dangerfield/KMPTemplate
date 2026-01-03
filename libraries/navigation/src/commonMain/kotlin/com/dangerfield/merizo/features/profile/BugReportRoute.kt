package com.dangerfield.merizo.features.profile

import com.dangerfield.merizo.libraries.navigation.NavigableWhileBlocked
import com.dangerfield.merizo.libraries.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class BugReportRoute(
	val logId: String? = null,
	val errorCode: Int? = null,
	val contextMessage: String? = null,
) : Route(), NavigableWhileBlocked
