package com.tomi.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleaseParserTest {

    private val releaseJson = """
        {
          "tag_name": "v1.1.0",
          "name": "Tomi 1.1.0",
          "body": "Faster reminders and bug fixes.",
          "html_url": "https://github.com/okyssta/Tomi/releases/tag/v1.1.0",
          "published_at": "2026-11-24T12:00:00Z",
          "prerelease": false,
          "assets": [
            {
              "name": "tomi-release.apk",
              "browser_download_url": "https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk"
            },
            {
              "name": "SHA256SUMS.txt",
              "browser_download_url": "https://github.com/okyssta/Tomi/releases/download/v1.1.0/SHA256SUMS.txt"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses a stable release and its signed apk asset`() {
        val update = GitHubReleaseParser.parse(releaseJson)

        assertNotNull(update)
        requireNotNull(update)
        assertEquals(AppVersion(1, 1, 0), update.version)
        assertEquals("Faster reminders and bug fixes.", update.notes)
        assertEquals(
            "https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk",
            update.apkUrl,
        )
        assertEquals("tomi-release.apk", update.apkAssetName)
        assertEquals(
            "https://github.com/okyssta/Tomi/releases/download/v1.1.0/SHA256SUMS.txt",
            update.checksumUrl,
        )
    }

    @Test
    fun `does not offer prereleases as automatic updates`() {
        val json = releaseJson.replace("\"prerelease\": false", "\"prerelease\": true")

        assertNull(GitHubReleaseParser.parse(json))
    }

    @Test
    fun `ignores releases without an apk asset`() {
        val json = releaseJson.replace(
            "\"prerelease\": false",
            "\"prerelease\": false",
        ).substringBefore("\"assets\"") + "\"assets\": [] }"

        assertNull(GitHubReleaseParser.parse(json))
    }

    @Test
    fun `rejects insecure asset urls`() {
        val json = releaseJson.replace("https://github.com/okyssta/Tomi/releases/download/", "http://example.com/")

        assertNull(GitHubReleaseParser.parse(json))
    }
}
