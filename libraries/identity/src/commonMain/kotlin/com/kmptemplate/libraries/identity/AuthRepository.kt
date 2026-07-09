package com.kmptemplate.libraries.identity

import kotlinx.coroutines.flow.Flow

/**
 * Client-side authentication. The :impl is backed by Supabase, and binding a
 * [com.kmptemplate.libraries.networking.AuthTokenProvider] alongside it feeds the
 * access token to the network layer automatically.
 *
 * App/auth-feature code injects this to drive sign-in; the network layer never
 * depends on it (it only sees `AuthTokenProvider`), keeping the dependency one-way.
 */
interface AuthRepository {
    /** Emits the current auth state; collect to react to sign-in / sign-out. */
    val authState: Flow<AuthState>

    /** Create an anonymous guest session (a real Supabase user with `is_anonymous = true`). */
    suspend fun signInAnonymously()

    /** End the current session. */
    suspend fun signOut()

    /** The current user's id (the JWT `sub`), or null when signed out. */
    fun currentUserId(): String?
}

enum class AuthState { Loading, Authenticated, Unauthenticated }
