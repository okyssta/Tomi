package com.tomi.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.tomi.app.R

object MovementAlarmChannel {
    fun id(sound: Boolean, vibrate: Boolean): String = when {
        sound && vibrate -> ID_SOUND_VIBRATE
        sound -> ID_SOUND
        vibrate -> ID_VIBRATE
        else -> ID_SILENT
    }

    const val ID_SOUND_VIBRATE = "tomi_movement_alarm_sound_vibrate"
    const val ID_SOUND = "tomi_movement_alarm_sound"
    const val ID_VIBRATE = "tomi_movement_alarm_vibrate"
    const val ID_SILENT = "tomi_movement_alarm_silent"
    const val LEGACY_ID = "movement_reminders"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val legacy = manager.getNotificationChannel(LEGACY_ID)
        fun create(id: String, nameRes: Int, sound: Boolean, vibrate: Boolean) {
            if (manager.getNotificationChannel(id) != null) return
            val newChannel = channel(context, id, nameRes, sound, vibrate, alarmSound, audioAttributes)
            if (legacy != null) {
                newChannel.importance = legacy.importance
                newChannel.setShowBadge(legacy.canShowBadge())
                newChannel.lockscreenVisibility = legacy.lockscreenVisibility
                newChannel.setBypassDnd(legacy.canBypassDnd())
                if (sound) {
                    newChannel.setSound(legacy.sound ?: alarmSound, legacy.audioAttributes ?: audioAttributes)
                }
                if (vibrate) {
                    newChannel.enableVibration(legacy.shouldVibrate())
                    if (legacy.shouldVibrate()) {
                        newChannel.setVibrationPattern(legacy.vibrationPattern ?: longArrayOf(0L, 350L, 180L, 350L))
                    }
                }
            }
            manager.createNotificationChannel(newChannel)
        }

        create(ID_SOUND_VIBRATE, R.string.channel_movement_alarm_full_name, sound = true, vibrate = true)
        create(ID_SOUND, R.string.channel_movement_alarm_sound_name, sound = true, vibrate = false)
        create(ID_VIBRATE, R.string.channel_movement_alarm_vibrate_name, sound = false, vibrate = true)
        create(ID_SILENT, R.string.channel_movement_alarm_silent_name, sound = false, vibrate = false)
    }

    private fun channel(
        context: Context,
        id: String,
        nameRes: Int,
        sound: Boolean,
        vibrate: Boolean,
        alarmSound: android.net.Uri,
        audioAttributes: AudioAttributes,
    ): NotificationChannel = NotificationChannel(id, context.getString(nameRes), NotificationManager.IMPORTANCE_HIGH).apply {
        description = context.getString(R.string.channel_movement_description)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        setSound(if (sound) alarmSound else null, if (sound) audioAttributes else null)
        enableVibration(vibrate)
        if (vibrate) setVibrationPattern(longArrayOf(0L, 350L, 180L, 350L))
    }
}
