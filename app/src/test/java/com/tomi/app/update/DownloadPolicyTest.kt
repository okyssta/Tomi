package com.tomi.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadPolicyTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `accepts a download at the size limit`() {
        assertEquals(250_000_000L, DownloadPolicy.acceptedSize(250_000_000L, DownloadPolicy.MAX_APK_BYTES))
    }

    @Test
    fun `rejects a download over the size limit`() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadPolicy.acceptedSize(250_000_001L, DownloadPolicy.MAX_APK_BYTES)
        }
    }

    @Test
    fun `repeated download requests append instead of replacing active work`() {
        assertEquals(androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, DownloadPolicy.EXISTING_WORK_POLICY)
    }

    @Test
    fun `each worker attempt gets a unique temporary apk path`() {
        val directory = temporaryFolder.newFolder("updates")

        val first = DownloadPolicy.newTemporaryFile(directory, "1.1.0", "apk")
        val second = DownloadPolicy.newTemporaryFile(directory, "1.1.0", "apk")

        assertNotEquals(first.absolutePath, second.absolutePath)
        assertEquals(directory, first.parentFile)
        assertTrue(first.name.startsWith("tomi-1.1.0-apk-"))
        assertTrue(first.name.endsWith(".apk"))
    }

    @Test
    fun `temporary file creation tolerates another worker creating the directory`() {
        val directory = object : File(temporaryFolder.root, "updates") {
            override fun mkdirs(): Boolean {
                super.mkdirs()
                return false
            }
        }

        val temporary = DownloadPolicy.newTemporaryFile(directory, "1.1.0", "apk")

        assertTrue(temporary.exists())
    }

    @Test
    fun `verified temporary apk is promoted without leaving the temporary file`() {
        val directory = temporaryFolder.newFolder("updates")
        val temporary = DownloadPolicy.newTemporaryFile(directory, "1.1.0", "apk").apply { writeText("verified") }
        val destination = File(directory, "tomi-1.1.0.apk")

        DownloadPolicy.promote(temporary, destination)

        assertFalse(temporary.exists())
        assertEquals("verified", destination.readText())
    }
}
