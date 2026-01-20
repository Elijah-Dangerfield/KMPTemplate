package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user WHERE id = 'user' LIMIT 1")
    fun observeUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE id = 'user' LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE user SET name = :name, name_updated_at = :updatedAt WHERE id = 'user'")
    suspend fun updateName(name: String?, updatedAt: Long)
    
    @Query("UPDATE user SET name = :name, name_set_at = :setAt, name_updated_at = :setAt WHERE id = 'user'")
    suspend fun setNameFirstTime(name: String?, setAt: Long)

    @Query("UPDATE user SET current_task_id = :taskId WHERE id = 'user'")
    suspend fun setCurrentTaskId(taskId: String?)

    @Query("UPDATE user SET sessions_count = sessions_count + 1, last_session_at = :timestamp WHERE id = 'user'")
    suspend fun incrementSessionCount(timestamp: Long)

    @Query("UPDATE user SET tasks_completed = tasks_completed + 1 WHERE id = 'user'")
    suspend fun incrementTasksCompleted()

    @Query("UPDATE user SET tasks_skipped = tasks_skipped + 1 WHERE id = 'user'")
    suspend fun incrementTasksSkipped()

    @Query("UPDATE user SET app_open_count = app_open_count + 1 WHERE id = 'user'")
    suspend fun incrementAppOpenCount()

    @Query("UPDATE user SET settings_open_count = settings_open_count + 1 WHERE id = 'user'")
    suspend fun incrementSettingsOpenCount()

    @Query("UPDATE user SET about_open_count = about_open_count + 1 WHERE id = 'user'")
    suspend fun incrementAboutOpenCount()

    @Query("UPDATE user SET late_night_session_count = late_night_session_count + 1 WHERE id = 'user'")
    suspend fun incrementLateNightSessionCount()
    
    @Query("UPDATE user SET morning_session_count = morning_session_count + 1 WHERE id = 'user'")
    suspend fun incrementMorningSessionCount()
    
    @Query("UPDATE user SET midday_session_count = midday_session_count + 1 WHERE id = 'user'")
    suspend fun incrementMiddaySessionCount()

    @Query("UPDATE user SET routing_effects_json = :json WHERE id = 'user'")
    suspend fun setRoutingEffects(json: String?)

    @Query("DELETE FROM user")
    suspend fun deleteAll()
}
