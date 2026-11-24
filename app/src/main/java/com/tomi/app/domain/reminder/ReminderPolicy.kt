package com.tomi.app.domain.reminder

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime

data class ReminderSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 40,
    val activeStartMinutes: Int = 9 * 60,
    val activeEndMinutes: Int = 18 * 60,
    val activeDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    ),
    val sound: Boolean = true,
    val vibrate: Boolean = true,
) {
    init {
        require(intervalMinutes in 30..45) { "Reminder interval must be between 30 and 45 minutes" }
        require(activeStartMinutes in 0..1_439) { "Invalid reminder start time" }
        require(activeEndMinutes in 0..1_439) { "Invalid reminder end time" }
        require(activeStartMinutes < activeEndMinutes) { "Reminder window must start before it ends" }
    }
}

object ReminderPolicy {

    fun nextReminderAt(
        now: ZonedDateTime,
        lastMovementAt: Instant?,
        settings: ReminderSettings,
        snoozeUntil: Instant? = null,
    ): ZonedDateTime? {
        if (!settings.enabled || settings.activeDays.isEmpty()) return null
        if (snoozeUntil != null && snoozeUntil.isAfter(now.toInstant())) {
            return snoozeUntil.atZone(now.zone)
        }

        val startToday = atMinute(now, settings.activeStartMinutes)
        val endToday = atMinute(now, settings.activeEndMinutes)

        if (now.dayOfWeek !in settings.activeDays || now >= endToday) {
            return nextActiveStart(now, settings)
        }
        if (now < startToday) return startToday

        val candidate = lastMovementAt
            ?.atZone(now.zone)
            ?.plusMinutes(settings.intervalMinutes.toLong())
            ?: now.plusMinutes(settings.intervalMinutes.toLong())

        val next = if (candidate < now) now else candidate
        return if (next < endToday) next else nextActiveStart(now, settings)
    }

    private fun nextActiveStart(now: ZonedDateTime, settings: ReminderSettings): ZonedDateTime? {
        var date = now.toLocalDate()
        repeat(8) {
            date = date.plusDays(1)
            if (date.dayOfWeek in settings.activeDays) {
                return date.atTime(settings.activeStartMinutes / 60, settings.activeStartMinutes % 60)
                    .atZone(now.zone)
            }
        }
        return null
    }

    private fun atMinute(now: ZonedDateTime, minuteOfDay: Int): ZonedDateTime =
        now.toLocalDate()
            .atTime(minuteOfDay / 60, minuteOfDay % 60)
            .atZone(now.zone)
}
