package com.dangerfield.merizo.libraries.navigation.impl

import com.dangerfield.merizo.libraries.core.Catching
import com.dangerfield.merizo.libraries.core.failure
import com.dangerfield.merizo.libraries.navigation.WebLinkLauncher
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class JvmWebLinkLauncher @Inject constructor() : WebLinkLauncher {

    override fun open(url: String): Catching<Unit> = failure(
        UnsupportedOperationException("Opening web links is not supported on JVM target: $url")
    )
}
