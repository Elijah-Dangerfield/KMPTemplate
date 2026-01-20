package com.dangerfield.goodtimes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appComponent = AndroidAppComponent::class.create(application)
        appComponent.telemetry.initialize()
        // Force eager initialization of lifecycle observer
        appComponent.appEventDispatcher

        setContent {
            App(appComponent)
        }
    }
}
