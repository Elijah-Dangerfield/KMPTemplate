package com.dangerfield.goodtimes.libraries.goodtimes

import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing and managing tasks.
 * 
 * On first launch, the repository seeds the database from bundled tasks.json.
 * Tasks are static definitions - user progress is tracked separately in TaskProgress.
 */
interface TaskRepository {
    
    /**
     * Initialize the task repository, seeding from JSON if database is empty.
     * This should be called once at app startup.
     */
    suspend fun initialize()
    
    /**
     * Observe all tasks in the database.
     */
    fun observeAllTasks(): Flow<List<Task>>
    
    /**
     * Get all tasks from the database.
     */
    suspend fun getAllTasks(): List<Task>
    
    /**
     * Get a task by its ID.
     */
    suspend fun getTask(id: String): Task?
    
    /**
     * Get the total count of tasks.
     */
    suspend fun getTaskCount(): Int
}
