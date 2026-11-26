package com.tomi.app.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import com.tomi.app.domain.prayer.TrackedPrayer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: TomiBackup): String {
        validate(backup)
        val encoded = json.encodeToString(TomiBackup.serializer(), backup)
        requireEncodedSize(encoded.toByteArray(Charsets.UTF_8).size.toLong())
        return encoded
    }

    fun decode(input: InputStream): TomiBackup {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_BACKUP_BYTES) { "Backup is too large" }
            output.write(buffer, 0, read)
        }
        return decode(output.toByteArray())
    }

    fun decode(content: String): TomiBackup {
        requireEncodedSize(content.toByteArray(Charsets.UTF_8).size.toLong())
        return decodeJson(content)
    }

    fun decode(bytes: ByteArray): TomiBackup {
        requireEncodedSize(bytes.size.toLong())
        return decodeJson(bytes.decodeToString())
    }

    private fun decodeJson(content: String): TomiBackup {
        val root = try {
            json.parseToJsonElement(content).jsonObject
        } catch (_: SerializationException) {
            throw IllegalArgumentException("Invalid Tomi backup")
        }
        require(REQUIRED_FIELDS.all(root::containsKey)) { "Incomplete Tomi backup" }
        val backup = try {
            json.decodeFromJsonElement(TomiBackup.serializer(), root)
        } catch (_: SerializationException) {
            throw IllegalArgumentException("Invalid Tomi backup")
        }
        require(backup.version == VERSION) { "Unsupported backup version" }
        validate(backup)
        return backup
    }

    private fun validate(backup: TomiBackup) {
        listOf(
            backup.tasks.size,
            backup.journal.size,
            backup.movements.size,
            backup.checkIns.size,
            backup.water.size,
            backup.stepCache.size,
            backup.prayers.size,
        ).forEach(::requireCollectionSize)
        backup.settings.toEntity()
        require(backup.settings.stepGoal in 1_000..30_000) { "Invalid step goal" }
        require(backup.settings.waterGoalMl in 500..6_000) { "Invalid water goal" }

        requireUnique(backup.tasks.map { it.id }) { "Duplicate task id" }
        backup.tasks.forEach { task ->
            requireSafeId(task.id) { "Invalid task id" }
            require(task.title.isNotBlank()) { "Invalid task" }
            require(task.priority in 0..2) { "Invalid task priority" }
            require(task.createdAt >= 0L && task.updatedAt >= 0L) { "Invalid task timestamp" }
            require(task.dueAt == null || task.dueAt >= 0L) { "Invalid task due date" }
            require(task.completedAt == null || task.completedAt >= 0L) { "Invalid task completion date" }
        }

        requireUnique(backup.journal.map { it.id }) { "Duplicate journal id" }
        backup.journal.forEach { entry ->
            requireSafeId(entry.id) { "Invalid journal id" }
            require(entry.title.isNotBlank() || entry.body.isNotBlank()) { "Invalid journal entry" }
            require(entry.mood == null || entry.mood in 1..5) { "Invalid journal mood" }
            require(entry.createdAt >= 0L && entry.updatedAt >= 0L) { "Invalid journal timestamp" }
        }

        requireUnique(backup.movements.map { it.id }) { "Duplicate movement id" }
        backup.movements.forEach { movement ->
            requireSafeId(movement.id) { "Invalid movement id" }
            require(movement.startedAt >= 0L && movement.endedAt >= movement.startedAt) { "Invalid movement time" }
            require(movement.durationSeconds >= 0) { "Invalid movement duration" }
            require(movement.reps == null || movement.reps >= 0) { "Invalid rep count" }
        }

        requireUnique(backup.checkIns.map { it.date }) { "Duplicate check-in date" }
        backup.checkIns.forEach { checkIn ->
            requireValidDate(checkIn.date)
            require(checkIn.mood in 1..5 && checkIn.energy in 1..5) { "Invalid check-in values" }
            require(checkIn.updatedAt >= 0L) { "Invalid check-in timestamp" }
        }

        requireUnique(backup.water.map { it.id }) { "Duplicate water id" }
        backup.water.forEach { water ->
            requireSafeId(water.id) { "Invalid water id" }
            require(water.amountMl > 0 && water.timestamp >= 0L) { "Invalid water entry" }
        }

        requireUnique(backup.stepCache.map { it.date }) { "Duplicate step date" }
        backup.stepCache.forEach { steps ->
            requireValidDate(steps.date)
            require(steps.steps >= 0L && steps.updatedAt >= 0L) { "Invalid step entry" }
            require(steps.updatedAt <= System.currentTimeMillis() + MAX_STEP_CLOCK_SKEW_MS) {
                "Step cache timestamp is in the future"
            }
        }

        val prayerNames = TrackedPrayer.entries.map { it.name }.toSet()
        requireUnique(backup.prayers.map { it.date to it.prayer }) { "Duplicate prayer entry" }
        backup.prayers.forEach { prayer ->
            requireValidDate(prayer.date)
            require(prayer.prayer in prayerNames) { "Invalid prayer" }
            require(prayer.updatedAt >= 0L) { "Invalid prayer timestamp" }
        }
    }

    private fun requireSafeId(id: Long, lazyMessage: () -> String) {
        require(id in 1L..MAX_SAFE_AUTOINCREMENT_ID, lazyMessage)
    }

    private fun requireValidDate(value: String) {
        require(runCatching { LocalDate.parse(value) }.isSuccess) { "Invalid date: $value" }
    }

    private fun requireUnique(values: List<Any>, lazyMessage: () -> String) {
        require(values.toSet().size == values.size, lazyMessage)
    }

    internal fun requireEncodedSize(size: Long) {
        require(size in 0..MAX_BACKUP_BYTES) { "Backup is too large" }
    }

    internal fun requireCollectionSize(size: Int) {
        require(size in 0..MAX_COLLECTION_ITEMS) { "Backup contains too many records" }
    }

    private const val VERSION = 1
    private val REQUIRED_FIELDS = setOf(
        "version",
        "tasks",
        "journal",
        "movements",
        "checkIns",
        "water",
        "stepCache",
        "settings",
    )
    private const val MAX_BACKUP_BYTES = 25_000_000
    private const val MAX_COLLECTION_ITEMS = 10_000
    private const val MAX_STEP_CLOCK_SKEW_MS = 300_000L
    // Leave room for future AUTOINCREMENT inserts instead of poisoning sqlite_sequence at Long.MAX_VALUE.
    private const val AUTOINCREMENT_HEADROOM = 10_000L
    private const val MAX_SAFE_AUTOINCREMENT_ID = Long.MAX_VALUE - AUTOINCREMENT_HEADROOM
}
