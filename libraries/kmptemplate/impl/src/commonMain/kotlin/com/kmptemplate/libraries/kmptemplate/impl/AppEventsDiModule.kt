package com.kmptemplate.libraries.kmptemplate.impl

import com.kmptemplate.libraries.kmptemplate.AppEventBus
import com.kmptemplate.libraries.kmptemplate.AppEvents
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(AppScope::class)
interface AppEventsDiModule {

    @Provides
    @SingleIn(AppScope::class)
    fun provideAppEvents(bus: AppEventBus): AppEvents = AppEvents(bus)
}
