package com.tomi.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class ApkVerifierTest {

    @Test
    fun `accepts a matching apk checksum`() {
        val apk = File.createTempFile("tomi", ".apk").apply {
            writeBytes("verified apk".toByteArray())
            deleteOnExit()
        }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(apk.readBytes())
            .joinToString("") { "%02x".format(it) }

        assertTrue(ApkVerifier.verify(apk, "$hash  ${apk.name}\n", apk.name))
    }

    @Test
    fun `rejects a modified apk`() {
        val apk = File.createTempFile("tomi", ".apk").apply {
            writeBytes("tampered apk".toByteArray())
            deleteOnExit()
        }
        val checksum = "${"0".repeat(64)}  ${apk.name}\n"

        assertFalse(ApkVerifier.verify(apk, checksum, apk.name))
    }

    @Test
    fun `rejects a checksum file that omits the apk`() {
        val apk = File.createTempFile("tomi", ".apk").apply {
            writeBytes("apk".toByteArray())
            deleteOnExit()
        }

        assertFalse(ApkVerifier.verify(apk, "${"0".repeat(64)}  other.apk\n", apk.name))
    }
}
