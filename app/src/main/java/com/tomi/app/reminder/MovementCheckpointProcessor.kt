package com.tomi.app.reminder

import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.health.StepDeltaResult
import java.time.Instant

internal object MovementCheckpointProcessor {
    suspend fun process(
        enabled: Boolean,
        queriedCheckpoint: Long?,
        hasPermission: suspend () -> Boolean,
        readSteps: suspend (Instant?) -> StepDeltaResult,
        updateRuntime: suspend ((ReminderRuntimeState) -> ReminderRuntimeState) -> Unit,
        onMovement: suspend () -> Unit,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!enabled || !hasPermission()) return false
        val checkpoint = queriedCheckpoint?.let(Instant::ofEpochMilli)
        val result = readSteps(checkpoint)
        if (result !is StepDeltaResult.Success) return false

        var movementDetected = false
        val checkpointAt = maxOf(now, result.queryEnd?.toEpochMilli() ?: now)
        updateRuntime { current ->
            MovementCheckpoint.advance(current, queriedCheckpoint, result.steps, checkpointAt).also { updated ->
                movementDetected = updated.lastMovementAt == checkpointAt && current.lastMovementAt != checkpointAt
            }
        }
        if (movementDetected) onMovement()
        return true
    }
}
