package com.tomi.app.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.tomi.app.domain.reminder.ReminderSettings
import java.time.DayOfWeek

val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
val REMINDER_INTERVAL = intPreferencesKey("reminder_interval_minutes")
val REMINDER_START = intPreferencesKey("reminder_start_minutes")
val REMINDER_END = intPreferencesKey("reminder_end_minutes")
val REMINDER_DAYS = intPreferencesKey("reminder_days_mask")
val REMINDER_SOUND = booleanPreferencesKey("reminder_sound")
val REMINDER_VIBRATE = booleanPreferencesKey("reminder_vibrate")
val STEP_GOAL = intPreferencesKey("step_goal")
val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
val THEME_MODE = intPreferencesKey("theme_mode")
val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
val SMART_PAUSE = booleanPreferencesKey("smart_pause")
val AUTO_DOWNLOAD_UPDATES = booleanPreferencesKey("auto_download_updates")
val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

val LAST_MOVEMENT_AT = androidx.datastore.preferences.core.longPreferencesKey("last_movement_at")
val LAST_STEP_CHECK_AT = androidx.datastore.preferences.core.longPreferencesKey("last_step_check_at")
val SNOOZE_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("snooze_until")
val LAST_REMINDER_AT = androidx.datastore.preferences.core.longPreferencesKey("last_reminder_at")
val NEXT_REMINDER_AT = androidx.datastore.preferences.core.longPreferencesKey("next_reminder_at")
val LAST_UPDATE_CHECK_AT = androidx.datastore.preferences.core.longPreferencesKey("last_update_check_at")

private val defaultDays = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

fun Preferences.toTomiSettings(): TomiSettings {
    val interval = this[REMINDER_INTERVAL]?.takeIf { it in 30..45 } ?: 40
    val start = this[REMINDER_START]?.takeIf { it in 0..1_439 } ?: 9 * 60
    val end = this[REMINDER_END]?.takeIf { it in 0..1_439 && it > start } ?: 18 * 60
    val days = if (contains(REMINDER_DAYS)) {
        val mask = this[REMINDER_DAYS] ?: 0
        DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
    } else {
        defaultDays
    }
    return TomiSettings(
        reminder = ReminderSettings(
            enabled = this[REMINDERS_ENABLED] ?: true,
            intervalMinutes = interval,
            activeStartMinutes = start,
            activeEndMinutes = end,
            activeDays = days,
            sound = this[REMINDER_SOUND] ?: true,
            vibrate = this[REMINDER_VIBRATE] ?: true,
        ),
        stepGoal = this[STEP_GOAL]?.takeIf { it in 1_000..30_000 } ?: 6_000,
        waterGoalMl = this[WATER_GOAL_ML]?.takeIf { it in 500..6_000 } ?: 2_000,
        themeMode = ThemeMode.entries.getOrNull(this[THEME_MODE] ?: 0) ?: ThemeMode.SYSTEM,
        dynamicColor = this[DYNAMIC_COLOR] ?: true,
        smartPause = this[SMART_PAUSE] ?: true,
        autoDownloadUpdates = this[AUTO_DOWNLOAD_UPDATES] ?: true,
        onboardingComplete = this[ONBOARDING_COMPLETE] ?: false,
    )
}

fun Preferences.toReminderRuntimeState(): ReminderRuntimeState = ReminderRuntimeState(
    lastMovementAt = this[LAST_MOVEMENT_AT],
    lastStepCheckAt = this[LAST_STEP_CHECK_AT],
    snoozeUntil = this[SNOOZE_UNTIL],
    lastReminderAt = this[LAST_REMINDER_AT],
    nextReminderAt = this[NEXT_REMINDER_AT],
)

fun ReminderRuntimeState.writeTo(preferences: MutablePreferences) {
    preferences.putNullable(LAST_MOVEMENT_AT, lastMovementAt)
    preferences.putNullable(LAST_STEP_CHECK_AT, lastStepCheckAt)
    preferences.putNullable(SNOOZE_UNTIL, snoozeUntil)
    preferences.putNullable(LAST_REMINDER_AT, lastReminderAt)
    preferences.putNullable(NEXT_REMINDER_AT, nextReminderAt)
}

private fun <T> MutablePreferences.putNullable(key: Preferences.Key<T>, value: T?) {
    if (value == null) remove(key) else this[key] = value
}

fun TomiSettings.writeTo(preferences: MutablePreferences) {
    preferences[REMINDERS_ENABLED] = reminder.enabled
    preferences[REMINDER_INTERVAL] = reminder.intervalMinutes
    preferences[REMINDER_START] = reminder.activeStartMinutes
    preferences[REMINDER_END] = reminder.activeEndMinutes
    preferences[REMINDER_DAYS] = reminder.activeDays.sumOf { 1 shl (it.value - 1) }
    preferences[REMINDER_SOUND] = reminder.sound
    preferences[REMINDER_VIBRATE] = reminder.vibrate
    preferences[STEP_GOAL] = stepGoal
    preferences[WATER_GOAL_ML] = waterGoalMl
    preferences[THEME_MODE] = themeMode.ordinal
    preferences[DYNAMIC_COLOR] = dynamicColor
    preferences[SMART_PAUSE] = smartPause
    preferences[AUTO_DOWNLOAD_UPDATES] = autoDownloadUpdates
    preferences[ONBOARDING_COMPLETE] = onboardingComplete
}
