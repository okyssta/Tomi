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
    fun `rejects unsupported backup versions`() {
        val json = BackupCodec.encode(TomiBackup()).replace("\"version\": 1", "\"version\": 99")

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(json) }
    }

    @Test
    fun `rejects malformed json`() {
        assertThrows(Exception::class.java) { BackupCodec.decode("not a backup") }
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
}
