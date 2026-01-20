package com.dangerfield.goodtimes.libraries.goodtimes

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/*
Thoughts for cleanup:
do we need mood dismissed here? Does it make sense?
 */
@Serializable
data class Session(
    val id: String,
    val sessionNumber: Int,
    val startedAt: Instant,
    val mood: Mood? = null,
    val moodDismissed: Boolean = false,
    val tasksCompleted: Int = 0,
    val tasksSkipped: Int = 0,
)
