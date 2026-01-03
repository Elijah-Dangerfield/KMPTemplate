package com.dangerfield.merizo

import com.dangerfield.merizo.libraries.navigation.impl.DelegatingRouter
import com.dangerfield.merizo.libraries.merizo.Telemetry
import com.dangerfield.merizo.libraries.navigation.FeatureEntryPoint
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock

@ContributesTo(AppScope::class)
@SingleIn(AppScope::class)
interface AppComponent {
    val featureEntryPoints: Set<FeatureEntryPoint>
    val appViewModel: () -> AppViewModel
    val delegatingRouter: DelegatingRouter
    val telemetry: Telemetry

    @Provides
    fun provideClock(): Clock = Clock.System

}
