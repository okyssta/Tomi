package com.tomi.app.reminder

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class MovementAlarmChannelTest {

    @Before
    fun clearChannels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            MovementAlarmChannel.ID_SOUND_VIBRATE,
            MovementAlarmChannel.ID_SOUND,
            MovementAlarmChannel.ID_VIBRATE,
            MovementAlarmChannel.ID_SILENT,
            MovementAlarmChannel.LEGACY_ID,
        ).forEach(manager::deleteNotificationChannel)
    }

    @Test
    fun `sound and vibration use the full alarm channel`() {
        assertEquals("tomi_movement_alarm_sound_vibrate", MovementAlarmChannel.id(sound = true, vibrate = true))
    }

    @Test
    fun `sound only uses the audible alarm channel`() {
        assertEquals("tomi_movement_alarm_sound", MovementAlarmChannel.id(sound = true, vibrate = false))
    }

    @Test
    fun `vibration only uses the vibrating alarm channel`() {
        assertEquals("tomi_movement_alarm_vibrate", MovementAlarmChannel.id(sound = false, vibrate = true))
    }

    @Test
    fun `silent mode uses the silent alarm channel`() {
        assertEquals("tomi_movement_alarm_silent", MovementAlarmChannel.id(sound = false, vibrate = false))
    }

    @Test
    fun `alarm channels apply sound vibration and alarm audio usage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MovementAlarmChannel.ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)

        val full = requireNotNull(manager.getNotificationChannel(MovementAlarmChannel.ID_SOUND_VIBRATE))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, full.importance)
        assertNotNull(full.sound)
        assertEquals(AudioAttributes.USAGE_ALARM, full.audioAttributes.usage)
        assertTrue(full.shouldVibrate())

        val sound = requireNotNull(manager.getNotificationChannel(MovementAlarmChannel.ID_SOUND))
        assertNotNull(sound.sound)
        assertFalse(sound.shouldVibrate())

        val vibrate = requireNotNull(manager.getNotificationChannel(MovementAlarmChannel.ID_VIBRATE))
        assertNull(vibrate.sound)
        assertTrue(vibrate.shouldVibrate())

        val silent = requireNotNull(manager.getNotificationChannel(MovementAlarmChannel.ID_SILENT))
        assertNull(silent.sound)
        assertFalse(silent.shouldVibrate())
    }

    @Test
    fun `new channels inherit legacy user importance and vibration preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(MovementAlarmChannel.LEGACY_ID, "Legacy", NotificationManager.IMPORTANCE_LOW).apply {
                enableVibration(true)
            },
        )

        MovementAlarmChannel.ensureChannels(context)

        val migrated = requireNotNull(manager.getNotificationChannel(MovementAlarmChannel.ID_SOUND_VIBRATE))
        assertEquals(NotificationManager.IMPORTANCE_LOW, migrated.importance)
        assertTrue(migrated.shouldVibrate())
    }

    @Test
    fun `movement reminders alert again while an older notification remains`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ReminderNotifier(context).showMovementReminder(com.tomi.app.domain.reminder.ReminderSettings())

        val notification = shadowOf(manager).allNotifications.single()
        assertEquals(0, notification.flags and android.app.Notification.FLAG_ONLY_ALERT_ONCE)
    }
}
