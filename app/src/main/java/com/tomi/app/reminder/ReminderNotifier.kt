package com.tomi.app.reminder

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tomi.app.R
import com.tomi.app.domain.reminder.ReminderSettings
import com.tomi.app.ui.MainActivity

class ReminderNotifier(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        val channel = NotificationChannel(
            CHANNEL_MOVEMENT,
            context.getString(R.string.channel_movement_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_movement_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showMovementReminder(settings: ReminderSettings) {
        if (!canPostNotifications()) return
        val startBreakIntent = PendingIntent.getActivity(
            context,
            REQUEST_START,
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_ROUTE, "move/WALK")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_SNOOZE,
            Intent(context, ReminderReceiver::class.java).apply { action = ReminderScheduler.ACTION_SNOOZE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, CHANNEL_MOVEMENT)
            .setSmallIcon(R.drawable.ic_stat_tomi)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.reminder_big_text),
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(startBreakIntent)
            .addAction(0, context.getString(R.string.action_start_break), startBreakIntent)
            .addAction(0, context.getString(R.string.action_snooze_10), snoozeIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .apply {
                if (settings.sound) {
                    setSound(defaultSound)
                } else {
                    setSound(null)
                }
                if (settings.vibrate) {
                    setVibrate(longArrayOf(0L, 180L, 120L, 180L))
                } else {
                    setVibrate(null)
                }
            }
            .build()
        notificationManager.notify(NOTIFICATION_MOVEMENT, notification)
    }

    @SuppressLint("MissingPermission")
    fun showTestReminder() {
        if (!canPostNotifications()) return
        notificationManager.notify(
            NOTIFICATION_TEST,
            NotificationCompat.Builder(context, CHANNEL_MOVEMENT)
                .setSmallIcon(R.drawable.ic_stat_tomi)
                .setContentTitle(context.getString(R.string.reminder_test_title))
                .setContentText(context.getString(R.string.reminder_test_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun cancelMovementReminder() {
        notificationManager.cancel(NOTIFICATION_MOVEMENT)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_MOVEMENT = "movement_reminders"
        const val CHANNEL_UPDATES = "updates"
        private const val NOTIFICATION_MOVEMENT = 2001
        private const val NOTIFICATION_TEST = 2002
        private const val REQUEST_START = 2003
        private const val REQUEST_SNOOZE = 2004
    }
}
