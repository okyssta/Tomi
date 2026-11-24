package com.tomi.app.reminder

import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.health.HealthConnectManager
import com.tomi.app.update.UpdateCheckScheduler
import com.tomi.app.widget.TomiWidget
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        return try {
            val settingsRepository = SettingsRepository.from(context)
            val scheduler = ReminderScheduler(context, settingsRepository, ReminderNotifier(context))
            scheduler.checkAndNotifyIfDue()

            if (HealthConnectManager(context, settingsRepository, TomiDatabase.get(context).stepCacheDao())
                .hasBackgroundReadPermission()
            ) {
                HealthConnectManager(context, settingsRepository, TomiDatabase.get(context).stepCacheDao())
                    .syncSteps()
            }
            UpdateCheckScheduler.checkIfDue(context)
            runCatching { TomiWidget().updateAll(context) }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "tomi_periodic_maintenance",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
