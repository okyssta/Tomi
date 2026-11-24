package com.tomi.app.domain.movement

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MovementStreakTest {

    @Test
    fun `counts consecutive movement days ending today`() {
        val today = LocalDate.of(2026, 11, 24)
        val days = setOf(today, today.minusDays(1), today.minusDays(2))

        assertEquals(3, MovementStreak.calculate(days, today))
    }

    @Test
    fun `keeps a streak alive when today has not been logged yet`() {
        val today = LocalDate.of(2026, 11, 24)
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))

        assertEquals(3, MovementStreak.calculate(days, today))
    }

    @Test
    fun `stops at the first missing day`() {
        val today = LocalDate.of(2026, 11, 24)
        val days = setOf(today, today.minusDays(1), today.minusDays(3))

        assertEquals(2, MovementStreak.calculate(days, today))
    }

    @Test
    fun `empty history has no streak`() {
        assertEquals(0, MovementStreak.calculate(emptySet(), LocalDate.of(2026, 11, 24)))
    }
}
