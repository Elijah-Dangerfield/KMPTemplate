package com.dangerfield.merizo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Periodic WorkManager job to run maintenance when the app never foregrounds. */
class UsageMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val component: AppComponent by lazy {
        AndroidAppComponent::class.create(appContext.applicationContext)
    }

    override suspend fun doWork(): Result {
        val result = runMaintenanceNow(component)
        return if (result.success) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "usage-maintenance"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageMaintenanceWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}