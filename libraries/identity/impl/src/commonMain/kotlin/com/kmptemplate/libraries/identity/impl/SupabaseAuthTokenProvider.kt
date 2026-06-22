package com.kmptemplate.libraries.identity.impl

import com.kmptemplate.libraries.core.SupabaseInfo
import com.kmptemplate.libraries.networking.AuthTokenProvider
import com.kmptemplate.libraries.networking.NoOpAuthTokenProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Feeds the Supabase access token to the network layer's bearer auth, replacing
 * [NoOpAuthTokenProvider]. Until Supabase is configured (blank anon key) it
 * returns null, so the template still runs (requests go out unauthenticated)
 * before you wire your project.
 *
 * Injects `() -> SupabaseClient` rather than the client directly so the client is
 * built lazily, only once a token is actually requested.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [NoOpAuthTokenProvider::class])
@Inject
class SupabaseAuthTokenProvider(
    private val supabaseClient: () -> SupabaseClient,
) : AuthTokenProvider {

    private val configured: Boolean get() = SupabaseInfo.anonKey.isNotBlank()

    override suspend fun getAccessToken(): String? {
        if (!configured) return null
        val auth = supabaseClient().auth
        auth.awaitInitialization()
        return auth.currentAccessTokenOrNull()
    }

    override suspend fun refreshAccessToken(): String? {
        if (!configured) return null
        return try {
            val auth = supabaseClient().auth
            auth.refreshCurrentSession()
            auth.currentAccessTokenOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
