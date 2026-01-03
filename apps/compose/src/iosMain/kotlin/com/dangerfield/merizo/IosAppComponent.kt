package com.dangerfield.merizo

import com.dangerfield.libraries.ui.nativeviews.NativeViewFactory
import com.dangerfield.merizo.libraries.merizo.PermissionManager
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class IosAppComponent(
    private val permissionManager: PermissionManager,
    val nativeViewFactory: NativeViewFactory
) : AppComponent {

    @Provides
    fun providePermissionManager(): PermissionManager = permissionManager

}


@MergeComponent.CreateComponent
expect fun create(
    permissionManager: PermissionManager,
    nativeViewFactory: NativeViewFactory
): IosAppComponent
