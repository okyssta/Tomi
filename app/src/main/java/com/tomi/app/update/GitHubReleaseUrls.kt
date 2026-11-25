package com.tomi.app.update

import java.net.URI

object GitHubReleaseUrls {
    fun isAllowed(url: String, repository: String): Boolean = runCatching {
        val uri = URI(url)
        val expectedPrefix = "/$repository/releases/"
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("github.com", ignoreCase = true) &&
            uri.userInfo == null &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            uri.path.startsWith(expectedPrefix) &&
            uri.path.split('/').none { it == ".." }
    }.getOrDefault(false)
}
