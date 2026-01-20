package com.dangerfield.goodtimes.libraries.goodtimes

/**
 * Use case for getting the next task to present to the user.
 * 
 * This is where the core task selection algorithm will live.
 * For now, it simply iterates through available tasks.
 * 
 * Future implementation will consider:
 * - User's personality scores
 * - Response style affinities  
 * - Current mood and mood trends
 * - Time-based conditions
 * - Task difficulty progression
 * - Social comfort level
 * - Previously completed/skipped tasks
 */
interface GetNextTaskUseCase {
    /**
     * Get the next task for the user.
     * Returns null if no tasks are available.
     */
    suspend operator fun invoke(): Task?
}
