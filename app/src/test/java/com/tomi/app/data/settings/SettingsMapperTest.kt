package com.tomi.app.data.settings

import com.tomi.app.domain.reminder.ReminderSettings
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class SettingsMapperTest {

    @Test
    fun `empty preferences produce safe defaults`() {
        val settings = emptyPreferences().toTomiSettings()

        assertEquals(40, settings.reminder.intervalMinutes)
        assertEquals(6_000, settings.stepGoal)
        assertEquals(2_000, settings.waterGoalMl)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.onboardingComplete)
        assertTrue(settings.autoDownloadUpdates)
    }

    @Test
    fun `settings round trip without losing reminder days or goals`() {
        val original = TomiSettings(
            reminder = ReminderSettings(
                enabled = false,
                intervalMinutes = 45,
                activeStartMinutes = 8 * 60,
                activeEndMinutes = 20 * 60,
                activeDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                sound = false,
                vibrate = false,
            ),
            stepGoal = 8_500,
            waterGoalMl = 2_500,
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            smartPause = false,
            autoDownloadUpdates = false,
            onboardingComplete = true,
        )
        val preferences = mutablePreferencesOf()
        original.writeTo(preferences)

        val restored = preferences.toPreferences().toTomiSettings()

        assertEquals(original, restored)
    }

    @Test
    fun `corrupt persisted values fall back to defaults`() {
        val preferences = preferencesOf(
            REMINDER_INTERVAL to 99,
            STEP_GOAL to -10,
            WATER_GOAL_ML to 0,
            REMINDER_DAYS to 0,
        )

        val settings = preferences.toTomiSettings()

        assertEquals(40, settings.reminder.intervalMinutes)
        assertEquals(6_000, settings.stepGoal)
        assertEquals(2_000, settings.waterGoalMl)
        assertTrue(settings.reminder.activeDays.isEmpty())
    }

    @Test
    fun `reminder runtime timestamps round trip and clear correctly`() {
        val preferences = mutablePreferencesOf()
        val state = ReminderRuntimeState(
            lastMovementAt = 100L,
            lastStepCheckAt = 200L,
            snoozeUntil = 300L,
            lastReminderAt = 400L,
            nextReminderAt = 500L,
        )
        state.writeTo(preferences)

        assertEquals(state, preferences.toPreferences().toReminderRuntimeState())

        state.copy(lastMovementAt = null, snoozeUntil = null).writeTo(preferences)
        val cleared = preferences.toPreferences().toReminderRuntimeState()
        assertEquals(null, cleared.lastMovementAt)
        assertEquals(null, cleared.snoozeUntil)
        assertEquals(200L, cleared.lastStepCheckAt)
    }
}
