package com.tomi.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateDownloadStateCodecTest {

    private val info = UpdateInfo(
        version = AppVersion.parse("1.1.0"),
        versionName = "1.1.0",
        notes = "Verified release notes",
        pageUrl = "https://github.com/okyssta/Tomi/releases/tag/v1.1.0",
        apkAssetName = "tomi-release.apk",
        apkUrl = "https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk",
        checksumUrl = "https://github.com/okyssta/Tomi/releases/download/v1.1.0/SHA256SUMS.txt",
        publishedAt = "2026-11-24T12:00:00Z",
    )

    @Test
    fun `persisted update retains the retry and install metadata`() {
        val pending = PendingUpdateDownload(
            info = info,
            workId = "d9e8a7b6-1234-4abc-8def-1234567890ab",
            enqueuedAt = 1_800_000_000_000L,
        )
        val encoded = UpdateDownloadStateCodec.encode(pending)

        assertEquals(pending, UpdateDownloadStateCodec.decode(encoded))
    }

    @Test
    fun `incomplete persisted data cannot fabricate a retryable update`() {
        assertNull(UpdateDownloadStateCodec.decode("{}"))
    }

    @Test
    fun `persisted retry metadata is revalidated before use`() {
        val pending = PendingUpdateDownload(
            info = info.copy(apkUrl = "http://example.com/tomi-release.apk"),
            workId = "d9e8a7b6-1234-4abc-8def-1234567890ab",
            enqueuedAt = 1_800_000_000_000L,
        )

        assertNull(UpdateDownloadStateCodec.decode(UpdateDownloadStateCodec.encode(pending)))
    }
}
