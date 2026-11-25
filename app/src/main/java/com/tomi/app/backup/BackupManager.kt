package com.tomi.app.backup

import android.content.Context
import androidx.room.withTransaction
import com.tomi.app.BuildConfig
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.reminder.ReminderNotifier
import com.tomi.app.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream

class BackupManager(
    private val context: Context,
    private val database: TomiDatabase = TomiDatabase.get(context),
    private val settingsRepository: SettingsRepository = SettingsRepository.from(context),
    private val rescheduleReminders: suspend () -> Unit = {
        ReminderScheduler(context, settingsRepository, ReminderNotifier(context)).reschedule()
    },
) {
    suspend fun export(): String = withContext(Dispatchers.IO) {
        database.withTransaction {
            BackupCodec.encode(
                TomiBackup(
                    exportedAt = System.currentTimeMillis(),
                    appVersion = BuildConfig.VERSION_NAME,
                    tasks = database.taskDao().observeAll().first().map { it.toBackup() },
                    journal = database.journalDao().observeAll().first().map { it.toBackup() },
                    movements = database.movementDao().observeRecent(Int.MAX_VALUE).first().map { it.toBackup() },
                    checkIns = database.checkInDao().observeRecent(Int.MAX_VALUE).first().map { it.toBackup() },
                    water = database.waterDao().observeAll().first().map { it.toBackup() },
                    stepCache = database.stepCacheDao().observeAll().first().map { it.toBackup() },
                    settings = settingsRepository.settings.first().toBackup(),
                ),
            )
        }
    }

    suspend fun import(content: String) = withContext(Dispatchers.IO) {
        restore(BackupCodec.decode(content))
    }

    suspend fun import(input: InputStream) = withContext(Dispatchers.IO) {
        restore(BackupCodec.decode(input))
    }

    private suspend fun restore(backup: TomiBackup) {
        val previousSettings = settingsRepository.settings.first()
        val importedSettings = backup.settings.toEntity()
        val importedAt = System.currentTimeMillis()
        BackupRestoreCoordinator.restore(
            previousSettings = previousSettings,
            importedSettings = importedSettings,
            updateSettings = { imported -> settingsRepository.updateSettings { imported } },
            replaceRecords = {
                database.withTransaction {
                    database.taskDao().deleteAll()
                    database.journalDao().deleteAll()
                    database.movementDao().deleteAll()
                    database.checkInDao().deleteAll()
                    database.waterDao().deleteAll()
                    database.stepCacheDao().deleteAll()

                    backup.tasks.forEach { database.taskDao().insert(it.toEntity()) }
                    backup.journal.forEach { database.journalDao().insert(it.toEntity()) }
                    backup.movements.forEach { database.movementDao().insert(it.toEntity()) }
                    backup.checkIns.forEach { database.checkInDao().upsert(it.toEntity()) }
                    backup.water.forEach { database.waterDao().insert(it.toEntity()) }
                    backup.stepCache.forEach { entry ->
                        val entity = entry.toEntity()
                        database.stepCacheDao().upsert(entity.copy(updatedAt = minOf(entity.updatedAt, importedAt)))
                    }
                }
            },
            rescheduleReminders = rescheduleReminders,
        )
    }
}
