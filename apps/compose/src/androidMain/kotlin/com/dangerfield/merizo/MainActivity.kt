package com.dangerfield.merizo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appComponent = AndroidAppComponent::class.create(application)
        appComponent.telemetry.initialize()

        UsageMaintenanceWorker.schedule(applicationContext)
        setContent {
            App(appComponent)
        }
    }
}
