package com.kmptemplate.server.domain

/**
 * Persistence for [Profile]. The interface lives in `domain/`, the Postgres impl
 * in `data/` (`PostgresProfileRepository`) — routes depend on this, never the
 * impl. Swapping the backing store is a one-line DI change.
 *
 * Implementations MUST run each method in a single transaction.
 */
interface ProfileRepository {

    /**
     * Returns the caller's profile, creating one with a default display name on
     * first contact. Idempotent: calling it twice returns the same row.
     */
    suspend fun findOrCreate(userId: UserId): Profile

    /**
     * Updates the display name. Returns an outcome the route maps to an HTTP
     * status (200 / 409 / 404) — the repository never speaks HTTP.
     */
    suspend fun updateDisplayName(userId: UserId, displayName: String): UpdateProfileOutcome
}

/**
 * Result of a write, pattern-matched by the route into an HTTP status. Modeling
 * outcomes as a sealed type (instead of throwing) keeps the route's `when`
 * exhaustive and the failure modes explicit.
 */
sealed interface UpdateProfileOutcome {
    data class Success(val profile: Profile) : UpdateProfileOutcome
    data object DisplayNameTaken : UpdateProfileOutcome
    data object NotFound : UpdateProfileOutcome
}
