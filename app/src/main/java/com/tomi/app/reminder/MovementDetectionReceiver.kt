package com.tomi.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.health.HealthConnectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

class MovementDetectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settingsRepository = SettingsRepository.from(context)
                if (!settingsRepository.settings.first().smartPause) return@launch
                val runtime = settingsRepository.runtime.first()
                val lastCheck = runtime.lastStepCheckAt?.let(Instant::ofEpochMilli)
                val manager = HealthConnectManager(
                    context,
                    settingsRepository,
                    TomiDatabase.get(context).stepCacheDao(),
                )
                if (!manager.hasReadPermission()) return@launch
                val steps = manager.stepsSince(lastCheck)
                if (steps >= SMART_PAUSE_STEPS) {
                    val now = System.currentTimeMillis()
                    settingsRepository.updateRuntime {
                        it.copy(lastMovementAt = now, lastStepCheckAt = now, snoozeUntil = null)
                    }
                    ReminderScheduler(context, settingsRepository, ReminderNotifier(context)).reschedule()
                } else {
                    settingsRepository.updateRuntime { it.copy(lastStepCheckAt = System.currentTimeMillis()) }
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    companion object {
        private const val SMART_PAUSE_STEPS = 50L
    }
}
