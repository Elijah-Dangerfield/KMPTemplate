package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records what the user actually did for a task.
 * Multiple results can exist per task (retries, "maybe later" reschedules).
 */
@Entity(
    tableName = "task_results",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("task_id"), Index("session_id")]
)
data class TaskResultEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String?,

    @ColumnInfo(name = "attempt_number")
    val attemptNumber: Int,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long, // epoch millis

    @ColumnInfo(name = "time_spent_ms")
    val timeSpentMs: Long,

    val outcome: String, // TaskOutcome enum name: COMPLETED, SKIPPED_PERMANENT, SKIPPED_RESCHEDULE

    @ColumnInfo(name = "response_type")
    val responseType: String?, // TaskResponse sealed class type

    @ColumnInfo(name = "response_json")
    val responseJson: String?, // JSON serialized response data

    @ColumnInfo(name = "signals_json")
    val signalsJson: String, // JSON list of Signal objects
)
