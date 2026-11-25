package com.tomi.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CurrentDayTest {

    @Test
    fun `refresh moves date scoped state across midnight`() {
        var now = Instant.parse("2026-11-24T23:59:00Z")
        val currentDay = CurrentDay(clock = { now })

        assertEquals(LocalDate.of(2026, 11, 24), currentDay.context.value.date)
        now = Instant.parse("2026-11-25T00:01:00Z")
        currentDay.refresh()

        assertEquals(LocalDate.of(2026, 11, 25), currentDay.context.value.date)
    }

    @Test
    fun `refresh tracks a timezone change`() {
        var zone = ZoneId.of("UTC")
        val currentDay = CurrentDay(clock = { Instant.parse("2026-11-24T23:30:00Z") }, zoneProvider = { zone })

        assertEquals(LocalDate.of(2026, 11, 24), currentDay.context.value.date)
        zone = ZoneId.of("America/Los_Angeles")
        currentDay.refresh()

        assertEquals(zone, currentDay.context.value.zone)
        assertEquals(LocalDate.of(2026, 11, 24), currentDay.context.value.date)
    }
}
