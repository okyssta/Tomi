package com.tomi.app.reminder

import com.tomi.app.data.settings.ReminderRuntimeState

object MovementCheckpoint {
    const val SMART_PAUSE_STEPS = 50L

    fun advance(
        current: ReminderRuntimeState,
        queriedCheckpoint: Long?,
        steps: Long,
        now: Long,
    ): ReminderRuntimeState {
        if (current.lastStepCheckAt != queriedCheckpoint) return current
        val checkpoint = maxOf(now, current.lastStepCheckAt ?: now)
        return if (steps >= SMART_PAUSE_STEPS) {
            current.copy(
                lastMovementAt = maxOf(now, current.lastMovementAt ?: now),
                lastStepCheckAt = checkpoint,
                snoozeUntil = null,
            )
        } else {
            current.copy(lastStepCheckAt = checkpoint)
        }
    }

    fun recordMovement(current: ReminderRuntimeState, now: Long): ReminderRuntimeState = current.copy(
        lastMovementAt = maxOf(now, current.lastMovementAt ?: now),
        lastStepCheckAt = maxOf(now, current.lastStepCheckAt ?: now),
        snoozeUntil = null,
    )
}
