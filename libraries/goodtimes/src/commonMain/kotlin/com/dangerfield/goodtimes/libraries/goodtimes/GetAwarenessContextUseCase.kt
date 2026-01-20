package com.dangerfield.goodtimes.libraries.goodtimes

/**
 * Builds an [AwarenessContext] representing everything the app knows about
 * the current moment, user, and history.
 * 
 * This is the single source of context for:
 * - Copy generation (making text feel alive)
 * - Task selection algorithm
 * - Mood prompt decisions
 * - Any adaptive behavior
 * 
 * Call sites should inject this use case and invoke it when they need
 * context-aware decisions.
 */
interface GetAwarenessContextUseCase {
    /**
     * Get awareness context for the current moment.
     * 
     * @param screenVisitCount How many times the user has visited this specific screen.
     *                         Pass 0 if not relevant to your use case.
     */
    suspend operator fun invoke(screenVisitCount: Int = 0): AwarenessContext
}
