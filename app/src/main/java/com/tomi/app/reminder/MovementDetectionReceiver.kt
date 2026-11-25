package com.tomi.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.health.HealthConnectManager
import com.tomi.app.health.StepDeltaResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.time.Instant

internal interface ForegroundMovementHealth {
    suspend fun hasReadPermission(): Boolean
    suspend fun hasBackgroundReadPermission(): Boolean
    suspend fun stepsSince(lastCheck: Instant?): StepDeltaResult
}

internal suspend fun detectForegroundMovement(
    smartPause: Boolean,
    runtime: ReminderRuntimeState,
    health: ForegroundMovementHealth,
    updateRuntime: suspend ((ReminderRuntimeState) -> ReminderRuntimeState) -> Unit,
    reschedule: suspend () -> Unit,
    now: Long = System.currentTimeMillis(),
): Boolean = MovementCheckpointProcessor.process(
    enabled = smartPause,
    queriedCheckpoint = runtime.lastStepCheckAt,
    hasPermission = health::hasReadPermission,
    readSteps = health::stepsSince,
    updateRuntime = updateRuntime,
    onMovement = reschedule,
    now = now,
)

private class HealthConnectMovementHealth(
    private val manager: HealthConnectManager,
) : ForegroundMovementHealth {
    override suspend fun hasReadPermission(): Boolean = manager.hasReadPermission()
    override suspend fun hasBackgroundReadPermission(): Boolean = manager.hasBackgroundReadPermission()
    override suspend fun stepsSince(lastCheck: Instant?): StepDeltaResult = manager.stepsSince(lastCheck)
}

class MovementDetectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> },
        )
        scope.launch {
            try {
                val settingsRepository = SettingsRepository.from(context)
                MovementCheckpointLock.mutex.withLock {
                    val settings = settingsRepository.settings.first()
                    val runtime = settingsRepository.runtime.first()
                    val manager = HealthConnectManager(
                        context,
                        settingsRepository,
                        TomiDatabase.get(context).stepCacheDao(),
                    )
                    detectForegroundMovement(
                        smartPause = settings.smartPause,
                        runtime = runtime,
                        health = HealthConnectMovementHealth(manager),
                        updateRuntime = settingsRepository::updateRuntime,
                        reschedule = {
                            ReminderScheduler(context, settingsRepository, ReminderNotifier(context)).reschedule()
                        },
                    )
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
