package com.tomi.app.data.settings

import com.tomi.app.domain.reminder.ReminderSettings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class TomiSettings(
    val reminder: ReminderSettings = ReminderSettings(),
    val stepGoal: Int = 6_000,
    val waterGoalMl: Int = 2_000,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val smartPause: Boolean = true,
    val autoDownloadUpdates: Boolean = true,
    val onboardingComplete: Boolean = false,
    val googleDriveTreeUri: String? = null,
    val googleDriveFolderName: String? = null,
    val googleDriveLastBackupAt: Long? = null,
    val googleDriveAutoSync: Boolean = true,
)

data class ReminderRuntimeState(
    val lastMovementAt: Long? = null,
    val lastStepCheckAt: Long? = null,
    val snoozeUntil: Long? = null,
    val lastReminderAt: Long? = null,
    val nextReminderAt: Long? = null,
)
