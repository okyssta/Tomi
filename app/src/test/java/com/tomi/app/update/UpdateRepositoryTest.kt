package com.tomi.app.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {

    private val release = """
        {
          "tag_name": "v1.1.0",
          "html_url": "https://github.com/okyssta/Tomi/releases/tag/v1.1.0",
          "body": "New release",
          "prerelease": false,
          "assets": [
            {"name": "tomi-release.apk", "browser_download_url": "https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk"},
            {"name": "SHA256SUMS.txt", "browser_download_url": "https://github.com/okyssta/Tomi/releases/download/v1.1.0/SHA256SUMS.txt"}
          ]
        }
    """.trimIndent()

    @Test
    fun `reports an available update only when the release is newer`() = runTest {
        val repository = UpdateRepository(
            http = FakeHttp(release),
            currentVersion = AppVersion(1, 0, 0),
            repository = "okyssta/Tomi",
        )

        val result = repository.check()

        assertTrue(result is UpdateResult.Available)
        assertEquals(AppVersion(1, 1, 0), (result as UpdateResult.Available).info.version)
    }

    @Test
    fun `reports up to date for the same or an older release`() = runTest {
        val repository = UpdateRepository(
            http = FakeHttp(release),
            currentVersion = AppVersion(1, 1, 0),
            repository = "okyssta/Tomi",
        )

        assertEquals(UpdateResult.UpToDate, repository.check())
    }

    @Test
    fun `surfaces malformed release data as an error`() = runTest {
        val repository = UpdateRepository(
            http = FakeHttp("not json"),
            currentVersion = AppVersion(1, 0, 0),
            repository = "okyssta/Tomi",
        )

        assertTrue(repository.check() is UpdateResult.Error)
    }

    private class FakeHttp(private val response: String) : UpdateHttp {
        override suspend fun get(url: String): String = response
    }
}
