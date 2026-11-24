package com.tomi.app.domain.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {

    private val zone = ZoneId.of("UTC")
    private val weekdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )
    private val settings = ReminderSettings(activeDays = weekdays)

    @Test
    fun `reminds one configured interval after the last movement`() {
        val now = at("2026-11-23T10:10:00")
        val lastMovement = Instant.parse("2026-11-23T10:00:00Z")

        val next = ReminderPolicy.nextReminderAt(now, lastMovement, settings)

        assertEquals(at("2026-11-23T10:40:00"), next)
    }

    @Test
    fun `reminds immediately when the configured interval has already elapsed`() {
        val now = at("2026-11-23T11:00:00")
        val lastMovement = Instant.parse("2026-11-23T09:30:00Z")

        val next = ReminderPolicy.nextReminderAt(now, lastMovement, settings)

        assertEquals(now, next)
    }

    @Test
    fun `starts the first interval when there is no movement history`() {
        val now = at("2026-11-23T10:10:00")

        val next = ReminderPolicy.nextReminderAt(now, null, settings)

        assertEquals(at("2026-11-23T10:50:00"), next)
    }

    @Test
    fun `does not schedule a reminder after the workday ends`() {
        val now = at("2026-11-23T17:30:00")
        val lastMovement = Instant.parse("2026-11-23T17:20:00Z")

        val next = ReminderPolicy.nextReminderAt(now, lastMovement, settings)

        assertEquals(at("2026-11-24T09:00:00"), next)
    }

    @Test
    fun `skips inactive days and starts at the next active day`() {
        val now = at("2026-11-21T12:00:00") // Saturday

        val next = ReminderPolicy.nextReminderAt(now, null, settings)

        assertEquals(at("2026-11-23T09:00:00"), next)
    }

    @Test
    fun `before work starts schedules the beginning of the active window`() {
        val now = at("2026-11-23T07:15:00")

        val next = ReminderPolicy.nextReminderAt(now, null, settings)

        assertEquals(at("2026-11-23T09:00:00"), next)
    }

    @Test
    fun `disabled reminders have no next occurrence`() {
        val now = at("2026-11-23T10:10:00")

        val next = ReminderPolicy.nextReminderAt(now, null, settings.copy(enabled = false))

        assertNull(next)
    }

    @Test
    fun `interval outside the supported sedentary range is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            settings.copy(intervalMinutes = 29)
        }
        assertThrows(IllegalArgumentException::class.java) {
            settings.copy(intervalMinutes = 46)
        }
    }

    @Test
    fun `active window must start before it ends`() {
        assertThrows(IllegalArgumentException::class.java) {
            settings.copy(activeStartMinutes = 18 * 60, activeEndMinutes = 9 * 60)
        }
    }

    @Test
    fun `active snooze overrides the normal schedule`() {
        val now = at("2026-11-23T10:10:00")
        val snoozeUntil = Instant.parse("2026-11-23T10:20:00Z")

        val next = ReminderPolicy.nextReminderAt(now, null, settings, snoozeUntil)

        assertEquals(at("2026-11-23T10:20:00"), next)
    }

    @Test
    fun `expired snooze falls back to the normal schedule`() {
        val now = at("2026-11-23T10:10:00")
        val snoozeUntil = Instant.parse("2026-11-23T10:05:00Z")

        val next = ReminderPolicy.nextReminderAt(now, null, settings, snoozeUntil)

        assertEquals(at("2026-11-23T10:50:00"), next)
    }

    private fun at(value: String): ZonedDateTime = LocalDateTime.parse(value).atZone(zone)
}
