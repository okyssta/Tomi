package com.tomi.app.reminder

import android.content.Context
import com.tomi.app.data.local.MovementEntity
import com.tomi.app.data.local.MovementType
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.health.HealthConnectManager
import java.time.Instant

class MovementSessionRecorder(
    private val context: Context,
    private val settingsRepository: SettingsRepository = SettingsRepository.from(context),
    private val database: TomiDatabase = TomiDatabase.get(context),
) {
    suspend fun log(
        type: MovementType,
        startedAt: Long,
        durationSeconds: Int,
        reps: Int? = null,
        note: String = "",
    ): Long {
        require(durationSeconds >= 0)
        require(reps == null || reps >= 0)
        val endedAt = startedAt + durationSeconds * 1_000L
        val id = database.movementDao().insert(
            MovementEntity(
                type = type,
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = durationSeconds,
                reps = reps,
                note = note,
            ),
        )
        val now = endedAt.coerceAtMost(System.currentTimeMillis())
        settingsRepository.updateRuntime {
            it.copy(
                lastMovementAt = now,
                lastStepCheckAt = now,
                snoozeUntil = null,
            )
        }
        ReminderScheduler(context, settingsRepository, ReminderNotifier(context)).reschedule(Instant.ofEpochMilli(now))
        HealthConnectManager(context, settingsRepository, database.stepCacheDao()).syncSteps(days = 1, now = Instant.ofEpochMilli(now))
        return id
    }
}
