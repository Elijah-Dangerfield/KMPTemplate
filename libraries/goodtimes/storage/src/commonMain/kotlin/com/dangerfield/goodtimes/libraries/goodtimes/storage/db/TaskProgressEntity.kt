package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * Tracks user's progress on each task.
 */
@Entity(
    tableName = "task_progress",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("task_id")]
)
data class TaskProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "task_id")
    val taskId: String,

    val status: String, // TaskStatus enum name: LOCKED, AVAILABLE, CURRENT, COMPLETED, SKIPPED_PERMANENT, SKIPPED_RESCHEDULE

    val attempts: Int = 0,

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long?, // epoch millis

    @ColumnInfo(name = "completed_at")
    val completedAt: Long?, // epoch millis

    @ColumnInfo(name = "rescheduled_until")
    val rescheduledUntil: Long?, // epoch millis
)
