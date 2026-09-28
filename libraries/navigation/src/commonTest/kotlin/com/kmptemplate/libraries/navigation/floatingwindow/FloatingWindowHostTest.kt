package com.kmptemplate.libraries.navigation.floatingwindow

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavigatorState
import androidx.savedstate.SavedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the leak fixed by the `LaunchedEffect` sweep in [FloatingWindowHost]: an entry pushed and
 * popped before the host ever composes it never disposes, so a host that completes the navigator
 * transition only from `onDispose` holds that entry in `transitionsInProgress` forever.
 *
 * NOT covered here: the guard that stops that sweep completing a window the user can still see.
 * Reaching it means putting an entry on the visible back stack, which observes its lifecycle, and
 * lifecycle observation enforces the Android main thread, which this host-JVM tier has no way to
 * satisfy. Same for anything needing a real `NavController`. That half of the fix is pinned by
 * matching AndroidX's `DialogHost` line for line instead.
 */
class FloatingWindowHostTest {

    @Test
    fun entryPoppedBeforeItComposesDoesNotStayInTransition() = runTest {
        val navigatorState = MinimalNavigatorState()
        val navigator = FloatingWindowNavigator()
        navigator.onAttach(navigatorState)

        val entry = navigatorState.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(entry), navOptions = null, navigatorExtras = null)
        navigator.popBackStack(entry, savedState = false)

        assertTrue(
            navigatorState.transitionsInProgress.value.contains(entry),
            "precondition: the popped entry is waiting on the host to complete its transition"
        )

        composeUntilIdle { FloatingWindowHost(navigator) }

        assertEquals(
            emptySet(),
            navigatorState.transitionsInProgress.value,
            "the host must release an entry it never composed, or NavController pins that entry " +
                "and its ViewModelStore for the life of the process"
        )
    }
}

/**
 * Composes [content] into a throwaway composition and runs its effects to quiescence. Compose's own
 * test harness is UI-tier and unavailable in this module; the host under test needs nothing from a
 * UI tree, only a running [Recomposer] so its `LaunchedEffect` starts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.composeUntilIdle(content: @Composable () -> Unit) {
    val recomposer = Recomposer(coroutineContext)
    val composition = Composition(NoOpApplier(), recomposer)
    val recomposeJob = launch(BroadcastFrameClock()) { recomposer.runRecomposeAndApplyChanges() }
    try {
        composition.setContent(content)
        advanceUntilIdle()
    } finally {
        composition.dispose()
        recomposer.cancel()
        recomposeJob.cancel()
    }
}

/**
 * The smallest [NavigatorState] a navigator can be attached to. `navigation-testing` publishes no
 * multiplatform artifact, and the push/pop bookkeeping this test asserts on is concrete in the base
 * class anyway.
 */
private class MinimalNavigatorState : NavigatorState() {
    override fun createBackStackEntry(
        destination: NavDestination,
        arguments: SavedState?
    ): NavBackStackEntry = NavBackStackEntry.create(
        context = null,
        destination = destination,
        arguments = arguments
    )
}

private class NoOpApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
}
