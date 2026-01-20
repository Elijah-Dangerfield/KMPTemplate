package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Represents a user session - a contiguous period of app usage.
 *
 * A new session is created when:
 * - App is opened fresh (no previous session)
 * - App is foregrounded after 10+ minutes in background
 * - App was force-killed and reopened
 *
 * Sessions track mood (optional), task completions, and link to previous session
 * for detecting session rollover.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,

    /** When this session started */
    val startedAt: Instant,

    /** When this session ended (null if still active) */
    val endedAt: Instant? = null,

    /** User's reported mood for this session (null if not set or dismissed) */
    val mood: String? = null, // Mood enum name: GREAT, GOOD, OKAY, LOW, BAD, COMPLICATED

    /** True if user explicitly dismissed the mood banner without selecting */
    val moodDismissed: Boolean = false,

    /** Number of tasks completed during this session */
    val tasksCompleted: Int = 0,

    /** Number of tasks skipped during this session */
    val tasksSkipped: Int = 0,

    /** ID of the previous session, for detecting session rollover */
    val previousSessionId: String? = null,
)
