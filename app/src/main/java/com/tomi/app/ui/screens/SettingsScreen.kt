package com.tomi.app.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomi.app.BuildConfig
import com.tomi.app.data.settings.ThemeMode
import com.tomi.app.health.HealthStatus
import com.tomi.app.ui.TomiViewModel
import com.tomi.app.ui.UpdateUiState
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: TomiViewModel,
    onConnectHealth: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val healthStatus by viewModel.healthStatus.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var timePickerForStart by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importFrom) }

    val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val exactAlarmsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            SettingsSection("Movement reminders")
            ListItem(
                headlineContent = { Text("Remind me to move") },
                supportingContent = { Text("Every ${settings.reminder.intervalMinutes} minutes during your active hours") },
                leadingContent = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = settings.reminder.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(reminder = it.reminder.copy(enabled = enabled)) }
                        },
                    )
                },
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
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            ListItem(
                headlineContent = { Text("Active hours") },
                supportingContent = {
                    Text(
                        "${formatMinute(settings.reminder.activeStartMinutes)} – ${formatMinute(settings.reminder.activeEndMinutes)}",
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                trailingContent = {
                    TextButton(onClick = { timePickerForStart = true; showTimePicker = true }) { Text("Start") }
                },
            )
            ListItem(
                headlineContent = { Text("End time") },
                supportingContent = { Text(formatMinute(settings.reminder.activeEndMinutes)) },
                trailingContent = {
                    TextButton(onClick = { timePickerForStart = false; showTimePicker = true }) { Text("Change") }
                },
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in settings.reminder.activeDays
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val days = settings.reminder.activeDays.toMutableSet()
                            if (selected) days.remove(day) else days.add(day)
                            viewModel.updateSettings { it.copy(reminder = it.reminder.copy(activeDays = days)) }
                        },
                        label = { Text(day.name.take(1)) },
                    )
                }
            }
            ListItem(
                headlineContent = { Text("Smart pause") },
                supportingContent = { Text("Reset the timer when Health Connect detects about 50 steps") },
                trailingContent = {
                    Switch(
                        checked = settings.smartPause,
                        onCheckedChange = { value -> viewModel.updateSettings { it.copy(smartPause = value) } },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Sound") },
                trailingContent = {
                    Switch(
                        checked = settings.reminder.sound,
                        onCheckedChange = { value ->
                            viewModel.updateSettings { it.copy(reminder = it.reminder.copy(sound = value)) }
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Vibration") },
                trailingContent = {
                    Switch(
                        checked = settings.reminder.vibrate,
                        onCheckedChange = { value ->
                            viewModel.updateSettings { it.copy(reminder = it.reminder.copy(vibrate = value)) }
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Test reminder") },
                supportingContent = { Text("Send a sample notification") },
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
                        TextButton(onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
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
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
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
                modifier = Modifier.padding(horizontal = 20.dp),
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
                modifier = Modifier.padding(horizontal = 20.dp),
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
            ListItem(
                headlineContent = { Text("Material You colors") },
                supportingContent = { Text("Use colors from your wallpaper on Android 12+") },
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = { value -> viewModel.updateSettings { it.copy(dynamicColor = value) } },
                    )
                },
            )

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
                            is UpdateUiState.Error -> state.message
                        },
                    )
                },
                leadingContent = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                trailingContent = {
                    TextButton(onClick = viewModel::checkForUpdates) { Text("Check") }
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
            ListItem(
                headlineContent = { Text("Download updates automatically") },
                supportingContent = { Text("Wi-Fi only; SHA-256 verified; installation always asks first") },
                trailingContent = {
                    Switch(
                        checked = settings.autoDownloadUpdates,
                        onCheckedChange = { value -> viewModel.updateSettings { it.copy(autoDownloadUpdates = value) } },
                    )
                },
            )
            if (!context.packageManager.canRequestPackageInstalls()) {
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
            ListItem(
                headlineContent = { Text("Import backup") },
                supportingContent = { Text("Replace current Tomi data with a backup") },
                leadingContent = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                trailingContent = {
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import") }
                },
            )
            ListItem(
                headlineContent = { Text("Clear all data") },
                supportingContent = { Text("Delete everything stored by Tomi on this device") },
                leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                trailingContent = { TextButton(onClick = { showClearDialog = true }) { Text("Clear") } },
            )
            ListItem(
                headlineContent = { Text("Privacy") },
                supportingContent = { Text("No account, analytics, ads, or cloud upload. Health data stays in Health Connect.") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
            )
        }
    }

    if (showTimePicker) {
        val initialMinute = if (timePickerForStart) settings.reminder.activeStartMinutes else settings.reminder.activeEndMinutes
        val state = rememberTimePickerState(
            initialHour = initialMinute / 60,
            initialMinute = initialMinute % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (timePickerForStart) "Reminder start time" else "Reminder end time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minute = state.hour * 60 + state.minute
                        viewModel.updateSettings { current ->
                            val reminder = if (timePickerForStart) {
                                current.reminder.copy(activeStartMinutes = minute)
                            } else {
                                current.reminder.copy(activeEndMinutes = minute)
                            }
                            current.copy(reminder = reminder)
                        }
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all Tomi data?") },
            text = { Text("This cannot be undone. Export a backup first if you want to keep your data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                ) { Text("Clear everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
        )
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
