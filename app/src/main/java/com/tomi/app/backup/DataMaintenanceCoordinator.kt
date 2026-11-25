package com.tomi.app.backup

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

internal object DataMaintenanceCoordinator {
    private val mutex = Mutex()

    suspend fun <T> exclusive(action: suspend () -> T): T = mutex.withLock { action() }

    suspend fun clear(
        deleteUpdateCache: () -> Boolean,
        clearDatabase: suspend () -> Unit,
        clearSettings: suspend () -> Unit,
        afterPersistentDataCleared: suspend () -> Unit = {},
    ) {
        clearDatabase()
        clearSettings()
        afterPersistentDataCleared()
        if (!deleteUpdateCache()) throw IOException("Could not remove downloaded updates")
    }
}
