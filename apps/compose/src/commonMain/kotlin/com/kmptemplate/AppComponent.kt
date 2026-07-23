package com.kmptemplate

import com.kmptemplate.libraries.core.AppState
import com.kmptemplate.libraries.core.AutoInit
import com.kmptemplate.libraries.identity.auth.AuthRepository
import com.kmptemplate.libraries.navigation.DeepLinkBridge
import com.kmptemplate.libraries.navigation.impl.DelegatingRouter
import com.kmptemplate.libraries.kmptemplate.Telemetry
import com.kmptemplate.libraries.navigation.FeatureEntryPoint
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock

@ContributesTo(AppScope::class)
@SingleIn(AppScope::class)
interface AppComponent {
    val featureEntryPoints: Set<FeatureEntryPoint>
    val appViewModel: AppViewModel  // Singleton, shared between MainActivity and App
    val delegatingRouter: DelegatingRouter
    val telemetry: Telemetry
    val shakeHandler: ShakeHandler
    val deepLinkBridge: DeepLinkBridge

    /**
     * Production app-wide state (offline banner etc.). Backed by
     * AppStateImpl — platform connectivity combined with witnessed
     * request reachability.
     */
    val appState: AppState

    /** Auth surface for App.kt's deep-link OAuth completion + routing collectors. */
    val authRepository: AuthRepository
    
    /**
     * Singletons that need to construct at app boot rather than lazily
     * on first injection. Anvil populates this set via the
     * `@ContributesBinding(... AutoInit::class, multibinding = true)`
     * annotation on each implementer — see [AutoInit] for the
     * contract and when to opt in.
     *
     * The set is resolved once on first composition in `App.kt` (and in
     * `Application.onCreate` on Android); the act of resolving forces
     * every contributor to construct, which runs each implementer's
     * `init {}` block. That's how
     * [com.kmptemplate.libraries.kmptemplate.impl.AppEventDispatcher]
     * registers its lifecycle listener at boot.
     */
    val autoInits: Set<AutoInit>

    @Provides
    fun provideClock(): Clock = Clock.System

}
