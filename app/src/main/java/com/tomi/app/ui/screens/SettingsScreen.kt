package com.tomi.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tomi.app.BuildConfig
import com.tomi.app.data.settings.ThemeMode
import com.tomi.app.health.HealthStatus
import com.tomi.app.ui.AboutActivity
import com.tomi.app.ui.TomiViewModel
import com.tomi.app.ui.components.SettingsToggleRow
import com.tomi.app.ui.UpdateUiState
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: TomiViewModel,
    onConnectHealth: () -> Unit,
    onRequestBackgroundHealthPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val healthStatus by viewModel.healthStatus.collectAsStateWithLifecycle()
    val backgroundHealthGranted by viewModel.backgroundHealthGranted.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    fun hasNotificationPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED

    fun hasExactAlarmPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()

    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission()) }
    var exactAlarmsGranted by remember { mutableStateOf(hasExactAlarmPermission()) }
    var installPermissionGranted by remember { mutableStateOf(context.packageManager.canRequestPackageInstalls()) }

    LifecycleResumeEffect(Unit) {
        notificationsGranted = hasNotificationPermission()
        exactAlarmsGranted = hasExactAlarmPermission()
        installPermissionGranted = context.packageManager.canRequestPackageInstalls()
        viewModel.refreshHealth()
        viewModel.resumeUpdateDownloadState()
        onPauseOrDispose { }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationsGranted = hasNotificationPermission() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportTo) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            SettingsSection("Movement reminders")
            SettingsToggleRow(
                title = "Remind me to move",
                supportingText = "Every ${settings.reminder.intervalMinutes} minutes during your active hours",
                checked = settings.reminder.enabled,
                onCheckedChange = { enabled ->
                    viewModel.updateSettings { it.copy(reminder = it.reminder.copy(enabled = enabled)) }
                },
                leadingIcon = Icons.Outlined.Alarm,
            )
            ListItem(
                headlineContent = { Text("Reminder interval") },
                supportingContent = { Text("${settings.reminder.intervalMinutes} minutes") },
            )
            Slider(
                value = settings.reminder.intervalMinutes.toFloat(),
                onValueChange = { value ->
                    viewModel.updateSettings { it.copy(reminder = it.reminder.copy(intervalMinutes = value.toInt())) }
                },
                valueRange = 30f..45f,
                steps = 14,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics {
                        contentDescription = "Reminder interval"
                        stateDescription = "${settings.reminder.intervalMinutes} minutes"
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            settings.reminder.intervalMinutes.toFloat(),
                            30f..45f,
                        )
                    },
            )
            ReminderTimePickerRow(
                title = "Start time",
                showLeadingIcon = true,
                minuteOfDay = settings.reminder.activeStartMinutes,
                onTimeSelected = { minute ->
                    viewModel.updateSettings { it.copy(reminder = it.reminder.copy(activeStartMinutes = minute)) }
                },
            )
            ReminderTimePickerRow(
                title = "End time",
                minuteOfDay = settings.reminder.activeEndMinutes,
                onTimeSelected = { minute ->
                    viewModel.updateSettings { it.copy(reminder = it.reminder.copy(activeEndMinutes = minute)) }
                },
            )
            WeekdayChips(
                activeDays = settings.reminder.activeDays,
                onDayToggle = { day ->
                    val days = settings.reminder.activeDays.toMutableSet()
                    if (day in days) days.remove(day) else days.add(day)
                    viewModel.updateSettings { it.copy(reminder = it.reminder.copy(activeDays = days)) }
                },
            )
            SettingsToggleRow(
                title = "Smart pause",
                supportingText = "Reset the timer when Health Connect detects about 50 steps",
                checked = settings.smartPause,
                onCheckedChange = { value -> viewModel.updateSettings { it.copy(smartPause = value) } },
            )
            SettingsToggleRow(
                title = "Alarm sound",
                supportingText = "Play your device alarm sound when it is time to move",
                checked = settings.reminder.sound,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(reminder = it.reminder.copy(sound = value)) }
                },
            )
            SettingsToggleRow(
                title = "Vibration",
                supportingText = null,
                checked = settings.reminder.vibrate,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(reminder = it.reminder.copy(vibrate = value)) }
                },
            )
            ListItem(
                headlineContent = { Text("Test alarm") },
                supportingContent = { Text("Play a sample movement alarm") },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                trailingContent = { TextButton(onClick = viewModel::sendTestReminder) { Text("Send") } },
            )

            HorizontalDivider()
            SettingsSection("Permissions & steps")
            ListItem(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text(if (notificationsGranted) "Allowed" else "Required for movement reminders") },
                trailingContent = {
                    if (!notificationsGranted) {
                        TextButton(onClick = { notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS") }) {
                            Text("Allow")
                        }
                    }
                },
            )
            ListItem(
                headlineContent = { Text("Exact alarms") },
                supportingContent = {
                    Text(if (exactAlarmsGranted) "Allowed for on-time reminders" else "Optional; reminders may be delayed by battery optimization")
                },
                trailingContent = {
                    if (!exactAlarmsGranted) {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        "android.settings.REQUEST_SCHEDULE_EXACT_ALARM",
                                        "package:${context.packageName}".toUri(),
                                    ),
                                )
                            },
                        ) { Text("Allow") }
                    }
                },
            )
            ListItem(
                headlineContent = { Text("Health Connect") },
                supportingContent = {
                    Text(
                        when (healthStatus) {
                            HealthStatus.AVAILABLE_GRANTED -> "Connected · reads phone and Google Fit step totals"
                            HealthStatus.AVAILABLE_NEEDS_PERMISSION -> "Step permission needed"
                            HealthStatus.UPDATE_REQUIRED -> "Health Connect needs an update"
                            HealthStatus.UNAVAILABLE -> "Install or enable Health Connect"
                            HealthStatus.UNKNOWN -> "Checking availability"
                        },
                    )
                },
                leadingContent = { Icon(Icons.Outlined.HealthAndSafety, contentDescription = null) },
                trailingContent = {
                    TextButton(onClick = onConnectHealth) { Text(if (healthStatus == HealthStatus.AVAILABLE_GRANTED) "Manage" else "Connect") }
                },
            )
            if (healthStatus == HealthStatus.AVAILABLE_GRANTED) {
                BackgroundHealthRow(
                    granted = backgroundHealthGranted,
                    onRequestBackgroundPermissions = onRequestBackgroundHealthPermissions,
                )
            }

            HorizontalDivider()
            SettingsSection("Daily goals")
            ListItem(
                headlineContent = { Text("Step goal") },
                supportingContent = { Text("%,d steps".format(settings.stepGoal)) },
            )
            Slider(
                value = settings.stepGoal.toFloat(),
                onValueChange = { value ->
                    val goal = ((value / 500f).toInt() * 500).coerceIn(1_000, 30_000)
                    viewModel.updateSettings { it.copy(stepGoal = goal) }
                },
                valueRange = 1_000f..30_000f,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics {
                        contentDescription = "Daily step goal"
                        stateDescription = "%,d steps".format(settings.stepGoal)
                        progressBarRangeInfo = ProgressBarRangeInfo(settings.stepGoal.toFloat(), 1_000f..30_000f)
                    },
            )
            ListItem(
                headlineContent = { Text("Water goal") },
                supportingContent = { Text("${settings.waterGoalMl / 250} glasses · ${settings.waterGoalMl} ml") },
            )
            Slider(
                value = settings.waterGoalMl.toFloat(),
                onValueChange = { value ->
                    val goal = ((value / 500f).toInt() * 500).coerceIn(500, 6_000)
                    viewModel.updateSettings { it.copy(waterGoalMl = goal) }
                },
                valueRange = 500f..6_000f,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics {
                        contentDescription = "Daily water goal"
                        stateDescription = "${settings.waterGoalMl} milliliters"
                        progressBarRangeInfo = ProgressBarRangeInfo(settings.waterGoalMl.toFloat(), 500f..6_000f)
                    },
            )

            HorizontalDivider()
            SettingsSection("Appearance")
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.updateSettings { it.copy(themeMode = mode) } },
                        label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
            SettingsToggleRow(
                title = "Material You colors",
                supportingText = "Use colors from your wallpaper on Android 12+",
                checked = settings.dynamicColor,
                onCheckedChange = { value -> viewModel.updateSettings { it.copy(dynamicColor = value) } },
                leadingIcon = Icons.Outlined.Palette,
            )

            if (BuildConfig.UPDATES_ENABLED) {
                HorizontalDivider()
                SettingsSection("Updates")
            ListItem(
                headlineContent = { Text("Tomi ${BuildConfig.VERSION_NAME}") },
                supportingContent = {
                    Text(
                        when (val state = updateState) {
                            UpdateUiState.Idle -> "Check for a new signed release"
                            UpdateUiState.Checking -> "Checking GitHub releases…"
                            UpdateUiState.UpToDate -> "You are up to date"
                            is UpdateUiState.Available -> "Version ${state.info.versionName} is available"
                            is UpdateUiState.DownloadQueued -> "Downloading ${state.info.versionName}; installation will ask for confirmation"
                            is UpdateUiState.DownloadReady -> "Download verified. Tap Install when ready."
                            is UpdateUiState.Error -> state.message
                        },
                    )
                },
                leadingContent = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                trailingContent = {
                    TextButton(
                        onClick = viewModel::checkForUpdates,
                        enabled = updateState != UpdateUiState.Checking,
                    ) { Text("Check") }
                },
            )
            (updateState as? UpdateUiState.Available)?.let { state ->
                ListItem(
                    headlineContent = { Text("Download ${state.info.versionName}") },
                    supportingContent = { Text(state.info.notes.lineSequence().firstOrNull() ?: "Verified GitHub release") },
                    leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    trailingContent = {
                        Button(onClick = { viewModel.downloadUpdate(state.info) }) { Text("Download") }
                    },
                )
            }
            (updateState as? UpdateUiState.DownloadReady)?.let { state ->
                ListItem(
                    headlineContent = { Text("Install ${state.info.versionName}") },
                    supportingContent = { Text("The APK passed checksum, package, version, and signing checks") },
                    leadingContent = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                    trailingContent = {
                        Button(onClick = { viewModel.installUpdate(state.info) }) { Text("Install") }
                    },
                )
            }
            (updateState as? UpdateUiState.Error)?.info?.let { info ->
                ListItem(
                    headlineContent = { Text("Retry ${info.versionName} download") },
                    supportingContent = { Text("Start a fresh verified download") },
                    leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    trailingContent = {
                        Button(onClick = { viewModel.downloadUpdate(info) }) { Text("Retry") }
                    },
                )
            }
            SettingsToggleRow(
                title = "Download updates automatically",
                supportingText = "Wi-Fi only; SHA-256 and signing identity verified; installation always asks first",
                checked = settings.autoDownloadUpdates,
                onCheckedChange = { value -> viewModel.updateSettings { it.copy(autoDownloadUpdates = value) } },
            )
            if (!installPermissionGranted) {
                ListItem(
                    headlineContent = { Text("Allow app installs") },
                    supportingContent = { Text("Required only when installing a Tomi update") },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        "package:${context.packageName}".toUri(),
                                    ),
                                )
                            },
                        ) { Text("Allow") }
                    },
                )
            }

            }

            HorizontalDivider()
            SettingsSection("Data & privacy")
            ListItem(
                headlineContent = { Text("Export backup") },
                supportingContent = { Text("Tasks, journal, movement, check-ins, water, step cache, and settings") },
                leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
                trailingContent = {
                    TextButton(onClick = { exportLauncher.launch("tomi-backup-${System.currentTimeMillis()}.json") }) {
                        Text("Export")
                    }
                },
            )
            BackupImportRow(onImport = viewModel::importFrom)
            ClearAllDataRow(onClearAllData = viewModel::clearAllData)
            PrivacyPolicyRow(
                onOpenPrivacy = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, BuildConfig.PRIVACY_POLICY_URL.toUri()))
                },
            )
            ListItem(
                headlineContent = { Text("About & licenses") },
                supportingContent = { Text("Version, source repository, privacy, and open-source notices") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier.clickable { context.startActivity(Intent(context, AboutActivity::class.java)) },
            )
        }
    }
}

@Composable
internal fun BackupImportRow(onImport: (Uri) -> Unit) {
    var pendingImportUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingImportUri = uri }
    ListItem(
        headlineContent = { Text("Import backup") },
        supportingContent = { Text("Replace current Tomi data with a backup") },
        leadingContent = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
        trailingContent = {
            TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import") }
        },
    )
    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Replace Tomi data?") },
            text = {
                Text(
                    "Importing this backup will replace all current tasks, journal entries, movement, check-ins, water, steps, and settings. Export a backup first if you want to keep them.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        onImport(uri)
                    },
                ) { Text("Replace data") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimePickerRow(
    title: String,
    minuteOfDay: Int,
    onTimeSelected: (Int) -> Unit,
    showLeadingIcon: Boolean = false,
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val pickerTitle = "Reminder ${title.lowercase()}"
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(formatMinute(minuteOfDay)) },
        leadingContent = if (showLeadingIcon) {
            { Icon(Icons.Outlined.Schedule, contentDescription = null) }
        } else {
            null
        },
        trailingContent = {
            TextButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.semantics { contentDescription = "Change reminder ${title.lowercase()}" },
            ) { Text("Change") }
        },
    )
    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(pickerTitle) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(state.hour * 60 + state.minute)
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun BackgroundHealthRow(
    granted: Boolean,
    onRequestBackgroundPermissions: () -> Unit,
) {
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("Background step access") },
        supportingContent = {
            Text(
                if (granted) {
                    "Allowed · smart pause can refresh while Tomi is closed"
                } else {
                    "Optional · allows smart pause and widget refreshes while Tomi is closed"
                },
            )
        },
        leadingContent = { Icon(Icons.Outlined.HealthAndSafety, contentDescription = null) },
        trailingContent = {
            TextButton(onClick = { showDisclosure = true }) {
                Text(if (granted) "Manage" else "Allow")
            }
        },
    )
    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Allow background step access?") },
            text = {
                Text(
                    "Tomi will read new step totals from Health Connect while the app is closed to reset your movement timer after walking. " +
                        "This is optional. Foreground step tracking and reminders work without it.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisclosure = false
                        onRequestBackgroundPermissions()
                    },
                ) { Text("Allow access") }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text("Not now") }
            },
        )
    }
}

@Composable
internal fun ClearAllDataRow(onClearAllData: () -> Unit) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("Clear all data") },
        supportingContent = { Text("Delete everything stored by Tomi on this device") },
        leadingContent = {
            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        trailingContent = {
            TextButton(onClick = { showClearDialog = true }) { Text("Clear") }
        },
    )
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all Tomi data?") },
            text = { Text("This cannot be undone. Export a backup first if you want to keep your data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllData()
                        showClearDialog = false
                    },
                ) { Text("Clear everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun PrivacyPolicyRow(onOpenPrivacy: () -> Unit) {
    ListItem(
        headlineContent = { Text("Privacy policy") },
        supportingContent = {
            Text("No account, analytics, ads, or cloud upload. Step totals are cached locally; Health Connect remains read-only.")
        },
        leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
        trailingContent = {
            TextButton(onClick = onOpenPrivacy) { Text("Open") }
        },
    )
}

@Composable
internal fun WeekdayChips(
    activeDays: Set<DayOfWeek>,
    onDayToggle: (DayOfWeek) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            val selected = day in activeDays
            FilterChip(
                selected = selected,
                onClick = { onDayToggle(day) },
                modifier = Modifier.semantics {
                    contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                },
                label = { Text(day.name.take(1)) },
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

private fun formatMinute(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
