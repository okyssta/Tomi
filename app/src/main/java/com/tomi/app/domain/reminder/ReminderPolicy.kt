package com.tomi.app.domain.reminder

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

        val startToday = atMinute(now, settings.activeStartMinutes)
        val endToday = atMinute(now, settings.activeEndMinutes)

        if (now.dayOfWeek !in settings.activeDays || now >= endToday || !startToday.isBefore(endToday)) {
            return nextActiveStart(now, settings)
        }
        if (now < startToday) return startToday
        val snooze = snoozeUntil?.atZone(now.zone)
        if (snooze != null && snooze.isAfter(now) && isActiveAt(snooze, settings)) {
            return snooze
        }

        val baseline = lastMovementAt?.coerceAtMost(now.toInstant()) ?: now.toInstant()
        val candidate = baseline.atZone(now.zone).plusMinutes(settings.intervalMinutes.toLong())

        val next = if (candidate < now) now else candidate
        return if (next < endToday && isActiveAt(next, settings)) next else nextActiveStart(now, settings)
    }

    fun isActiveAt(time: ZonedDateTime, settings: ReminderSettings): Boolean {
        if (!settings.enabled || time.dayOfWeek !in settings.activeDays) return false
        val start = atMinute(time, settings.activeStartMinutes)
        val end = atMinute(time, settings.activeEndMinutes)
        return start.isBefore(end) && !time.isBefore(start) && time.isBefore(end)
    }

    private fun nextActiveStart(now: ZonedDateTime, settings: ReminderSettings): ZonedDateTime? {
        var date = now.toLocalDate()
        repeat(8) {
            date = date.plusDays(1)
            if (date.dayOfWeek in settings.activeDays) {
                val start = atMinute(date, settings.activeStartMinutes, now.zone)
                val end = atMinute(date, settings.activeEndMinutes, now.zone)
                if (start.isBefore(end)) return start
            }
        }
        return null
    }

    private fun atMinute(now: ZonedDateTime, minuteOfDay: Int): ZonedDateTime =
        atMinute(now.toLocalDate(), minuteOfDay, now.zone)

    private fun atMinute(date: LocalDate, minuteOfDay: Int, zone: ZoneId): ZonedDateTime =
        date.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(zone)
}
