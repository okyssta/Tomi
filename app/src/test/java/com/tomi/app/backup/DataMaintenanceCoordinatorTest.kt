package com.tomi.app.backup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DataMaintenanceCoordinatorTest {

    @Test
    fun `cache cleanup failure is reported only after persistent data is cleared`() = runBlocking {
        val events = mutableListOf<String>()

        val failure = runCatching {
            DataMaintenanceCoordinator.clear(
                deleteUpdateCache = { events += "cache"; false },
                clearDatabase = { events += "database" },
                clearSettings = { events += "settings" },
            )
        }.exceptionOrNull()

        assertTrue(failure is java.io.IOException)
        assertEquals(listOf("database", "settings", "cache"), events)
        Unit
    }

    @Test
    fun `settings failure preserves the update cache for recovery`() = runBlocking {
        val failure = IllegalStateException("Settings I/O failed")
        var cacheDeleted = false

        val thrown = runCatching {
            DataMaintenanceCoordinator.clear(
                deleteUpdateCache = { cacheDeleted = true; true },
                clearDatabase = { },
                clearSettings = { throw failure },
            )
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertFalse(cacheDeleted)
        Unit
    }

    @Test
    fun `external cleanup waits until database and settings deletion succeed`() = runBlocking {
        val events = mutableListOf<String>()

        DataMaintenanceCoordinator.clear(
            deleteUpdateCache = { events += "cache"; true },
            clearDatabase = { events += "database" },
            clearSettings = { events += "settings" },
            afterPersistentDataCleared = { events += "external" },
        )

        assertEquals(listOf("database", "settings", "external", "cache"), events)
        Unit
    }

    @Test
    fun `settings failure preserves external resources for recovery`() = runBlocking {
        val failure = IllegalStateException("Settings I/O failed")
        var externalCleaned = false

        val thrown = runCatching {
            DataMaintenanceCoordinator.clear(
                deleteUpdateCache = { true },
                clearDatabase = { },
                clearSettings = { throw failure },
                afterPersistentDataCleared = { externalCleaned = true },
            )
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertFalse(externalCleaned)
        Unit
    }

    @Test
    fun `database failure is reported and does not claim settings were cleared`() = runBlocking {
        val failure = IllegalStateException("Database I/O failed")
        var settingsCleared = false

        val thrown = runCatching {
            DataMaintenanceCoordinator.clear(
                deleteUpdateCache = { true },
                clearDatabase = { throw failure },
                clearSettings = { settingsCleared = true },
            )
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertFalse(settingsCleared)
        Unit
    }

    @Test
    fun `clear and import share one exclusive action lock`() = runBlocking {
        val clearStarted = CompletableDeferred<Unit>()
        val releaseClear = CompletableDeferred<Unit>()
        var importStarted = false

        val clear = async {
            DataMaintenanceCoordinator.exclusive {
                clearStarted.complete(Unit)
                releaseClear.await()
            }
        }
        clearStarted.await()
        val import = async {
            DataMaintenanceCoordinator.exclusive { importStarted = true }
        }
        yield()

        val startedEarly = withTimeoutOrNull(1_000) { import.await(); true } != null
        releaseClear.complete(Unit)
        awaitAll(clear, import)

        assertFalse(startedEarly)
        assertTrue(importStarted)
        Unit
    }
}
