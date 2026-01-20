package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface SessionDao {

    // ========== Observe ==========

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAllSessions(): Flow<List<SessionEntity>>

    // ========== Get ==========

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int

    @Query("SELECT * FROM sessions WHERE mood IS NOT NULL ORDER BY startedAt DESC")
    suspend fun getSessionsWithMood(): List<SessionEntity>

    /** Get recent sessions with mood for computing mood trend (last N sessions with mood set) */
    @Query("SELECT * FROM sessions WHERE mood IS NOT NULL ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentSessionsWithMood(limit: Int = 5): List<SessionEntity>

    /** Observe recent moods for UI display */
    @Query("SELECT * FROM sessions WHERE mood IS NOT NULL ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentSessionsWithMood(limit: Int = 5): Flow<List<SessionEntity>>

    /** Count sessions with specific mood */
    @Query("SELECT COUNT(*) FROM sessions WHERE mood = :mood")
    suspend fun countSessionsWithMood(mood: String): Int

    /** Get consecutive sessions with GOOD or GREAT mood (for easter egg) */
    @Query("SELECT COUNT(*) FROM (SELECT * FROM sessions WHERE mood IS NOT NULL ORDER BY startedAt DESC LIMIT :limit) WHERE mood IN ('GOOD', 'GREAT')")
    suspend fun countRecentPositiveMoods(limit: Int = 5): Int

    // ========== Insert ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    // ========== Update ==========

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE id = :sessionId")
    suspend fun endSession(sessionId: String, endedAt: Instant)

    @Query("UPDATE sessions SET mood = :mood, moodDismissed = 0 WHERE id = :sessionId")
    suspend fun setMood(sessionId: String, mood: String)

    @Query("UPDATE sessions SET moodDismissed = 1 WHERE id = :sessionId")
    suspend fun dismissMood(sessionId: String)

    @Query("UPDATE sessions SET tasksCompleted = tasksCompleted + 1 WHERE id = :sessionId")
    suspend fun incrementTasksCompleted(sessionId: String)

    @Query("UPDATE sessions SET tasksSkipped = tasksSkipped + 1 WHERE id = :sessionId")
    suspend fun incrementTasksSkipped(sessionId: String)

    // ========== Delete ==========

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}
