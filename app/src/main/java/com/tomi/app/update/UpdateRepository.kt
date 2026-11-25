package com.tomi.app.update

import kotlinx.coroutines.CancellationException

sealed interface UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult
    data object UpToDate : UpdateResult
    data class Error(val cause: Throwable) : UpdateResult
}

interface UpdateHttp {
    suspend fun get(url: String): String
}

class UpdateRepository(
    private val http: UpdateHttp,
    private val currentVersion: AppVersion,
    private val repository: String,
) {
    suspend fun check(): UpdateResult = try {
        require(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").matches(repository))
        val content = http.get("https://api.github.com/repos/$repository/releases/latest")
        val info = GitHubReleaseParser.parse(content, repository)
        when {
            info == null -> UpdateResult.Error(IllegalStateException("No compatible signed release found"))
            info.version.isNewerThan(currentVersion) -> UpdateResult.Available(info)
            else -> UpdateResult.UpToDate
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        UpdateResult.Error(error)
    }
}
