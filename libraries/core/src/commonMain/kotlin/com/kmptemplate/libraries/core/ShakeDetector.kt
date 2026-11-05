package com.kmptemplate.libraries.core

import kotlinx.coroutines.flow.Flow

/**
 * A stream of shakes: [ShakeRecognizer] over the accelerometer on Android,
 * UIKit's own shake gesture on iOS (`IosShakeDetector`). UIKit says nothing
 * about how hard, so an iOS shake is always [ShakeIntensity.NORMAL].
 *
 * [start] and [stop] are foreground-scoped, not composition-scoped: a detector
 * left running while the app is backgrounded keeps sampling a phone in a
 * pocket, and anything downstream that queues work until the app is visible
 * will replay that jostle as a user gesture on resume. Bind with
 * `LifecycleStartEffect`, never `DisposableEffect`.
 *
 * [shakeEvents] is a broadcast stream that drops rather than buffers. A shake
 * is only meaningful in the moment it happens, and a single-consumer channel
 * here means two collectors silently split the events between them instead of
 * both seeing each one.
 */
interface ShakeDetector {
    val shakeEvents: Flow<ShakeEvent>

    fun start()
    fun stop()
}

data class ShakeEvent(
    val timestampMs: Long,
    val intensity: ShakeIntensity = ShakeIntensity.NORMAL,
)

enum class ShakeIntensity {
    GENTLE,
    NORMAL,
    VIGOROUS,
}
