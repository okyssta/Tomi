package com.tomi.app.ui

import androidx.work.WorkInfo
import com.tomi.app.update.AppVersion
import com.tomi.app.update.UpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateDownloadStateTest {

    private val info = UpdateInfo(
        version = AppVersion.parse("1.1.0"),
        versionName = "1.1.0",
        notes = "",
        pageUrl = "https://github.com/okyssta/Tomi/releases/tag/v1.1.0",
        apkAssetName = "tomi-release.apk",
        apkUrl = "https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk",
        checksumUrl = "https://github.com/okyssta/Tomi/releases/download/v1.1.0/SHA256SUMS.txt",
        publishedAt = null,
    )

    @Test
    fun `successful verified work becomes ready to install`() {
        assertEquals(UpdateUiState.DownloadReady(info), updateDownloadUiState(info, WorkInfo.State.SUCCEEDED))
    }

    @Test
    fun `failed work becomes a retryable error`() {
        val state = updateDownloadUiState(info, WorkInfo.State.FAILED)
        val error = state as UpdateUiState.Error

        assertEquals("Update download failed. Tap Download to retry.", error.message)
        assertEquals(info, error.info)
    }

    @Test
    fun `nonterminal work remains observable as queued`() {
        listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED).forEach { state ->
            assertEquals(UpdateUiState.DownloadQueued(info), updateDownloadUiState(info, state))
        }
    }
}
