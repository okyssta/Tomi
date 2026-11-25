package com.tomi.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tomi.app.BuildConfig
import com.tomi.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object UpdateCheckScheduler {
    suspend fun checkIfDue(context: Context, force: Boolean = false) {
        val settingsRepository = SettingsRepository.from(context)
        val lastCheck = settingsRepository.lastUpdateCheckAt()
        val now = System.currentTimeMillis()
        if (!force && lastCheck != null && now - lastCheck < CHECK_INTERVAL_MS) return
        settingsRepository.setLastUpdateCheckAt(now)

        val repository = UpdateRepository(
            http = HttpUrlConnectionUpdateHttp(),
            currentVersion = AppVersion.parse(BuildConfig.VERSION_NAME.substringBefore('-')),
            repository = BuildConfig.GITHUB_REPOSITORY,
        )
        when (val result = repository.check()) {
            is UpdateResult.Available -> {
                UpdateNotifier(context).showAvailable(result.info)
                val settings = settingsRepository.settings.first()
                if (settings.autoDownloadUpdates) {
                    UpdateDownloadScheduler.enqueue(context, result.info, unmeteredOnly = true)
                }
            }
            UpdateResult.UpToDate, is UpdateResult.Error -> Unit
        }
    }

    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "tomi_update_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1_000L
}

class UpdateCheckWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters,
) : androidx.work.CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        UpdateCheckScheduler.checkIfDue(applicationContext)
        Result.success()
    } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
    } catch (_: Exception) {
        Result.retry()
    }
}
