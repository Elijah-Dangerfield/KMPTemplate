package com.kmptemplate.libraries.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

/** UIKit's shake, as the window posts it, is a shake while the app listens and nothing otherwise. */
class IosShakeDetectorTest {

    private val detector = IosShakeDetector()

    @Test
    fun aSystemShake_whileStarted_isAShake() = shakesSeen {
        detector.start()
        SystemShake.post()
        detector.stop()
    }.let { assertEquals(1, it) }

    /** Backgrounded, a shake is a phone in a pocket. */
    @Test
    fun aSystemShake_afterStop_isNothing() = shakesSeen {
        detector.start()
        detector.stop()
        SystemShake.post()
    }.let { assertEquals(0, it) }

    /** Every foreground calls start again; one shake must not arrive twice. */
    @Test
    fun startingTwice_stillOneShakePerShake() = shakesSeen {
        detector.start()
        detector.start()
        SystemShake.post()
        detector.stop()
    }.let { assertEquals(1, it) }

    private fun shakesSeen(shake: () -> Unit): Int = runBlocking {
        var seen = 0
        val collecting = launch(start = CoroutineStart.UNDISPATCHED) { detector.shakeEvents.collect { seen++ } }
        shake()
        yield()
        collecting.cancel()
        seen
    }
}
