package com.tomi.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomi.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val scheduler = ReminderScheduler(
                    context,
                    SettingsRepository.from(context),
                    ReminderNotifier(context),
                )
                when (intent.action) {
                    ReminderScheduler.ACTION_REMIND -> scheduler.onAlarm()
                    ReminderScheduler.ACTION_SNOOZE -> scheduler.snooze(10)
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
        ) return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                ReminderScheduler(
                    context,
                    SettingsRepository.from(context),
                    ReminderNotifier(context),
                ).reschedule()
                ReminderWorker.enqueue(context)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
