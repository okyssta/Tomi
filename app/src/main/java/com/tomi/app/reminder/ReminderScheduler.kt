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
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

class ReminderScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val notifier: ReminderNotifier,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun reschedule(now: Instant = Instant.now()) = ReminderSchedulerLock.mutex.withLock {
        rescheduleLocked(now)
    }

    private suspend fun rescheduleLocked(now: Instant) {
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
            cancelLocked()
            settingsRepository.updateRuntime { it.copy(nextReminderAt = null) }
            runCatching { TomiWidget().updateAll(context) }
            return
        }

        schedule(next.toInstant().toEpochMilli())
        settingsRepository.updateRuntime { it.copy(nextReminderAt = next.toInstant().toEpochMilli()) }
        runCatching { TomiWidget().updateAll(context) }
    }

    suspend fun onClockChanged(now: Instant = Instant.now()) = MovementCheckpointLock.mutex.withLock {
        val nowMillis = now.toEpochMilli()
        settingsRepository.updateRuntime { current ->
            current.copy(
                lastMovementAt = current.lastMovementAt?.coerceAtMost(nowMillis),
                snoozeUntil = current.snoozeUntil?.coerceAtMost(nowMillis),
                lastReminderAt = current.lastReminderAt?.coerceAtMost(nowMillis),
                nextReminderAt = current.nextReminderAt?.coerceAtMost(nowMillis),
            )
        }
        ReminderSchedulerLock.mutex.withLock { rescheduleLocked(now) }
    }

    suspend fun onAlarm(now: Instant = Instant.now()) = ReminderSchedulerLock.mutex.withLock {
        onAlarmLocked(now)
    }

    private suspend fun onAlarmLocked(now: Instant) {
        val settings = settingsRepository.settings.first()
        val runtime = settingsRepository.runtime.first()
        if (!settings.reminder.enabled || settings.reminder.activeDays.isEmpty()) {
            cancelLocked()
            settingsRepository.updateRuntime { it.copy(nextReminderAt = null) }
            return
        }
        if (!ReminderPolicy.isActiveAt(now.atZone(ZoneId.systemDefault()), settings.reminder)) {
            rescheduleLocked(now)
            return
        }

        val scheduledFor = runtime.nextReminderAt
        if (scheduledFor != null && now.toEpochMilli() < scheduledFor - 30_000L) {
            rescheduleLocked(now)
            return
        }
        val nowMillis = now.toEpochMilli()
        if (runtime.lastReminderAt != null && runtime.lastReminderAt > nowMillis) {
            settingsRepository.updateRuntime { it.copy(lastReminderAt = nowMillis) }
        }
        val lastShown = runtime.lastReminderAt?.coerceAtMost(nowMillis)
        if (lastShown != null && nowMillis - lastShown < 5 * 60_000L) {
            rescheduleLocked(now)
            return
        }

        notifier.showMovementReminder(settings.reminder)
        settingsRepository.updateRuntime { it.copy(lastReminderAt = nowMillis) }
        rescheduleLocked(now)
    }

    suspend fun checkAndNotifyIfDue(now: Instant = Instant.now()) = ReminderSchedulerLock.mutex.withLock {
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
            onAlarmLocked(now)
        } else {
            rescheduleLocked(now)
        }
    }

    suspend fun snooze(minutes: Int = 10) = ReminderSchedulerLock.mutex.withLock {
        require(minutes in 1..60)
        val now = Instant.now()
        val until = now.plusSeconds(minutes * 60L)
        settingsRepository.updateRuntime { it.copy(snoozeUntil = until.toEpochMilli()) }
        // Evaluate from the request time so the future snooze occurrence is selected.
        rescheduleLocked(now)
        notifier.cancelMovementReminder()
    }

    suspend fun cancel() = ReminderSchedulerLock.mutex.withLock {
        cancelLocked()
    }

    private fun cancelLocked() {
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
