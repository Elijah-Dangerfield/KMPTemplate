package com.kmptemplate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmptemplate.features.home.HomeRoute
import com.kmptemplate.features.onboarding.OnboardingRoute
import com.kmptemplate.libraries.core.logging.KLog
import com.kmptemplate.libraries.identity.auth.AuthRepository
import com.kmptemplate.libraries.identity.auth.AuthState
import com.kmptemplate.libraries.kmptemplate.AppCache
import com.kmptemplate.libraries.navigation.Route
import com.kmptemplate.libraries.networking.AccessDeniedBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * App-level ViewModel. Resolves the start destination by reading the
 * persistent `AppData` cache — onboarded users land on [HomeRoute];
 * first-launch users land on [OnboardingRoute].
 *
 * Scoped as singleton so Android's splash-screen API can read the same
 * instance used by the App composable.
 *
 * `startDestination` is a [StateFlow] that's `null` until the cache read
 * completes. The App composable blocks NavHost construction on a non-null
 * value, keeping the splash visible during the brief async read. [isReady]
 * flips at the same moment, releasing the platform splash (Android's native
 * splash API / iOS's splash overlay).
 *
 * The iOS splash-overlay state lives inside the [App] composable itself, not
 * here — keeping it out of the reactive app state means flipping it cannot
 * trigger a root recomposition (which previously caused the NavHost to re-emit
 * its start destination).
 */
@SingleIn(AppScope::class)
@Inject
class AppViewModel(
    private val appCache: AppCache,
    private val authRepository: AuthRepository,
    private val accessDeniedBus: AccessDeniedBus,
) : ViewModel() {

    private val logger = KLog.withTag("AppNav")

    private val _startDestination = MutableStateFlow<Route?>(null)
    val startDestination: StateFlow<Route?> = _startDestination.asStateFlow()

    /**
     * Fires when the auth server rejected our session mid-run
     * ([AuthState.Unauthenticated.Reason.SessionExpired]). The App composable
     * collects this to boot the user back to a recovery screen + explain why. A
     * one-shot **event** (navigation), so a `Channel` — same convention as
     * `SEAViewModel`: exactly-once delivery to the single collector, no replay,
     * buffered if it fires before the collector is ready.
     */
    private val _sessionExpired = Channel<SessionExpired>(Channel.UNLIMITED)
    val sessionExpired: Flow<SessionExpired> = _sessionExpired.receiveAsFlow()

    /**
     * Fires when the server returned a `403` with the locked access-denied
     * envelope — the caller is banned / suspended. The App composable collects
     * this to push a blocking access-denied screen with copy keyed off
     * [AccessDeniedBus.Denial.reason] + the appeal link. Same Channel shape as
     * [sessionExpired] — exactly-once delivery, buffered if it fires before the
     * collector is ready.
     *
     * Distinct from [sessionExpired] because the routing is different (a banned
     * user's session isn't dead — the token still validates — so we can't tear it
     * down; the screen owns the appeal choice instead).
     */
    private val _accessDenied = Channel<AccessDeniedBus.Denial>(Channel.UNLIMITED)
    val accessDenied: Flow<AccessDeniedBus.Denial> = _accessDenied.receiveAsFlow()

    private val _isReady = MutableStateFlow(false)

    /**
     * True once the start destination is resolved. Exposed to Android's splash
     * screen API for keepOnScreenCondition so the platform splash dismisses
     * promptly once we know where to navigate.
     */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            val onboarded = appCache.get().hasUserOnboarded
            logger.d { "Resolving start destination: hasUserOnboarded=$onboarded → ${if (onboarded) "Home" else "Onboarding"}" }
            _startDestination.value = if (onboarded) HomeRoute() else OnboardingRoute()
            _isReady.value = true
        }

        // Boot the user to re-auth when the auth server rejects our session
        // mid-run. The teardown (clear caches / session / account-scoped data)
        // already happened in AuthRepository — this only handles the
        // *navigation* + the "why" message, which an ambient network-triggered
        // event can't do from a feature screen.
        viewModelScope.launch {
            var previousExpired = false
            authRepository.observe().collect { auth ->
                val expired = auth is AuthState.Unauthenticated &&
                    auth.reason == AuthState.Unauthenticated.Reason.SessionExpired
                // Fire only on the edge into SessionExpired, never repeatedly.
                if (expired && !previousExpired) {
                    // Route to the blocking session-expired screen; that screen
                    // owns "sign in again" (claimed) vs "start fresh" (guest).
                    _sessionExpired.trySend(SessionExpired(wasAnonymous = auth.wasAnonymous))
                }
                previousExpired = expired
            }
        }

        // Route to the access-denied screen when the network layer reports a `403`
        // with the locked envelope. Forward every signal — the App composable's
        // `launchSingleTop` keeps a burst of denied calls from stacking duplicate
        // screens, and the screen itself is dismiss-blocked, so re-emission is
        // safe.
        viewModelScope.launch {
            accessDeniedBus.denials.collect { denial -> _accessDenied.trySend(denial) }
        }
    }

    /** A server-rejected session; [wasAnonymous] picks the guest vs claimed copy. */
    data class SessionExpired(val wasAnonymous: Boolean)
}
