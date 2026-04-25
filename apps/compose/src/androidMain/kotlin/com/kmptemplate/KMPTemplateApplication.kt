package com.kmptemplate

import android.app.Application

class KMPTemplateApplication : Application() {
    
    lateinit var appComponent: AndroidAppComponent
        private set
    
    override fun onCreate() {
        super.onCreate()
        appComponent = AndroidAppComponent::class.create(this)
        appComponent.telemetry.initialize()
        appComponent.appEventDispatcher
        // Eagerly start tracking the foreground Activity so bindings that
        // need it (e.g. AndroidReviewPrompter) work the moment they're called.
        appComponent.activityProvider
    }
}
