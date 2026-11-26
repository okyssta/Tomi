package com.tomi.app.backup

import com.tomi.app.data.local.DailyCheckInEntity
import com.tomi.app.data.local.JournalEntity
import com.tomi.app.data.local.MovementEntity
import com.tomi.app.data.local.MovementType
import com.tomi.app.data.local.PrayerLogEntity
import com.tomi.app.data.local.StepCacheEntity
import com.tomi.app.data.local.TaskEntity
import com.tomi.app.data.local.WaterEntity
import com.tomi.app.data.settings.ThemeMode
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.domain.reminder.ReminderSettings
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
data class TomiBackup(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val appVersion: String = "",
    val tasks: List<TaskBackup> = emptyList(),
    val journal: List<JournalBackup> = emptyList(),
    val movements: List<MovementBackup> = emptyList(),
    val checkIns: List<CheckInBackup> = emptyList(),
    val water: List<WaterBackup> = emptyList(),
    val stepCache: List<StepCacheBackup> = emptyList(),
    val prayers: List<PrayerBackup> = emptyList(),
    val settings: SettingsBackup = SettingsBackup(),
)

@Serializable
data class TaskBackup(
    val id: Long,
    val title: String,
    val notes: String,
    val dueAt: Long?,
    val priority: Int,
    val category: String,
    val completed: Boolean,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class JournalBackup(
    val id: Long,
    val title: String,
    val body: String,
    val mood: Int?,
    val tags: String,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class MovementBackup(
    val id: Long,
    val type: MovementType,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Int,
    val reps: Int?,
    val note: String,
)

@Serializable
data class CheckInBackup(
    val date: String,
    val mood: Int,
    val energy: Int,
    val note: String,
    val updatedAt: Long,
)

@Serializable
data class WaterBackup(val id: Long, val amountMl: Int, val timestamp: Long)

@Serializable
data class StepCacheBackup(val date: String, val steps: Long, val updatedAt: Long)

@Serializable
data class PrayerBackup(
    val date: String,
    val prayer: String,
    val offered: Boolean,
    val updatedAt: Long,
)

@Serializable
data class SettingsBackup(
    val remindersEnabled: Boolean = true,
    val intervalMinutes: Int = 40,
    val activeStartMinutes: Int = 540,
    val activeEndMinutes: Int = 1_080,
    val activeDays: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    ),
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val stepGoal: Int = 6_000,
    val waterGoalMl: Int = 2_000,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val smartPause: Boolean = true,
    val autoDownloadUpdates: Boolean = true,
    val addonsEnabled: List<String> = emptyList(),
    val prayerMethod: String = "KARACHI",
    val prayerMadhab: String = "HANAFI",
)

fun TaskEntity.toBackup() = TaskBackup(id, title, notes, dueAt, priority, category, completed, completedAt, createdAt, updatedAt)
fun TaskBackup.toEntity() = TaskEntity(id, title, notes, dueAt, priority, category, completed, completedAt, createdAt, updatedAt)

fun JournalEntity.toBackup() = JournalBackup(id, title, body, mood, tags, pinned, createdAt, updatedAt)
fun JournalBackup.toEntity() = JournalEntity(id, title, body, mood, tags, pinned, createdAt, updatedAt)

fun MovementEntity.toBackup() = MovementBackup(id, type, startedAt, endedAt, durationSeconds, reps, note)
fun MovementBackup.toEntity() = MovementEntity(id, type, startedAt, endedAt, durationSeconds, reps, note)

fun DailyCheckInEntity.toBackup() = CheckInBackup(date, mood, energy, note, updatedAt)
fun CheckInBackup.toEntity() = DailyCheckInEntity(date, mood, energy, note, updatedAt)

fun WaterEntity.toBackup() = WaterBackup(id, amountMl, timestamp)
fun WaterBackup.toEntity() = WaterEntity(id, amountMl, timestamp)

fun StepCacheEntity.toBackup() = StepCacheBackup(date, steps, updatedAt)
fun StepCacheBackup.toEntity() = StepCacheEntity(date, steps, updatedAt)

fun PrayerLogEntity.toBackup() = PrayerBackup(date, prayer, offered, updatedAt)
fun PrayerBackup.toEntity() = PrayerLogEntity(date, prayer, offered, updatedAt)

fun TomiSettings.toBackup() = SettingsBackup(
    remindersEnabled = reminder.enabled,
    intervalMinutes = reminder.intervalMinutes,
    activeStartMinutes = reminder.activeStartMinutes,
    activeEndMinutes = reminder.activeEndMinutes,
    activeDays = reminder.activeDays.toList(),
    sound = reminder.sound,
    vibrate = reminder.vibrate,
    stepGoal = stepGoal,
    waterGoalMl = waterGoalMl,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
    smartPause = smartPause,
    autoDownloadUpdates = autoDownloadUpdates,
    addonsEnabled = addonsEnabled.toList(),
    prayerMethod = prayerMethod,
    prayerMadhab = prayerMadhab,
)

fun SettingsBackup.toEntity(): TomiSettings = TomiSettings(
    reminder = ReminderSettings(
        enabled = remindersEnabled,
        intervalMinutes = intervalMinutes,
        activeStartMinutes = activeStartMinutes,
        activeEndMinutes = activeEndMinutes,
        activeDays = activeDays.toSet(),
        sound = sound,
        vibrate = vibrate,
    ),
    stepGoal = stepGoal,
    waterGoalMl = waterGoalMl,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
    smartPause = smartPause,
    autoDownloadUpdates = autoDownloadUpdates,
    addonsEnabled = addonsEnabled.toSet(),
    prayerMethod = prayerMethod,
    prayerMadhab = prayerMadhab,
    onboardingComplete = true,
)
