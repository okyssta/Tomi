package com.tomi.app.update

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tomi.app.R
import com.tomi.app.reminder.ReminderNotifier
import com.tomi.app.ui.MainActivity

class UpdateNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UPDATES,
                context.getString(R.string.channel_updates_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_updates_description)
            },
        )
    }

    @SuppressLint("MissingPermission")
    fun showAvailable(info: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val openApp = PendingIntent.getActivity(
            context,
            3001,
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_ROUTE, "settings")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openPage = PendingIntent.getActivity(
            context,
            3002,
            Intent(Intent.ACTION_VIEW, info.pageUrl.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            NOTIFICATION_UPDATE,
            NotificationCompat.Builder(context, ReminderNotifier.CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_stat_tomi)
                .setContentTitle(context.getString(R.string.update_available_title, info.versionName))
                .setContentText(context.getString(R.string.update_available_text))
                .setContentIntent(openApp)
                .addAction(0, context.getString(R.string.action_open_release), openPage)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val CHANNEL_UPDATES = ReminderNotifier.CHANNEL_UPDATES
        private const val NOTIFICATION_UPDATE = 3003
    }
}
