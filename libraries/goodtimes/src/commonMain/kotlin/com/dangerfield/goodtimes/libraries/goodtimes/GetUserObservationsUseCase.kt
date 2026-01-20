package com.dangerfield.goodtimes.libraries.goodtimes

/**
 * Gets meaningful observations about the user for the About You screen.
 * 
 * This use case:
 * - Analyzes user behavioral data to find meaningful patterns
 * - Applies progressive disclosure (don't overwhelm new users)
 * - Generates warm, noticing language for each observation
 * - Prioritizes observations that feel insightful, not creepy
 * 
 * Rules for observation disclosure:
 * - New users (<=3 sessions): Only show 1-2 gentle observations
 * - Established users (4-10 sessions): Show 3-5 observations  
 * - Veterans (10+ sessions): Show all applicable observations
 */
interface GetUserObservationsUseCase {
    /**
     * Get current observations about the user.
     * 
     * @return List of observations, sorted by priority (highest first)
     */
    suspend operator fun invoke(): List<Observation>
}
