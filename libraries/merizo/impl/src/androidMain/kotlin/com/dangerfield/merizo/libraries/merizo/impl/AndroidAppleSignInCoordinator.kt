package com.dangerfield.merizo.libraries.merizo.impl

import com.dangerfield.merizo.libraries.merizo.AppleSignInCoordinator
import com.dangerfield.merizo.libraries.merizo.AppleSignInCredential
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidAppleSignInCoordinator : AppleSignInCoordinator {
    override suspend fun requestCredential(): AppleSignInCredential {
        throw UnsupportedOperationException("Apple Sign In is available on iOS only")
    }
}