package com.kmptemplate.libraries.identity.impl

import com.kmptemplate.libraries.identity.AuthRepository
import com.kmptemplate.libraries.identity.AuthState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Supabase-backed [AuthRepository]. Anonymous-first: `signInAnonymously` mints a
 * real Supabase user (`is_anonymous = true`) that can later be claimed by linking
 * an email/OAuth identity to the same id — progress is preserved.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class SupabaseAuthRepository(
    private val supabaseClient: () -> SupabaseClient,
) : AuthRepository {

    override val authState: Flow<AuthState>
        get() = supabaseClient().auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthState.Authenticated
                is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                else -> AuthState.Loading
            }
        }

    override suspend fun signInAnonymously() {
        supabaseClient().auth.signInAnonymously()
    }

    override suspend fun signOut() {
        supabaseClient().auth.signOut()
    }

    override fun currentUserId(): String? =
        supabaseClient().auth.currentUserOrNull()?.id
}
