package com.tomi.app.reminder

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tomi.app.BuildConfig
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.health.HealthConnectManager
import com.tomi.app.update.UpdateCheckScheduler
import com.tomi.app.widget.TomiWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
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
            val settings = settingsRepository.settings.first()
            if (settings.smartPause) {
                val healthManager = HealthConnectManager(context, settingsRepository, TomiDatabase.get(context).stepCacheDao())
                var shouldSyncSteps = false
                MovementCheckpointLock.mutex.withLock {
                    val runtime = settingsRepository.runtime.first()
                    shouldSyncSteps = MovementCheckpointProcessor.process(
                        enabled = settings.smartPause,
                        queriedCheckpoint = runtime.lastStepCheckAt,
                        hasPermission = healthManager::hasBackgroundReadPermission,
                        readSteps = healthManager::stepsSince,
                        updateRuntime = settingsRepository::updateRuntime,
                        onMovement = { scheduler.reschedule() },
                    )
                }
                if (shouldSyncSteps) healthManager.syncSteps()
            }
            scheduler.checkAndNotifyIfDue()
            if (BuildConfig.UPDATES_ENABLED) UpdateCheckScheduler.checkIfDue(context)
            runCatching { TomiWidget().updateAll(context) }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "tomi_periodic_maintenance",
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
