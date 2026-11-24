package com.tomi.app.update

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ApkVerifier {
    fun verify(apk: File, checksumContent: String, apkName: String): Boolean {
        val expected = checksumContent.lineSequence()
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val name = parts[1].removePrefix("*")
                if (name == apkName) parts[0].lowercase() else null
            }
            .firstOrNull()
            ?: return false
        if (!Regex("^[0-9a-f]{64}$").matches(expected)) return false

        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(apk).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest()
            val expectedBytes = expected.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            MessageDigest.isEqual(expectedBytes, actual)
        }.getOrDefault(false)
    }
}
