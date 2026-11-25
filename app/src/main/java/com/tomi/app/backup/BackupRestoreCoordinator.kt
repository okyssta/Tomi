package com.tomi.app.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal object BackupRestoreCoordinator {
    suspend fun completeImport(
        commit: suspend () -> Unit,
        maintenance: suspend () -> Unit,
    ) {
        commit()
        ignoreMaintenanceFailure { maintenance() }
    }

    suspend fun <T> restore(
        previousSettings: T,
        importedSettings: T,
        updateSettings: suspend (T) -> Unit,
        replaceRecords: suspend () -> Unit,
        rescheduleReminders: suspend () -> Unit,
    ) {
        var committed = false
        try {
            currentCoroutineContext().ensureActive()
            // DataStore and Room cannot share a physical transaction. Keep the commit
            // window non-cancellable so cancellation cannot split the two writes.
            withContext(NonCancellable) {
                updateSettings(importedSettings)
                replaceRecords()
                committed = true
            }
            currentCoroutineContext().ensureActive()
            // Records and settings are already committed. A scheduler failure must not
            // report a failed import; startup will retry reminder scheduling.
            ignoreMaintenanceFailure { rescheduleReminders() }
        } catch (error: CancellationException) {
            if (!committed) failImport(error, previousSettings, updateSettings)
            throw error
        } catch (error: Exception) {
            if (!committed) failImport(error, previousSettings, updateSettings)
            throw error
        }
    }

    private suspend fun <T> failImport(
        error: Throwable,
        previousSettings: T,
        updateSettings: suspend (T) -> Unit,
    ): Nothing {
        val rollbackFailure = withContext(NonCancellable) {
            runCatching { updateSettings(previousSettings) }.exceptionOrNull()
        }
        if (rollbackFailure != null) {
            throw IllegalStateException("Import failed and settings rollback also failed", error).apply {
                addSuppressed(rollbackFailure)
            }
        }
        throw error
    }


    private inline fun ignoreMaintenanceFailure(block: () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The import is already committed; maintenance can be retried later.
        }
    }
}
