@file:OptIn(ExperimentalObjCName::class)

package com.kmptemplate.libraries.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.tatarka.inject.annotations.Inject
import platform.Foundation.NSDate
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObjectProtocol
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Shakes as UIKit recognizes them: the app's `UIWindow` hears the system shake gesture and calls
 * [SystemShake.post] (`ShakeWindow.swift`).
 *
 * This used to run [ShakeRecognizer] over CoreMotion's accelerometer, which never fired on a
 * real iPhone in an app built from here and can't run in the simulator at all (it has no
 * accelerometer). UIKit's gesture is the one iOS people already know from shake to undo, works
 * the same on a device and under Device › Shake in the simulator, and needs no sensor or tuning
 * of ours.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosShakeDetector : ShakeDetector {

    private var observer: NSObjectProtocol? = null

    private val shakes = MutableSharedFlow<ShakeEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val shakeEvents: Flow<ShakeEvent> = shakes.asSharedFlow()

    override fun start() {
        if (observer != null) return
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = SystemShake.NOTIFICATION,
            `object` = null,
            // On the posting thread, which is main: UIKit delivers the shake there.
            queue = null,
        ) { _: NSNotification? ->
            shakes.tryEmit(ShakeEvent(timestampMs = (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()))
        }
    }

    override fun stop() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observer = null
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000
    }
}

/**
 * The door Swift knocks on when UIKit recognizes a shake. A notification rather than a call into
 * [IosShakeDetector], so the window needs no reference to the app's graph and a shake while the
 * detector is stopped (backgrounded) reaches nobody.
 */
@ObjCName("SystemShake", exact = true)
object SystemShake {
    internal const val NOTIFICATION = "com.kmptemplate.systemShake"

    fun post() {
        NSNotificationCenter.defaultCenter.postNotificationName(NOTIFICATION, `object` = null)
    }
}
