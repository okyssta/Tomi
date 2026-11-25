package com.tomi.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class ReminderSchedulerTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun grantExactAlarms() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val context = ApplicationProvider.getApplicationContext<Application>()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `snooze schedules ten minutes from the request time`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val settingsRepository = SettingsRepository.from(context)
        settingsRepository.updateSettings { settings ->
            settings.copy(
                reminder = settings.reminder.copy(
                    enabled = true,
                    activeStartMinutes = 0,
                    activeEndMinutes = 1_439,
                ),
            )
        }

        val before = System.currentTimeMillis()
        ReminderScheduler(context, settingsRepository, ReminderNotifier(context)).snooze(10)
        val after = System.currentTimeMillis()

        val alarm = requireNotNull(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.firstOrNull())
        assertTrue(alarm.triggerAtMs in (before + 600_000L)..(after + 600_000L))
    }

    @Test
    fun `future last reminder does not suppress alerts forever after a backward clock change`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val settingsRepository = SettingsRepository.from(context)
        settingsRepository.clearAll()
        settingsRepository.updateSettings { settings ->
            settings.copy(
                reminder = settings.reminder.copy(
                    enabled = true,
                    activeStartMinutes = 0,
                    activeEndMinutes = 1_439,
                    activeDays = com.tomi.app.domain.reminder.ReminderSettings().activeDays,
                ),
            )
        }
        val now = java.time.Instant.parse("2026-11-23T10:00:00Z")
        val futureLastReminder = now.plusSeconds(2 * 60 * 60L).toEpochMilli()
        settingsRepository.updateRuntime {
            ReminderRuntimeState(lastReminderAt = futureLastReminder, nextReminderAt = now.toEpochMilli())
        }
        val scheduler = ReminderScheduler(context, settingsRepository, ReminderNotifier(context))
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        scheduler.onAlarm(now)
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())

        scheduler.onAlarm(now.plusSeconds(40 * 60L))
        assertTrue(shadowOf(notificationManager).allNotifications.isNotEmpty())
    }

    @Test
    fun `clock change clamps reminder times without regressing the step checkpoint`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val settingsRepository = SettingsRepository.from(context)
        settingsRepository.clearAll()
        settingsRepository.updateSettings { settings ->
            settings.copy(
                reminder = settings.reminder.copy(
                    enabled = true,
                    activeStartMinutes = 0,
                    activeEndMinutes = 1_439,
                    activeDays = com.tomi.app.domain.reminder.ReminderSettings().activeDays,
                ),
            )
        }
        val now = java.time.Instant.parse("2026-11-23T10:00:00Z")
        val nowMillis = now.toEpochMilli()
        settingsRepository.updateRuntime {
            ReminderRuntimeState(
                lastMovementAt = nowMillis + 3_600_000L,
                lastStepCheckAt = nowMillis + 3_600_000L,
                snoozeUntil = nowMillis + 1_800_000L,
                lastReminderAt = nowMillis + 7_200_000L,
                nextReminderAt = nowMillis + 10_800_000L,
            )
        }

        ReminderScheduler(context, settingsRepository, ReminderNotifier(context)).onClockChanged(now)

        val runtime = settingsRepository.runtime.first()
        org.junit.Assert.assertEquals(nowMillis, runtime.lastMovementAt)
        org.junit.Assert.assertEquals(nowMillis + 3_600_000L, runtime.lastStepCheckAt)
        org.junit.Assert.assertEquals(nowMillis, runtime.snoozeUntil)
        org.junit.Assert.assertEquals(nowMillis, runtime.lastReminderAt)
        org.junit.Assert.assertTrue(runtime.nextReminderAt!! <= nowMillis + 40 * 60_000L)
    }
}
