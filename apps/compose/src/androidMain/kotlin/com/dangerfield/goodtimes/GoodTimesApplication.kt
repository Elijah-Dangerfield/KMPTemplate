package com.dangerfield.goodtimes

import android.app.Application

class GoodTimesApplication : Application() {
    
    lateinit var appComponent: AndroidAppComponent
        private set
    
    override fun onCreate() {
        super.onCreate()
        appComponent = AndroidAppComponent::class.create(this)
        appComponent.telemetry.initialize()
        appComponent.appEventDispatcher
    }
}
