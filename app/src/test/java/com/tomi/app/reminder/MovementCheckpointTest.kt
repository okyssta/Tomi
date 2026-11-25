package com.tomi.app.reminder

import com.tomi.app.data.settings.ReminderRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovementCheckpointTest {

    @Test
    fun `successful no-movement read advances the checkpoint`() {
        val current = ReminderRuntimeState(lastStepCheckAt = 1_000L, lastMovementAt = 500L)

        val updated = MovementCheckpoint.advance(current, queriedCheckpoint = 1_000L, steps = 12L, now = 2_000L)

        assertEquals(2_000L, updated.lastStepCheckAt)
        assertEquals(500L, updated.lastMovementAt)
    }

    @Test
    fun `detected movement resets inactivity and clears snooze`() {
        val current = ReminderRuntimeState(lastStepCheckAt = 1_000L, snoozeUntil = 5_000L)

        val updated = MovementCheckpoint.advance(current, queriedCheckpoint = 1_000L, steps = 50L, now = 2_000L)

        assertEquals(2_000L, updated.lastMovementAt)
        assertEquals(2_000L, updated.lastStepCheckAt)
        assertNull(updated.snoozeUntil)
    }

    @Test
    fun `stale concurrent read cannot overwrite a newer checkpoint`() {
        val current = ReminderRuntimeState(lastStepCheckAt = 3_000L, lastMovementAt = 3_000L)

        val updated = MovementCheckpoint.advance(current, queriedCheckpoint = 1_000L, steps = 0L, now = 2_000L)

        assertEquals(current, updated)
    }

    @Test
    fun `backward clock sync cannot regress the checkpoint`() {
        val current = ReminderRuntimeState(lastStepCheckAt = 3_000L, lastMovementAt = 2_500L)

        val updated = MovementCheckpoint.advance(current, queriedCheckpoint = 3_000L, steps = 0L, now = 2_000L)

        assertEquals(current, updated)
    }
}
