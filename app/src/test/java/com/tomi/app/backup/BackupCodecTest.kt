package com.tomi.app.backup

import com.tomi.app.data.local.DailyCheckInEntity
import com.tomi.app.data.local.JournalEntity
import com.tomi.app.data.local.MovementEntity
import com.tomi.app.data.local.MovementType
import com.tomi.app.data.local.TaskEntity
import com.tomi.app.data.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.DayOfWeek

class BackupCodecTest {

    @Test
    fun `backup round trips tasks journal movement check-in and settings`() {
        val backup = TomiBackup(
            exportedAt = 1_800_000_000_000L,
            appVersion = "1.0.0",
            tasks = listOf(
                TaskBackup(1L, "Walk", "After lunch", 1_800_000_000_000L, 2, "Health", true, 1_800_000_000_001L, 1L, 2L),
            ),
            journal = listOf(
                JournalBackup(2L, "Good day", "Focused work", 4, "work", true, 1L, 2L),
            ),
            movements = listOf(
                MovementBackup(3L, MovementType.WALK, 1_000L, 1_120L, 120, null, ""),
            ),
            checkIns = listOf(
                CheckInBackup("2026-11-24", 4, 3, "Felt good", 2L),
            ),
            water = listOf(WaterBackup(4L, 250, 1_000L)),
            stepCache = listOf(StepCacheBackup("2026-11-24", 6_000L, 2L)),
            settings = SettingsBackup(
                remindersEnabled = true,
                intervalMinutes = 35,
                activeStartMinutes = 540,
                activeEndMinutes = 1_080,
                activeDays = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                sound = true,
                vibrate = false,
                stepGoal = 8_000,
                waterGoalMl = 2_500,
                themeMode = ThemeMode.DARK,
                dynamicColor = false,
                smartPause = true,
                autoDownloadUpdates = false,
            ),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(backup))

        assertEquals(backup, decoded)
    }

    @Test
    fun `rejects structurally incomplete backups`() {
        val incomplete = listOf(
            "{}",
            """{"version":1}""",
            """{"version":1,"settings":{}}""",
            BackupCodec.encode(TomiBackup()).replace("\"tasks\": []", "").replace(",\n\n", "\n"),
        )

        incomplete.forEach { content ->
            assertThrows("Incomplete backup: $content", IllegalArgumentException::class.java) {
                BackupCodec.decode(content)
            }
        }
    }

    @Test
    fun `accepts a structurally complete empty backup`() {
        val backup = TomiBackup()

        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)))
    }

    @Test
    fun `rejects unsupported backup versions`() {
        val json = BackupCodec.encode(TomiBackup()).replace("\"version\": 1", "\"version\": 99")

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(json) }
    }

    @Test
    fun `rejects malformed json`() {
        assertThrows(Exception::class.java) { BackupCodec.decode("not a backup") }
    }

    @Test
    fun `rejects invalid reminder settings before any data is replaced`() {
        val backup = TomiBackup(settings = SettingsBackup(intervalMinutes = 99))

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(backup)) }
    }

    @Test
    fun `rejects negative movement and health values`() {
        val backup = TomiBackup(
            movements = listOf(MovementBackup(1L, MovementType.WALK, 1_000L, 500L, -1, null, "")),
            water = listOf(WaterBackup(1L, -250, 1_000L)),
            stepCache = listOf(StepCacheBackup("2026-11-24", -5L, 1_000L)),
        )

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(backup)) }
    }

    @Test
    fun `rejects invalid check-in dates and mood values`() {
        val backup = TomiBackup(checkIns = listOf(CheckInBackup("not-a-date", 9, 3, "", 1L)))

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(backup)) }
    }

    @Test
    fun `rejects duplicate primary keys`() {
        val task = TaskBackup(1L, "Task", "", null, 2, "", false, null, 1L, 1L)
        val backup = TomiBackup(tasks = listOf(task, task))

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(backup)) }
    }

    @Test
    fun `rejects autoincrement ids that leave no safe sequence headroom`() {
        val unsafeIds = listOf(Long.MAX_VALUE, Long.MAX_VALUE - 1L)
        val backups = listOf<Pair<String, (Long) -> TomiBackup>>(
            "task" to { id -> TomiBackup(tasks = listOf(TaskBackup(id, "Task", "", null, 1, "", false, null, 1L, 1L))) },
            "journal" to { id -> TomiBackup(journal = listOf(JournalBackup(id, "Entry", "Body", null, "", false, 1L, 1L))) },
            "movement" to { id -> TomiBackup(movements = listOf(MovementBackup(id, MovementType.WALK, 1L, 2L, 1, null, ""))) },
            "water" to { id -> TomiBackup(water = listOf(WaterBackup(id, 250, 1L))) },
        )

        backups.forEach { (name, backup) ->
            unsafeIds.forEach { id ->
                assertThrows("Unsafe $name id $id", IllegalArgumentException::class.java) {
                    BackupCodec.encode(backup(id))
                }
            }
        }
    }

    @Test
    fun `accepts an autoincrement id at the safe headroom boundary`() {
        val id = Long.MAX_VALUE - 10_000L
        val backup = TomiBackup(tasks = listOf(TaskBackup(id, "Task", "", null, 1, "", false, null, 1L, 1L)))

        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)))
    }

    @Test
    fun `rejects an oversized input stream`() {
        val input = ByteArrayInputStream(ByteArray(25_000_001))

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(input) }
    }

    @Test
    fun `rejects an oversized encoded backup`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.requireEncodedSize(25_000_001L)
        }
    }

    @Test
    fun `rejects a collection over the record limit`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.requireCollectionSize(10_001)
        }
    }

    @Test
    fun `decodes bytes without an intermediate string copy`() {
        val backup = TomiBackup(tasks = listOf(TaskBackup(1L, "Task", "", null, 1, "", false, null, 1L, 1L)))

        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup).toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `maps persisted entities without changing their values`() {
        val task = TaskEntity(5L, "Task", "Notes", 10L, 1, "Work", false, null, 1L, 2L)
        val movement = MovementEntity(6L, MovementType.PUSHUPS, 10L, 70L, 60, 12, "Set")

        assertEquals(task, task.toBackup().toEntity())
        assertEquals(movement, movement.toBackup().toEntity())
        assertEquals(
            DailyCheckInEntity("2026-11-24", 3, 4, "Okay", 5L),
            DailyCheckInEntity("2026-11-24", 3, 4, "Okay", 5L).toBackup().toEntity(),
        )
    }

    @Test
    fun `rejects future-dated step cache entries`() {
        val future = System.currentTimeMillis() + 3_600_000L
        val backup = TomiBackup(stepCache = listOf(StepCacheBackup("2026-11-25", 100L, future)))

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.encode(backup)
        }
    }
}
