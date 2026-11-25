package com.tomi.app.update

import androidx.work.OneTimeWorkRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class UpdateDownloadSchedulerTest {
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
    fun `work name is stable and version scoped`() {
        assertEquals("tomi_update_1.1.0", UpdateDownloadScheduler.workName(info))
    }

    @Test
    fun `enqueue failure leaves existing retry metadata untouched`() = runBlocking {
        val previous = PendingUpdateDownload(info, "d9e8a7b6-1234-4abc-8def-1234567890ab", 1L)
        var persisted = previous
        val failure = IllegalStateException("WorkManager unavailable")

        val thrown = runCatching {
            UpdateDownloadScheduler.enqueueDownload(
                info = info,
                unmeteredOnly = false,
                enqueueWork = { _, _, _ -> throw failure },
                saveSnapshot = { persisted = it },
            )
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertSame(previous, persisted)
        Unit
    }

    @Test
    fun `snapshot is persisted only after WorkManager accepts the request`() = runBlocking {
        val events = mutableListOf<String>()
        var persisted: PendingUpdateDownload? = null

        val workId = UpdateDownloadScheduler.enqueueDownload(
            info = info,
            unmeteredOnly = false,
            enqueueWork = { _, policy, request ->
                events += "enqueue"
                assertEquals(DownloadPolicy.EXISTING_WORK_POLICY, policy)
            },
            saveSnapshot = { pending ->
                events += "save"
                persisted = pending
            },
        )

        assertEquals(listOf("enqueue", "save"), events)
        assertEquals(workId, persisted?.workId)
        Unit
    }

    @Test
    fun `snapshot failure cancels the accepted work request`() = runBlocking {
        val failure = IllegalStateException("DataStore unavailable")
        var cancelledId: java.util.UUID? = null

        val thrown = runCatching {
            UpdateDownloadScheduler.enqueueDownload(
                info = info,
                unmeteredOnly = false,
                enqueueWork = { _, _, _ -> },
                saveSnapshot = { throw failure },
                cancelWork = { cancelledId = it },
            )
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals(cancelledId != null, true)
    }
}
