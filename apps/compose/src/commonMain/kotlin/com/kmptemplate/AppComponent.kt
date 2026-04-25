package com.kmptemplate

import com.kmptemplate.libraries.kmptemplate.impl.AppEventDispatcher
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
     * Eagerly initialized to start observing app lifecycle events.
     * This ensures sessions are created on foreground entry.
     */
    val appEventDispatcher: AppEventDispatcher

    @Provides
    fun provideClock(): Clock = Clock.System

}
