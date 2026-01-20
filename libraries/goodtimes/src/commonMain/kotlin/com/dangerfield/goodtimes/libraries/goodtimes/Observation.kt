package com.dangerfield.goodtimes.libraries.goodtimes

/**
 * An observation is something meaningful we've noticed about the user.
 * 
 * These are surfaced in the "About You" screen to create moments of 
 * recognition - "oh, this app actually sees me."
 * 
 * Observations use warm, noticing language rather than clinical tracking:
 * - "I've noticed you..." rather than "You have a tendency to..."
 * - "It seems like..." rather than "Data shows..."
 * - "I wonder if..." rather than "Your metrics indicate..."
 * 
 * @property id Unique identifier for the observation
 * @property message The human-friendly observation text
 * @property category Category for grouping observations
 * @property priority Higher priority observations appear first (1-10)
 */
data class Observation(
    val id: String,
    val message: String,
    val category: ObservationCategory,
    val priority: Int = 5,
)

/**
 * Categories for organizing observations in the About You screen.
 */
enum class ObservationCategory {
    /** Who you are (name, identity) */
    IDENTITY,
    
    /** When you use the app (time patterns) */
    TIMING,
    
    /** How you engage with tasks */
    ENGAGEMENT,
    
    /** Your communication style */
    EXPRESSION,
    
    /** Things that make you you */
    PERSONALITY,
}
