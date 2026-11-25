package com.tomi.app.update

import android.content.Context
import androidx.work.WorkManager
import androidx.work.await

object UpdateDownloadScheduler {
    const val TAG_DOWNLOAD = "tomi_update_download"
    const val KEY_VERSION = "tomi_update_version"

    fun workName(info: UpdateInfo): String = "tomi_update_${info.versionName}"
    internal suspend fun latestDownload(context: Context): UpdateDownloadSnapshot? = null
    suspend fun latestWork(context: Context): androidx.work.WorkInfo? = null
    fun enqueue(context: Context, info: UpdateInfo, unmeteredOnly: Boolean): String = workName(info)
    suspend fun cancelAndClear(context: Context) {
        WorkManager.getInstance(context).cancelAllWork().await()
        UpdateDownloadStateStore.clear(context)
    }
}
