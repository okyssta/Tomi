package com.tomi.app.health

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayBounds(val start: Instant, val endExclusive: Instant)

object StepSyncPlanner {
    fun dayBounds(date: LocalDate, zone: ZoneId): DayBounds = DayBounds(
        start = date.atStartOfDay(zone).toInstant(),
        endExclusive = date.plusDays(1).atStartOfDay(zone).toInstant(),
    )

    fun lastDays(today: LocalDate, count: Int): List<LocalDate> {
        require(count > 0)
        return (count - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    fun queryEnd(date: LocalDate, today: LocalDate, start: Instant, endExclusive: Instant, now: Instant): Instant? {
        if (date != today) return endExclusive
        if (!now.isAfter(start)) return null
        return minOf(endExclusive, now)
    }
}
