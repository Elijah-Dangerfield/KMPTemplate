package com.dangerfield.goodtimes.features.profile

import com.dangerfield.goodtimes.libraries.navigation.NavigableWhileBlocked
import com.dangerfield.goodtimes.libraries.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class BugReportRoute(
	val logId: String? = null,
	val errorCode: Int? = null,
	val contextMessage: String? = null,
) : Route(), NavigableWhileBlocked
