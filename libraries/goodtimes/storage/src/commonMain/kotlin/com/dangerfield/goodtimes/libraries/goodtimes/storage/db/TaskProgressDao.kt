package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskProgressDao {

    @Query("SELECT * FROM task_progress")
    fun observeAllProgress(): Flow<List<TaskProgressEntity>>

    @Query("SELECT * FROM task_progress")
    suspend fun getAllProgress(): List<TaskProgressEntity>

    @Query("SELECT * FROM task_progress WHERE task_id = :taskId LIMIT 1")
    suspend fun getProgress(taskId: String): TaskProgressEntity?

    @Query("SELECT * FROM task_progress WHERE status = :status")
    suspend fun getByStatus(status: String): List<TaskProgressEntity>

    @Query("SELECT COUNT(*) FROM task_progress WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progress: List<TaskProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: TaskProgressEntity)

    @Update
    suspend fun update(progress: TaskProgressEntity)

    @Query("UPDATE task_progress SET status = :status WHERE task_id = :taskId")
    suspend fun updateStatus(taskId: String, status: String)

    @Query("UPDATE task_progress SET status = :status, completed_at = :completedAt WHERE task_id = :taskId")
    suspend fun markCompleted(taskId: String, status: String, completedAt: Long)

    @Query("UPDATE task_progress SET status = :status, rescheduled_until = :rescheduledUntil, attempts = attempts + 1 WHERE task_id = :taskId")
    suspend fun markRescheduled(taskId: String, status: String, rescheduledUntil: Long)

    @Query("DELETE FROM task_progress")
    suspend fun deleteAll()
}
