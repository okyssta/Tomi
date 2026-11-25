package com.tomi.app.update

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class UpdateDownloadStateStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        UpdateDownloadStateStore.clear(context)
        Unit
    }

    @After
    fun tearDown() = runBlocking {
        UpdateDownloadStateStore.clear(context)
        Unit
    }

    @Test
    fun `persisted download retains real retry metadata across reads`() = runBlocking {
        val pending = PendingUpdateDownload(
            info = UpdateInfo(
                version = AppVersion.parse("1.1.0"),
                versionName = "1.1.0",
                notes = "Real release notes",
                pageUrl = "https://github.com/okyssta/Tomi/releases/tag/v1.1.0",
                apkAssetName = "tomi-release.apk",
                apkUrl = "https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk",
                checksumUrl = "https://github.com/okyssta/Tomi/releases/download/v1.1.0/SHA256SUMS.txt",
                publishedAt = "2026-11-24T12:00:00Z",
            ),
            workId = "d9e8a7b6-1234-4abc-8def-1234567890ab",
            enqueuedAt = 1_800_000_000_000L,
        )

        UpdateDownloadStateStore.save(context, pending)

        assertEquals(pending, UpdateDownloadStateStore.read(context))
        UpdateDownloadStateStore.clear(context)
        assertNull(UpdateDownloadStateStore.read(context))
        Unit
    }
}
