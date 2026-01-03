package com.dangerfield.merizo.libraries.merizo.impl

import android.content.Context
import com.dangerfield.merizo.libraries.merizo.PlatformApp
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidAppPicker(
    private val context: Context
) : AppPicker {

    override suspend fun pickApps(initialSelection: List<PlatformApp>): List<PlatformApp> {
        // TODO: Implement actual app picker UI for Android
        // For now, return empty list as a stub
        // Future: Show custom picker UI with PackageManager data
        return emptyList()
    }
}
