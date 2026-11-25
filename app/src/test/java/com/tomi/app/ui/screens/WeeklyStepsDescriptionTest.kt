package com.tomi.app.ui.screens

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyStepsDescriptionTest {

    @Test
    fun `describes every day and the weekly total`() {
        val days = listOf(
            LocalDate.of(2026, 11, 23),
            LocalDate.of(2026, 11, 24),
            LocalDate.of(2026, 11, 25),
            LocalDate.of(2026, 11, 26),
            LocalDate.of(2026, 11, 27),
            LocalDate.of(2026, 11, 28),
            LocalDate.of(2026, 11, 29),
        )

        val description = weeklyStepsDescription(days, listOf(1_000L, 2_000L, 3_000L, 0L, 4_000L, 5_000L, 6_000L))

        assertEquals(
            "Weekly steps. Total 21,000. Monday, November 23: 1,000 steps. Tuesday, November 24: 2,000 steps. Wednesday, November 25: 3,000 steps. Thursday, November 26: 0 steps. Friday, November 27: 4,000 steps. Saturday, November 28: 5,000 steps. Sunday, November 29: 6,000 steps.",
            description,
        )
    }
}
