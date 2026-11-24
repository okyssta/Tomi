package com.tomi.app.health

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tomi.app.data.local.StepCacheDao
import com.tomi.app.data.local.StepCacheEntity
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.widget.TomiWidget
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

class HealthConnectManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val stepCacheDao: StepCacheDao,
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
    } catch (_: Exception) {
        HealthStatus.UNAVAILABLE
    }

    suspend fun hasReadPermission(): Boolean = try {
        client.permissionController.getGrantedPermissions().containsAll(requiredPermissions)
    } catch (_: Exception) {
        false
    }

    suspend fun hasBackgroundReadPermission(): Boolean = try {
        client.permissionController.getGrantedPermissions().containsAll(optionalPermissions)
    } catch (_: Exception) {
        false
    }

    suspend fun readSteps(start: Instant, end: Instant): Long {
        require(end.isAfter(start))
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun syncSteps(days: Int = 7, now: Instant = Instant.now()): StepSyncResult {
        if (!hasReadPermission()) return StepSyncResult.PermissionRequired
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        var total = 0L
        return try {
            StepSyncPlanner.lastDays(today, days).forEach { date ->
                val bounds = StepSyncPlanner.dayBounds(date, zone)
                val steps = readSteps(bounds.start, bounds.endExclusive)
                stepCacheDao.upsert(
                    StepCacheEntity(
                        date = date.toString(),
                        steps = steps,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                total += steps
            }
            settingsRepository.updateRuntime { it.copy(lastStepCheckAt = now.toEpochMilli()) }
            runCatching { TomiWidget().updateAll(context) }
            StepSyncResult.Success(days, total)
        } catch (_: SecurityException) {
            StepSyncResult.PermissionRequired
        } catch (error: IOException) {
            StepSyncResult.Error(error)
        } catch (error: Exception) {
            StepSyncResult.Error(error)
        }
    }

    suspend fun stepsSince(lastCheck: Instant?): Long {
        if (lastCheck == null) return 0L
        val now = Instant.now()
        if (!now.isAfter(lastCheck)) return 0L
        return try {
            readSteps(lastCheck, now)
        } catch (_: SecurityException) {
            0L
        } catch (_: Exception) {
            0L
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
