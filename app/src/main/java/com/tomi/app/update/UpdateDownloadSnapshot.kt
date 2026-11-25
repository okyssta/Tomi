package com.tomi.app.update

import androidx.work.WorkInfo

internal data class UpdateDownloadSnapshot(
    val pending: PendingUpdateDownload,
    val work: WorkInfo,
)
