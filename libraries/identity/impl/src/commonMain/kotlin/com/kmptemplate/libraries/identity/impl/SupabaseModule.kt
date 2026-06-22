package com.kmptemplate.libraries.identity.impl

import com.kmptemplate.libraries.core.SupabaseInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Provides the app-scoped [SupabaseClient], reading the project URL + anon key
 * from [SupabaseInfo] (build config — set `supabase.projectId` / `supabase.anonKey`
 * in local.properties, or the env vars; see build-logic). The client is built
 * lazily on first injection, so an unconfigured template never constructs one.
 */
@ContributesTo(AppScope::class)
interface SupabaseModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = SupabaseInfo.url,
            supabaseKey = SupabaseInfo.anonKey,
        ) {
            install(Auth)
        }
}
