package com.kmptemplate.server.di

import com.kmptemplate.server.config.SupabaseConfig
import com.kmptemplate.server.db.Database
import com.kmptemplate.server.domain.ExampleSource
import com.kmptemplate.server.domain.ModerationRepository
import com.kmptemplate.server.domain.PlayerReportRepository
import com.kmptemplate.server.domain.ProfileRepository
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Root DI component for the server process. Mirrors the client's `AppComponent`
 * — anvil aggregates every `@ContributesBinding(ServerScope::class)` on the
 * classpath and KSP generates the merged implementation at compile time.
 *
 * Boot once at startup with the runtime [Database]:
 *
 * ```
 * val component = ServerComponent::class.create(database)
 * routing { meRoutes(component.profileRepository) }
 * ```
 *
 * Add a new service: annotate its impl with `@ContributesBinding(ServerScope::class)`,
 * expose its interface as an `abstract val` here. Done.
 *
 * Runtime inputs (the [Database], later parsed config) come in as constructor
 * params exposed with `@get:Provides` so impls can take them as ordinary
 * constructor parameters without a separate factory.
 */
@MergeComponent(ServerScope::class)
@SingleIn(ServerScope::class)
@OptIn(ExperimentalTime::class)
abstract class ServerComponent(
    @get:Provides val database: Database,
    /**
     * Null when Supabase isn't configured — consumers degrade (the admin
     * client answers NotConfigured) instead of failing at construction.
     */
    @get:Provides val supabaseConfig: SupabaseConfig? = null,
) {
    abstract val exampleSource: ExampleSource
    abstract val profileRepository: ProfileRepository
    abstract val moderationRepository: ModerationRepository
    abstract val playerReportRepository: PlayerReportRepository

    /**
     * Wall-clock source. Singleton so every component sees the same "now".
     * Tests pass a fixed clock to the class under test directly.
     */
    @Provides
    fun provideClock(): Clock = Clock.System
}
