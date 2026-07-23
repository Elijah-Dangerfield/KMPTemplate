package com.kmptemplate

import com.kmptemplate.libraries.identity.auth.AppleSignInCoordinator
import com.kmptemplate.libraries.identity.auth.SecureSessionStorage
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
    // The Swift `IOSAppleSignInCoordinator` (ASAuthorizationController flow),
    // passed in from `iOSApp.swift`. Android binds its own no-op via anvil.
    private val appleSignInCoordinator: AppleSignInCoordinator,
    // The Swift `IOSSecureSessionStorage` (Keychain-backed Supabase session
    // store), passed in from `iOSApp.swift`. Android binds
    // EncryptedSessionStorage via anvil.
    private val secureSessionStorage: SecureSessionStorage,
    val nativeViewFactory: NativeViewFactory
) : AppComponent {

    @Provides
    fun providePermissionManager(): PermissionManager = permissionManager

    @Provides
    fun provideReviewPrompter(): ReviewPrompter = reviewPrompter

    @Provides
    fun provideAppleSignInCoordinator(): AppleSignInCoordinator = appleSignInCoordinator

    @Provides
    fun provideSecureSessionStorage(): SecureSessionStorage = secureSessionStorage
}


@MergeComponent.CreateComponent
expect fun create(
    permissionManager: PermissionManager,
    reviewPrompter: ReviewPrompter,
    appleSignInCoordinator: AppleSignInCoordinator,
    secureSessionStorage: SecureSessionStorage,
    nativeViewFactory: NativeViewFactory
): IosAppComponent
