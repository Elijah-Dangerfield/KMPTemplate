package com.kmptemplate

import com.kmptemplate.libraries.kmptemplate.PermissionManager
import com.kmptemplate.libraries.kmptemplate.ReviewPrompter
import com.kmptemplate.libraries.ui.nativeviews.NativeViewFactory
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class IosAppComponent(
    private val permissionManager: PermissionManager,
    private val reviewPrompter: ReviewPrompter,
    val nativeViewFactory: NativeViewFactory
) : AppComponent {

    @Provides
    fun providePermissionManager(): PermissionManager = permissionManager

    @Provides
    fun provideReviewPrompter(): ReviewPrompter = reviewPrompter
}


@MergeComponent.CreateComponent
expect fun create(
    permissionManager: PermissionManager,
    reviewPrompter: ReviewPrompter,
    nativeViewFactory: NativeViewFactory
): IosAppComponent
