package com.tomi.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseUrlsTest {

    private val repository = "okyssta/Tomi"

    @Test
    fun `accepts exact HTTPS GitHub release asset URLs`() {
        val url = "https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk"

        assertTrue(GitHubReleaseUrls.isAllowed(url, repository))
    }

    @Test
    fun `rejects other hosts userinfo and cleartext`() {
        assertFalse(GitHubReleaseUrls.isAllowed("http://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk", repository))
        assertFalse(GitHubReleaseUrls.isAllowed("https://github.com@evil.example/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk", repository))
        assertFalse(GitHubReleaseUrls.isAllowed("https://evil.example/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk", repository))
    }

    @Test
    fun `rejects other repositories traversal queries and fragments`() {
        assertFalse(GitHubReleaseUrls.isAllowed("https://github.com/other/Tomi/releases/download/v1.1.0/tomi-release.apk", repository))
        assertFalse(GitHubReleaseUrls.isAllowed("https://github.com/okyssta/Tomi/releases/download/v1.1.0/../evil.apk", repository))
        assertFalse(GitHubReleaseUrls.isAllowed("https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk?token=x", repository))
        assertFalse(GitHubReleaseUrls.isAllowed("https://github.com/okyssta/Tomi/releases/download/v1.1.0/tomi-release.apk#x", repository))
    }
}
