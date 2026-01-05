package com.dangerfield.goodtimes

import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@Inject
class SplashCompletedCallback {
    private var callback: (() -> Unit)? = null

    fun register(onSplashCompleted: () -> Unit) {
        callback = onSplashCompleted
    }

    fun onSplashCompleted() {
        callback?.invoke()
    }
}
