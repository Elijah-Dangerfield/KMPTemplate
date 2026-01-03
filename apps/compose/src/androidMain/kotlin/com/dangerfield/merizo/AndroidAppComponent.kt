package com.dangerfield.merizo

import android.content.Context
import com.dangerfield.libraries.ui.app.AndroidAppMetaDataRepository
import com.dangerfield.libraries.ui.app.AppMetaDataRepository
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AndroidAppComponent(
    private val context: Context
) : AppComponent {

    @Provides
    fun context() = context

    @Provides
    fun provideAppMetaDataRepository(): AppMetaDataRepository = AndroidAppMetaDataRepository(context)
}
