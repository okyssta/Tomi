package com.tomi.app.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class StepSyncPlannerTest {

    @Test
    fun `day bounds follow the device timezone across daylight saving changes`() {
        val zone = ZoneId.of("America/New_York")
        val date = LocalDate.of(2026, 3, 8)

        val (start, end) = StepSyncPlanner.dayBounds(date, zone)

        assertEquals(date.atStartOfDay(zone).toInstant(), start)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), end)
        assertEquals(Duration.ofHours(23), Duration.between(start, end))
    }

    @Test
    fun `weekly range is ordered oldest to newest and inclusive`() {
        val days = StepSyncPlanner.lastDays(LocalDate.of(2026, 11, 24), 7)

        assertEquals(LocalDate.of(2026, 11, 18), days.first())
        assertEquals(LocalDate.of(2026, 11, 24), days.last())
        assertEquals(7, days.size)
    }

    @Test
    fun `today query ends at now instead of future midnight`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 24)
        val now = Instant.parse("2026-11-24T12:30:00Z")
        val bounds = StepSyncPlanner.dayBounds(today, zone)

        assertEquals(now, StepSyncPlanner.queryEnd(today, today, bounds.start, bounds.endExclusive, now))
    }

    @Test
    fun `today query is skipped when now has not passed day start`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 24)
        val now = Instant.parse("2026-11-23T23:59:00Z")
        val bounds = StepSyncPlanner.dayBounds(today, zone)

        assertNull(StepSyncPlanner.queryEnd(today, today, bounds.start, bounds.endExclusive, now))
    }
}
