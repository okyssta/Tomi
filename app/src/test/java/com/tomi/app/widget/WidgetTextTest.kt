package com.tomi.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class WidgetTextTest {
    @Test
    fun `formats the next reminder countdown`() {
        val now = Instant.parse("2026-11-24T10:00:00Z")
        val next = ZonedDateTime.of(2026, 11, 24, 10, 25, 0, 0, ZoneId.of("UTC"))

        assertEquals("Next reminder in 25 min", WidgetText.nextReminder(true, next, now))
    }

    @Test
    fun `reports disabled and unscheduled states`() {
        val now = Instant.parse("2026-11-24T10:00:00Z")

        assertEquals("Reminders off", WidgetText.nextReminder(false, null, now))
        assertEquals("No reminder scheduled", WidgetText.nextReminder(true, null, now))
    }
}
