package com.tomi.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.domain.reminder.ReminderPolicy
import com.tomi.app.widget.TomiWidget
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

class ReminderScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val notifier: ReminderNotifier,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun reschedule(now: Instant = Instant.now()) {
        val settings = settingsRepository.settings.first()
        val runtime = settingsRepository.runtime.first()
        val baseline = listOfNotNull(runtime.lastMovementAt, runtime.lastReminderAt).maxOrNull()
        val next = ReminderPolicy.nextReminderAt(
            now = now.atZone(ZoneId.systemDefault()),
            lastMovementAt = baseline?.let(Instant::ofEpochMilli),
            settings = settings.reminder,
            snoozeUntil = runtime.snoozeUntil?.let(Instant::ofEpochMilli),
        )

        if (next == null) {
            cancel()
            settingsRepository.updateRuntime { it.copy(nextReminderAt = null) }
            runCatching { TomiWidget().updateAll(context) }
            return
        }

        schedule(next.toInstant().toEpochMilli())
        settingsRepository.updateRuntime { it.copy(nextReminderAt = next.toInstant().toEpochMilli()) }
        runCatching { TomiWidget().updateAll(context) }
    }

    suspend fun onAlarm(now: Instant = Instant.now()) {
        val settings = settingsRepository.settings.first()
        val runtime = settingsRepository.runtime.first()
        if (!settings.reminder.enabled || settings.reminder.activeDays.isEmpty()) {
            cancel()
            settingsRepository.updateRuntime { it.copy(nextReminderAt = null) }
            return
        }

        val scheduledFor = runtime.nextReminderAt
        if (scheduledFor != null && now.toEpochMilli() < scheduledFor - 30_000L) {
            reschedule(now)
            return
        }
        val lastShown = runtime.lastReminderAt
        if (lastShown != null && now.toEpochMilli() - lastShown < 5 * 60_000L) {
            reschedule(now)
            return
        }

        notifier.showMovementReminder(settings.reminder)
        settingsRepository.updateRuntime { it.copy(lastReminderAt = now.toEpochMilli()) }
        reschedule(now)
    }

    suspend fun checkAndNotifyIfDue(now: Instant = Instant.now()) {
        val settings = settingsRepository.settings.first()
        val runtime = settingsRepository.runtime.first()
        val baseline = listOfNotNull(runtime.lastMovementAt, runtime.lastReminderAt).maxOrNull()
        val next = ReminderPolicy.nextReminderAt(
            now = now.atZone(ZoneId.systemDefault()),
            lastMovementAt = baseline?.let(Instant::ofEpochMilli),
            settings = settings.reminder,
            snoozeUntil = runtime.snoozeUntil?.let(Instant::ofEpochMilli),
        )
        if (next != null && !next.toInstant().isAfter(now)) {
            onAlarm(now)
        } else {
            reschedule(now)
        }
    }

    suspend fun snooze(minutes: Int = 10) {
        require(minutes in 1..60)
        val until = Instant.now().plusSeconds(minutes * 60L)
        settingsRepository.updateRuntime { it.copy(snoozeUntil = until.toEpochMilli()) }
        reschedule(until)
        notifier.cancelMovementReminder()
    }

    fun cancel() {
        alarmManager?.cancel(reminderIntent())
    }

    private fun schedule(triggerAtMillis: Long) {
        val pendingIntent = reminderIntent()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager?.canScheduleExactAlarms() == true -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            else -> alarmManager?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    private fun reminderIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMIND },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_REMIND = "com.tomi.app.action.REMIND"
        const val ACTION_SNOOZE = "com.tomi.app.action.SNOOZE"
        private const val REQUEST_CODE = 1001
    }
}
