package com.tomi.app.health

import com.tomi.app.data.local.StepCacheDao
import com.tomi.app.data.local.StepCacheEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object StepCacheWriter {
    private val mutex = Mutex()

    suspend fun write(dao: StepCacheDao, entry: StepCacheEntity) = mutex.withLock {
        val existing = dao.observeDay(entry.date).first()
        if (existing == null || entry.updatedAt > existing.updatedAt) {
            dao.upsert(entry)
        }
    }
}
