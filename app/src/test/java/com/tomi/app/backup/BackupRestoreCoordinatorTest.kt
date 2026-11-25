package com.tomi.app.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreCoordinatorTest {

    @Test
    fun `scheduler failure does not fail an already committed restore`() = runBlocking {
        var settingsWritten = false
        var recordsReplaced = false

        BackupRestoreCoordinator.restore(
            previousSettings = "old",
            importedSettings = "imported",
            updateSettings = { settingsWritten = true },
            replaceRecords = { recordsReplaced = true },
            rescheduleReminders = { error("Alarm manager unavailable") },
        )

        assertTrue(settingsWritten)
        assertTrue(recordsReplaced)
    }

    @Test
    fun `record failure restores the previous settings`() = runBlocking {
        var settings = "old"
        val failure = IllegalStateException("Database failed")

        val thrown = runCatching {
            BackupRestoreCoordinator.restore(
                previousSettings = "old",
                importedSettings = "imported",
                updateSettings = { value -> settings = value },
                replaceRecords = { throw failure },
                rescheduleReminders = { },
            )
        }.exceptionOrNull()

        org.junit.Assert.assertTrue(thrown is IllegalStateException)
        org.junit.Assert.assertEquals(failure.message, thrown?.message)
        org.junit.Assert.assertEquals("old", settings)
    }

    @Test
    fun `mid-import cancellation cannot leave imported settings with old records`() = runBlocking {
        var settings = "old"
        var records = "old"
        val settingsWritten = CompletableDeferred<Unit>()
        val releaseSettingsWrite = CompletableDeferred<Unit>()

        val import = async {
            BackupRestoreCoordinator.restore(
                previousSettings = "old",
                importedSettings = "imported",
                updateSettings = { value ->
                    settings = value
                    settingsWritten.complete(Unit)
                    releaseSettingsWrite.await()
                },
                replaceRecords = { records = "imported" },
                rescheduleReminders = { },
            )
        }
        settingsWritten.await()
        import.cancel()
        releaseSettingsWrite.complete(Unit)
        val thrown = runCatching { import.await() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertEquals(settings, records)
        Unit
    }

    @Test
    fun `post-commit view model maintenance cannot turn a committed import into a failure`() = runBlocking {
        var committed = false

        BackupRestoreCoordinator.completeImport(
            commit = { committed = true },
            maintenance = { error("Reminder scheduling failed") },
        )

        assertTrue(committed)
    }

    @Test
    fun `post-commit maintenance propagates cancellation`() = runBlocking {
        val cancellation = CancellationException("cancelled")

        val thrown = runCatching {
            BackupRestoreCoordinator.completeImport(
                commit = { },
                maintenance = { throw cancellation },
            )
        }.exceptionOrNull()

        org.junit.Assert.assertSame(cancellation, thrown)
    }

    @Test
    fun `restore propagates cancellation instead of rolling it back as an error`() = runBlocking {
        val cancellation = CancellationException("cancelled")

        val thrown = runCatching {
            BackupRestoreCoordinator.restore(
                previousSettings = "old",
                importedSettings = "imported",
                updateSettings = { },
                replaceRecords = { },
                rescheduleReminders = { throw cancellation },
            )
        }.exceptionOrNull()

        org.junit.Assert.assertSame(cancellation, thrown)
    }

    @Test
    fun `rollback failure is reported instead of hidden`() = runBlocking {
        val recordFailure = IllegalStateException("Database failed")
        val rollbackFailure = IllegalStateException("Settings rollback failed")
        var settings = "old"

        val thrown = runCatching {
            BackupRestoreCoordinator.restore(
                previousSettings = "old",
                importedSettings = "imported",
                updateSettings = { value ->
                    if (value == "old") throw rollbackFailure
                    settings = value
                },
                replaceRecords = { throw recordFailure },
                rescheduleReminders = { },
            )
        }.exceptionOrNull()

        org.junit.Assert.assertTrue(thrown is IllegalStateException)
        org.junit.Assert.assertEquals("Database failed", thrown?.cause?.message)
        org.junit.Assert.assertSame(rollbackFailure, thrown?.suppressed?.single())
        org.junit.Assert.assertEquals("imported", settings)
    }
}
