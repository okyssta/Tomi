package com.tomi.app.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class UpdateInfo(
    val version: AppVersion,
    val versionName: String,
    val notes: String,
    val pageUrl: String,
    val apkAssetName: String,
    val apkUrl: String,
    val checksumUrl: String,
    val publishedAt: String?,
)

object GitHubReleaseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(content: String, expectedRepository: String = "okyssta/Tomi"): UpdateInfo? {
        return try {
            val release = json.decodeFromString(GitHubReleaseDto.serializer(), content)
            if (release.draft || release.prerelease) return null

            val apk = release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true) && GitHubReleaseUrls.isAllowed(it.browserDownloadUrl, expectedRepository)
            } ?: return null
            val checksum = release.assets.firstOrNull {
                it.name.equals("SHA256SUMS.txt", ignoreCase = true) &&
                    GitHubReleaseUrls.isAllowed(it.browserDownloadUrl, expectedRepository)
            } ?: return null
            if (!GitHubReleaseUrls.isAllowed(release.htmlUrl, expectedRepository)) return null

            UpdateInfo(
                version = AppVersion.parse(release.tagName),
                versionName = release.tagName.removePrefix("v"),
                notes = release.body?.takeIf { it.isNotBlank() } ?: "No release notes provided.",
                pageUrl = release.htmlUrl,
                apkAssetName = apk.name,
                apkUrl = apk.browserDownloadUrl,
                checksumUrl = checksum.browserDownloadUrl,
                publishedAt = release.publishedAt,
            )
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    @Serializable
    private data class GitHubReleaseDto(
        @SerialName("tag_name") val tagName: String,
        @SerialName("html_url") val htmlUrl: String,
        val body: String? = null,
        @SerialName("published_at") val publishedAt: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<GitHubAssetDto> = emptyList(),
    )

    @Serializable
    private data class GitHubAssetDto(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )
}
