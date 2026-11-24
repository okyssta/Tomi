package com.tomi.app.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: TomiBackup): String = json.encodeToString(TomiBackup.serializer(), backup)

    fun decode(content: String): TomiBackup {
        require(content.length <= MAX_BACKUP_BYTES) { "Backup is too large" }
        val backup = try {
            json.decodeFromString(TomiBackup.serializer(), content)
        } catch (_: SerializationException) {
            throw IllegalArgumentException("Invalid Tomi backup")
        }
        require(backup.version == VERSION) { "Unsupported backup version" }
        require(backup.tasks.size <= MAX_RECORDS) { "Too many tasks in backup" }
        require(backup.journal.size <= MAX_RECORDS) { "Too many journal entries in backup" }
        require(backup.movements.size <= MAX_RECORDS) { "Too many movement sessions in backup" }
        require(backup.water.size <= MAX_RECORDS) { "Too many water entries in backup" }
        return backup
    }

    private const val VERSION = 1
    private const val MAX_BACKUP_BYTES = 5_000_000
    private const val MAX_RECORDS = 10_000
}
