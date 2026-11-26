package com.tomi.app.backup.drive

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tomi.app.backup.BackupManager
import com.tomi.app.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class DriveBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        GoogleDriveBackupRepository(
            settingsRepository = SettingsRepository.from(applicationContext),
            backup = BackupManager(applicationContext),
            store = SafGoogleDriveStore(applicationContext),
        ).syncIfEnabled()
        Result.success()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        Result.retry()
    }
}

object DriveBackupScheduler {
    private const val PERIODIC_WORK = "tomi_drive_backup_periodic"
    private const val IMMEDIATE_WORK = "tomi_drive_backup_now"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<DriveBackupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK)
    }
}
