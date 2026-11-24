package com.tomi.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object UpdateDownloadScheduler {
    fun enqueue(context: Context, info: UpdateInfo, unmeteredOnly: Boolean) {
        val network = if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
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
        WorkManager.getInstance(context).enqueueUniqueWork(
            "tomi_update_${info.versionName}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
