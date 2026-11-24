package com.tomi.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `parses release tags with or without a v prefix`() {
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("v1.2.3"))
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("1.2.3"))
    }

    @Test
    fun `compares major minor and patch numerically`() {
        assertTrue(AppVersion(1, 10, 0).isNewerThan(AppVersion(1, 9, 9)))
        assertTrue(AppVersion(2, 0, 0).isNewerThan(AppVersion(1, 99, 99)))
        assertFalse(AppVersion(1, 2, 3).isNewerThan(AppVersion(1, 2, 3)))
    }

    @Test
    fun `stable release is newer than a matching prerelease`() {
        assertTrue(AppVersion(1, 0, 0).isNewerThan(AppVersion(1, 0, 0, "beta.1")))
        assertFalse(AppVersion(1, 0, 0, "beta.1").isNewerThan(AppVersion(1, 0, 0)))
    }

    @Test
    fun `rejects malformed versions`() {
        assertThrows(IllegalArgumentException::class.java) { AppVersion.parse("latest") }
        assertThrows(IllegalArgumentException::class.java) { AppVersion.parse("1.2") }
    }
}
