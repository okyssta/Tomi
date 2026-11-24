package com.tomi.app.domain.movement

import java.time.LocalDate

object MovementStreak {
    fun calculate(movementDays: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in movementDays) today else today.minusDays(1)
        var streak = 0
        while (cursor in movementDays) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
