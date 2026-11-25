package com.tomi.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

object UpdateDownloadScheduler {
    const val TAG_DOWNLOAD = "tomi_update_download"
    const val KEY_VERSION = "tomi_update_version"
    private val enqueueMutex = Mutex()
    private var generation = 0L

    fun workName(info: UpdateInfo): String = "tomi_update_${info.versionName}"

    internal suspend fun latestDownload(context: Context): UpdateDownloadSnapshot? = enqueueMutex.withLock {
        val pending = UpdateDownloadStateStore.read(context) ?: return@withLock null
        val workId = runCatching { UUID.fromString(pending.workId) }.getOrNull() ?: return@withLock null
        val work = WorkManager.getInstance(context).getWorkInfoByIdFlow(workId).first() ?: return@withLock null
        UpdateDownloadSnapshot(pending, work)
    }

    suspend fun latestWork(context: Context): androidx.work.WorkInfo? = latestDownload(context)?.work

    suspend fun enqueue(context: Context, info: UpdateInfo, unmeteredOnly: Boolean): String {
        val startedGeneration = generation
        return enqueueMutex.withLock {
            if (startedGeneration != generation) {
                throw CancellationException("Update enqueue superseded by data clear")
            }
            enqueueDownload(
                info = info,
                unmeteredOnly = unmeteredOnly,
                enqueueWork = { workName, policy, request ->
                    WorkManager.getInstance(context).enqueueUniqueWork(workName, policy, request)
                },
                saveSnapshot = { pending -> UpdateDownloadStateStore.save(context, pending) },
                cancelWork = { id -> WorkManager.getInstance(context).cancelWorkById(id).await() },
            )
        }
    }

    internal suspend fun enqueueDownload(
        info: UpdateInfo,
        unmeteredOnly: Boolean,
        enqueueWork: (String, androidx.work.ExistingWorkPolicy, OneTimeWorkRequest) -> Unit,
        saveSnapshot: suspend (PendingUpdateDownload) -> Unit,
        cancelWork: suspend (UUID) -> Unit = {},
    ): String {
        val workName = workName(info)
        val workId = UUID.randomUUID()
        val network = if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setId(workId)
            .addTag(TAG_DOWNLOAD)
            .setInputData(
                workDataOf(
                    UpdateDownloadWorker.KEY_APK_URL to info.apkUrl,
                    UpdateDownloadWorker.KEY_APK_ASSET_NAME to info.apkAssetName,
                    UpdateDownloadWorker.KEY_CHECKSUM_URL to info.checksumUrl,
                    UpdateDownloadWorker.KEY_VERSION to info.versionName,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(network)
                    .build(),
            )
            .build()
        enqueueWork(workName, DownloadPolicy.EXISTING_WORK_POLICY, request)
        try {
            saveSnapshot(
                PendingUpdateDownload(info = info, workId = workId.toString(), enqueuedAt = System.currentTimeMillis()),
            )
        } catch (error: Exception) {
            runCatching { cancelWork(workId) }.onFailure(error::addSuppressed)
            throw error
        }
        return workId.toString()
    }

    suspend fun cancelAndClear(context: Context) = enqueueMutex.withLock {
        WorkManager.getInstance(context).cancelAllWork().await()
        UpdateDownloadStateStore.clear(context)
        generation++
    }
}
