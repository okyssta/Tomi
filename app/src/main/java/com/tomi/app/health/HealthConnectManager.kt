package com.tomi.app.health

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tomi.app.backup.DataMaintenanceCoordinator
import com.tomi.app.data.local.StepCacheDao
import com.tomi.app.data.local.StepCacheEntity
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.reminder.MovementCheckpoint
import com.tomi.app.reminder.MovementCheckpointLock
import com.tomi.app.widget.TomiWidget
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

enum class HealthStatus {
    UNKNOWN,
    AVAILABLE_GRANTED,
    AVAILABLE_NEEDS_PERMISSION,
    UPDATE_REQUIRED,
    UNAVAILABLE,
}

sealed interface StepSyncResult {
    data class Success(val daysSynced: Int, val totalSteps: Long) : StepSyncResult
    data object PermissionRequired : StepSyncResult
    data class Error(val cause: Throwable) : StepSyncResult
}

sealed interface StepDeltaResult {
    data class Success(val steps: Long, val queryEnd: Instant? = null) : StepDeltaResult
    data class Error(val cause: Throwable) : StepDeltaResult
}

class HealthConnectManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val stepCacheDao: StepCacheDao,
    private val permissionChecker: (suspend () -> Boolean)? = null,
    private val stepsReader: (suspend (Instant, Instant) -> Long)? = null,
    private val widgetUpdater: (() -> Unit)? = null,
) {
    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    val optionalPermissions: Set<String> = setOf(
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
    )

    suspend fun status(): HealthStatus = try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthStatus.UNAVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.UPDATE_REQUIRED
            else -> if (hasReadPermission()) {
                HealthStatus.AVAILABLE_GRANTED
            } else {
                HealthStatus.AVAILABLE_NEEDS_PERMISSION
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        HealthStatus.UNAVAILABLE
    }

    suspend fun hasReadPermission(): Boolean = try {
        permissionChecker?.invoke() ?: client.permissionController.getGrantedPermissions().containsAll(requiredPermissions)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }

    suspend fun hasBackgroundReadPermission(): Boolean = try {
        client.permissionController.getGrantedPermissions().containsAll(optionalPermissions)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }

    suspend fun readSteps(start: Instant, end: Instant): Long {
        require(end.isAfter(start))
        stepsReader?.let { return it(start, end) }
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun syncSteps(days: Int = 7, now: Instant = Instant.now()): StepSyncResult =
        DataMaintenanceCoordinator.exclusive { syncStepsExclusive(days, now) }

    internal suspend fun syncStepsExclusive(days: Int = 7, now: Instant = Instant.now()): StepSyncResult {
        if (!hasReadPermission()) return StepSyncResult.PermissionRequired
        val queriedCheckpoint = settingsRepository.runtime.first().lastStepCheckAt
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        var total = 0L
        var syncedDays = 0
        return try {
            StepSyncPlanner.lastDays(today, days).forEach { date ->
                val bounds = StepSyncPlanner.dayBounds(date, zone)
                val queryEnd = StepSyncPlanner.queryEnd(date, today, bounds.start, bounds.endExclusive, now)
                    ?: return@forEach
                val steps = readSteps(bounds.start, queryEnd)
                StepCacheWriter.write(
                    stepCacheDao,
                    StepCacheEntity(
                        date = date.toString(),
                        steps = steps,
                        updatedAt = queryEnd.toEpochMilli(),
                    ),
                )
                total += steps
                syncedDays++
            }
            MovementCheckpointLock.mutex.withLock {
                settingsRepository.updateRuntime {
                    MovementCheckpoint.advance(it, queriedCheckpoint, steps = 0L, now = now.toEpochMilli())
                }
            }
            if (widgetUpdater != null) widgetUpdater.invoke() else runCatching { TomiWidget().updateAll(context) }
            StepSyncResult.Success(syncedDays, total)
        } catch (_: SecurityException) {
            StepSyncResult.PermissionRequired
        } catch (error: IOException) {
            StepSyncResult.Error(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            StepSyncResult.Error(error)
        }
    }

    suspend fun stepsSince(lastCheck: Instant?): StepDeltaResult {
        if (lastCheck == null) return StepDeltaResult.Success(0L)
        val now = Instant.now()
        if (!now.isAfter(lastCheck)) return StepDeltaResult.Success(0L)
        return try {
            StepDeltaResult.Success(readSteps(lastCheck, now), queryEnd = now)
        } catch (_: SecurityException) {
            StepDeltaResult.Error(SecurityException("Health Connect step permission denied"))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            StepDeltaResult.Error(IllegalStateException("Could not read Health Connect steps"))
        }
    }

    fun openSettingsIntent(): Intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun openProviderStoreIntent(): Intent = Intent(
        Intent.ACTION_VIEW,
        "market://details?id=com.google.android.apps.healthdata".toUri(),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    companion object {
        fun dateKey(date: LocalDate): String = date.toString()
    }
}
