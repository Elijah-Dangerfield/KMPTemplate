package com.kmptemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kmptemplate.libraries.telemetry.impl.AndroidJankMonitor

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge with light status bar (dark icons)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Keep the splash screen on until AppViewModel has determined the destination.
        // AppViewModel is a singleton, so this is the same instance used in App composable.
        splashScreen.setKeepOnScreenCondition {
            !appComponent.appViewModel.isReady.value
        }

        // JankStats needs a Window, so it can only be armed once the Activity
        // has one. Attaching after setContent would miss the first frames,
        // which are the ones most likely to be janky.
        (appComponent.jankMonitor as? AndroidJankMonitor)?.attach(window)

        setContent {
            App(appComponent)
        }
    }

    override fun onStop() {
        super.onStop()
        // Flush whatever this screen accumulated before the process can be
        // killed in the background. A session that never returns still reports
        // the screen it was on, which is the one worth knowing about.
        appComponent.jankMonitor.onBackground()
    }

    override fun onDestroy() {
        (appComponent.jankMonitor as? AndroidJankMonitor)?.detach()
        super.onDestroy()
    }

    private val appComponent get() = (application as KMPTemplateApplication).appComponent
}
