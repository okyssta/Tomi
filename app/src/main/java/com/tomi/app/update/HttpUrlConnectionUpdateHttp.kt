package com.tomi.app.update

import com.tomi.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HttpUrlConnectionUpdateHttp : UpdateHttp {
    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Tomi/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) {
                error("Update service returned HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
