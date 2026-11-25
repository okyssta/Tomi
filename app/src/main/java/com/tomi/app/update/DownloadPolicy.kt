package com.tomi.app.update

import androidx.work.ExistingWorkPolicy
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object DownloadPolicy {
    const val MAX_APK_BYTES = 250_000_000L
    const val MAX_CHECKSUM_BYTES = 1_000_000L
    val EXISTING_WORK_POLICY = ExistingWorkPolicy.APPEND_OR_REPLACE

    fun acceptedSize(size: Long, maximum: Long): Long {
        require(size in 0..maximum) { "Download exceeds the allowed size" }
        return size
    }

    fun newTemporaryFile(directory: File, version: String, kind: String): File {
        require(Regex("^[A-Za-z0-9._-]+$").matches(version)) { "Invalid update version" }
        require(kind == "apk" || kind == "checksum") { "Invalid update file kind" }
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Could not create update directory")
        }
        return File.createTempFile("tomi-$version-$kind-", ".$kind", directory)
    }

    fun promote(temporary: File, destination: File) {
        try {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
