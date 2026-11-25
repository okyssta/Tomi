package com.tomi.app.update

import com.tomi.app.BuildConfig
import kotlinx.serialization.json.Json
import java.util.UUID

internal object UpdateDownloadStateCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(pending: PendingUpdateDownload): String =
        json.encodeToString(PendingUpdateDownload.serializer(), pending)

    fun decode(content: String): PendingUpdateDownload? = runCatching {
        json.decodeFromString(PendingUpdateDownload.serializer(), content)
    }.getOrNull()?.takeIf { pending ->
        val info = pending.info
        val validWorkId = runCatching { UUID.fromString(pending.workId) }.isSuccess
        val safeVersion = Regex("^[A-Za-z0-9._-]+$").matches(info.versionName) &&
            runCatching { AppVersion.parse(info.versionName) == info.version }.getOrDefault(false)
        val safeAsset = Regex("^[A-Za-z0-9._-]+$").matches(info.apkAssetName)
        validWorkId &&
            pending.enqueuedAt >= 0L &&
            safeVersion &&
            safeAsset &&
            GitHubReleaseUrls.isAllowed(info.apkUrl, BuildConfig.GITHUB_REPOSITORY) &&
            GitHubReleaseUrls.isAllowed(info.checksumUrl, BuildConfig.GITHUB_REPOSITORY) &&
            GitHubReleaseUrls.isAllowed(info.pageUrl, BuildConfig.GITHUB_REPOSITORY)
    }
}
