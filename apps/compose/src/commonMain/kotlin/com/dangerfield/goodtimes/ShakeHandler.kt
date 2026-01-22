package com.dangerfield.goodtimes

import com.dangerfield.goodtimes.libraries.core.ShakeDetector
import com.dangerfield.goodtimes.libraries.core.ShakeEvent
import com.dangerfield.goodtimes.libraries.core.ShakeMessageContext
import com.dangerfield.goodtimes.libraries.core.ShakeMessageProvider
import com.dangerfield.goodtimes.libraries.goodtimes.GetAwarenessContextUseCase
import com.dangerfield.goodtimes.libraries.goodtimes.UserRepository
import com.dangerfield.goodtimes.libraries.navigation.Router
import com.dangerfield.goodtimes.libraries.navigation.ShakeDialogRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(AppScope::class)
class ShakeHandler(
    private val shakeDetector: ShakeDetector,
    private val shakeMessageProvider: ShakeMessageProvider,
    private val userRepository: UserRepository,
    private val getAwarenessContextUseCase: GetAwarenessContextUseCase,
    private val router: Router,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isShowingDialog = false
    
    fun start() {
        shakeDetector.start()
        scope.launch {
            shakeDetector.shakeEvents.collect { event ->
                handleShake(event)
            }
        }
    }
    
    fun stop() {
        shakeDetector.stop()
    }
    
    fun onDialogDismissed() {
        isShowingDialog = false
    }
    
    private suspend fun handleShake(event: ShakeEvent) {
        if (isShowingDialog) return
        
        val user = userRepository.getUser() ?: return
        val awareness = getAwarenessContextUseCase()
        
        val context = ShakeMessageContext(
            shakeCount = user.shakeCount,
            intensity = event.intensity,
            isLateNight = awareness.isLateNight,
            isFirstSession = awareness.isFirstSession,
            userName = user.name,
        )
        
        val message = shakeMessageProvider.getMessage(context)
        
        isShowingDialog = true
        router.navigate(
            ShakeDialogRoute(
                headline = message.headline,
                subtext = message.subtext,
            )
        )
        
        userRepository.onShakeDetected()
    }
}
