package com.dangerfield.goodtimes

import kotlinx.atomicfu.atomic
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@Inject
class SplashCompletedCallback {
    private var callback: (() -> Unit)? = null
    private val hasCompleted = atomic(false)

    fun register(onSplashCompleted: () -> Unit) {
        callback = onSplashCompleted
    }

    fun onSplashCompleted() {
        if (hasCompleted.compareAndSet(expect = false, update = true)) {
            callback?.invoke()
        }
    }
}
