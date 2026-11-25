package com.tomi.app.reminder

import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.health.StepDeltaResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MovementDetectionReceiverTest {

    @Test
    fun `foreground smart pause needs read permission but not background permission`() = runBlocking {
        val health = FakeHealth()
        var runtime = ReminderRuntimeState(lastStepCheckAt = 1_000L)
        var reschedules = 0

        detectForegroundMovement(
            smartPause = true,
            runtime = runtime,
            health = health,
            updateRuntime = { transform -> runtime = transform(runtime) },
            reschedule = { reschedules++ },
            now = 2_000L,
        )

        assertEquals(1, health.readPermissionChecks)
        assertEquals(0, health.backgroundPermissionChecks)
        assertEquals(2_000L, runtime.lastMovementAt)
        assertEquals(1, reschedules)
        Unit
    }

    private class FakeHealth : ForegroundMovementHealth {
        var readPermissionChecks = 0
        var backgroundPermissionChecks = 0

        override suspend fun hasReadPermission(): Boolean {
            readPermissionChecks++
            return true
        }

        override suspend fun hasBackgroundReadPermission(): Boolean {
            backgroundPermissionChecks++
            return false
        }

        override suspend fun stepsSince(lastCheck: Instant?): StepDeltaResult = StepDeltaResult.Success(50L)
    }
}
