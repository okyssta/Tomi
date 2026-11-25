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
    fun `uses the configured start and end times for interval boundaries`() {
        val customWindow = settings.copy(activeStartMinutes = 10 * 60, activeEndMinutes = 16 * 60 + 15)
        val beforeStart = at("2026-11-23T09:00:00")
        val afterEnd = at("2026-11-23T16:10:00")
        val lastMovement = Instant.parse("2026-11-23T15:50:00Z")

        assertEquals(at("2026-11-23T10:00:00"), ReminderPolicy.nextReminderAt(beforeStart, null, customWindow))
        assertEquals(at("2026-11-24T10:00:00"), ReminderPolicy.nextReminderAt(afterEnd, lastMovement, customWindow))
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

    @Test
    fun `snooze cannot move a reminder outside active hours`() {
        val now = at("2026-11-23T17:55:00")
        val snoozeUntil = Instant.parse("2026-11-23T18:05:00Z")

        val next = ReminderPolicy.nextReminderAt(now, null, settings, snoozeUntil)

        assertEquals(at("2026-11-24T09:00:00"), next)
    }

    @Test
    fun `snooze cannot move a reminder onto an inactive day`() {
        val now = at("2026-11-22T23:55:00") // Sunday
        val snoozeUntil = Instant.parse("2026-11-23T00:05:00Z")

        val next = ReminderPolicy.nextReminderAt(now, null, settings, snoozeUntil)

        assertEquals(at("2026-11-23T09:00:00"), next)
    }

    @Test
    fun `future movement history is clamped after the clock moves backward`() {
        val now = at("2026-11-23T10:00:00")
        val futureMovement = Instant.parse("2026-11-23T12:00:00Z")

        val next = ReminderPolicy.nextReminderAt(now, futureMovement, settings)

        assertEquals(at("2026-11-23T10:40:00"), next)
    }

    @Test
    fun `gap shifted start cannot create an inverted active window`() {
        val zone = ZoneId.of("America/New_York")
        val settings = ReminderSettings(
            activeStartMinutes = 2 * 60 + 45,
            activeEndMinutes = 3 * 60 + 15,
            activeDays = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
        )
        val now = ZonedDateTime.of(2026, 3, 8, 1, 50, 0, 0, zone)

        val next = ReminderPolicy.nextReminderAt(now, null, settings)

        assertEquals(ZonedDateTime.of(2026, 3, 9, 2, 45, 0, 0, zone), next)
    }

    @Test
    fun `snooze uses the gap shifted end boundary`() {
        val zone = ZoneId.of("America/New_York")
        val settings = ReminderSettings(
            activeStartMinutes = 60,
            activeEndMinutes = 2 * 60 + 30,
            activeDays = setOf(DayOfWeek.SUNDAY),
        )
        val now = ZonedDateTime.of(2026, 3, 8, 1, 50, 0, 0, zone)
        val snoozeUntil = Instant.parse("2026-03-08T07:15:00Z") // 03:15 EDT

        val next = ReminderPolicy.nextReminderAt(now, null, settings, snoozeUntil)

        assertEquals(snoozeUntil.atZone(zone), next)
    }

    @Test
    fun `active check uses the same resolved fold boundary as scheduling`() {
        val zone = ZoneId.of("America/New_York")
        val settings = ReminderSettings(
            activeStartMinutes = 60,
            activeEndMinutes = 90,
            activeDays = setOf(DayOfWeek.SUNDAY),
        )
        val secondOneTwenty = ZonedDateTime.of(2026, 11, 1, 1, 20, 0, 0, zone)
            .withLaterOffsetAtOverlap()

        org.junit.Assert.assertFalse(ReminderPolicy.isActiveAt(secondOneTwenty, settings))
    }

    private fun at(value: String): ZonedDateTime = LocalDateTime.parse(value).atZone(zone)
}
