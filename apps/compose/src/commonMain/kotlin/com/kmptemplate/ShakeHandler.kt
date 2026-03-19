package com.kmptemplate

import com.kmptemplate.libraries.core.ShakeDetector
import com.kmptemplate.libraries.core.ShakeEvent
import com.kmptemplate.libraries.core.ShakeMessageContext
import com.kmptemplate.libraries.core.ShakeMessageProvider
import com.kmptemplate.libraries.kmptemplate.UserRepository
import com.kmptemplate.libraries.navigation.Router
import com.kmptemplate.libraries.navigation.ShakeDialogRoute
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
        
        val context = ShakeMessageContext(
            shakeCount = user.shakeCount,
            intensity = event.intensity,
            isLateNight = false,
            isFirstSession = user.sessionsCount <= 1,
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
