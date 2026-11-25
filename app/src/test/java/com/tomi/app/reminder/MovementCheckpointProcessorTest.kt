package com.tomi.app.reminder

import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.health.StepDeltaResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MovementCheckpointProcessorTest {

    @Test
    fun `disabled smart pause does not check permissions or read steps`() = runBlocking {
        var permissionChecks = 0
        var reads = 0
        var updates = 0

        val processed = MovementCheckpointProcessor.process(
            enabled = false,
            queriedCheckpoint = 1_000L,
            hasPermission = { permissionChecks++; true },
            readSteps = { reads++; StepDeltaResult.Success(50L) },
            updateRuntime = { updates++; it },
            onMovement = { error("Movement must not be reported") },
            now = 2_000L,
        )

        assertFalse(processed)
        assertEquals(0, permissionChecks)
        assertEquals(0, reads)
        assertEquals(0, updates)
        Unit
    }

    @Test
    fun `foreground read permission is enough to detect movement`() = runBlocking {
        var queried: Instant? = null
        var runtime = ReminderRuntimeState(lastStepCheckAt = 1_000L, snoozeUntil = 3_000L)
        var movements = 0

        val processed = MovementCheckpointProcessor.process(
            enabled = true,
            queriedCheckpoint = 1_000L,
            hasPermission = { true },
            readSteps = { checkpoint -> queried = checkpoint; StepDeltaResult.Success(50L) },
            updateRuntime = { transform -> runtime = transform(runtime) },
            onMovement = { movements++ },
            now = 2_000L,
        )

        assertTrue(processed)
        assertEquals(Instant.ofEpochMilli(1_000L), queried)
        assertEquals(2_000L, runtime.lastMovementAt)
        assertEquals(null, runtime.snoozeUntil)
        assertEquals(1, movements)
        Unit
    }

    @Test
    fun `checkpoint advances to the health query end rather than the request start`() = runBlocking {
        var runtime = ReminderRuntimeState(lastStepCheckAt = 1_000L)
        val queryEnd = Instant.ofEpochMilli(3_500L)

        val processed = MovementCheckpointProcessor.process(
            enabled = true,
            queriedCheckpoint = 1_000L,
            hasPermission = { true },
            readSteps = { StepDeltaResult.Success(50L, queryEnd) },
            updateRuntime = { transform -> runtime = transform(runtime) },
            onMovement = { },
            now = 2_000L,
        )

        assertTrue(processed)
        assertEquals(queryEnd.toEpochMilli(), runtime.lastStepCheckAt)
        assertEquals(queryEnd.toEpochMilli(), runtime.lastMovementAt)
        Unit
    }

    @Test
    fun `missing permission skips the health read`() = runBlocking {
        var reads = 0

        val processed = MovementCheckpointProcessor.process(
            enabled = true,
            queriedCheckpoint = 1_000L,
            hasPermission = { false },
            readSteps = { reads++; StepDeltaResult.Success(50L) },
            updateRuntime = { it },
            onMovement = { error("Movement must not be reported") },
            now = 2_000L,
        )

        assertFalse(processed)
        assertEquals(0, reads)
        Unit
    }
}
