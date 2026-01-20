package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskResultsDao {

    @Query("SELECT * FROM task_results ORDER BY completed_at DESC")
    fun observeAllResults(): Flow<List<TaskResultEntity>>

    @Query("SELECT * FROM task_results ORDER BY completed_at DESC")
    suspend fun getAllResults(): List<TaskResultEntity>

    @Query("SELECT * FROM task_results WHERE task_id = :taskId ORDER BY completed_at DESC")
    suspend fun getResultsForTask(taskId: String): List<TaskResultEntity>

    @Query("SELECT * FROM task_results WHERE session_id = :sessionId ORDER BY completed_at DESC")
    suspend fun getResultsForSession(sessionId: String): List<TaskResultEntity>

    @Query("SELECT * FROM task_results ORDER BY completed_at DESC LIMIT :limit")
    suspend fun getRecentResults(limit: Int): List<TaskResultEntity>

    @Query("SELECT COUNT(*) FROM task_results WHERE task_id = :taskId")
    suspend fun getAttemptCount(taskId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TaskResultEntity)

    @Query("DELETE FROM task_results")
    suspend fun deleteAll()
}
