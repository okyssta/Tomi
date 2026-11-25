package com.tomi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CheckCircle

import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomi.app.data.local.MovementType
import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.domain.movement.MovementStreak
import com.tomi.app.domain.reminder.ReminderPolicy
import com.tomi.app.health.HealthStatus
import com.tomi.app.ui.TomiViewModel
import com.tomi.app.ui.StepSyncUiState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: TomiViewModel,
    onStartMovement: (MovementType) -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSettings: () -> Unit,
    onConnectHealth: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val runtime by viewModel.runtime.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val movements by viewModel.movements.collectAsStateWithLifecycle()
    val dayContext by viewModel.dayContext.collectAsStateWithLifecycle()
    val todayMovements by viewModel.todayMovements.collectAsStateWithLifecycle()
    val steps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val waterMl by viewModel.waterMl.collectAsStateWithLifecycle()
    val canUndoWater by viewModel.canUndoWater.collectAsStateWithLifecycle()
    val checkIn by viewModel.checkIn.collectAsStateWithLifecycle()
    val healthStatus by viewModel.healthStatus.collectAsStateWithLifecycle()
    val stepSyncState by viewModel.stepSyncState.collectAsStateWithLifecycle()
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(60_000L)
        }
    }
    val nextReminder = remember(now, settings, runtime) {
        val baseline = listOfNotNull(runtime.lastMovementAt, runtime.lastReminderAt).maxOrNull()
        ReminderPolicy.nextReminderAt(
            now = now.atZone(ZoneId.systemDefault()),
            lastMovementAt = baseline?.let(Instant::ofEpochMilli),
            settings = settings.reminder,
            snoozeUntil = runtime.snoozeUntil?.let(Instant::ofEpochMilli),
        )
    }
    val today = dayContext.date
    val streak = remember(movements, today) {
        MovementStreak.calculate(
            movements.map { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet(),
            today,
        )
    }
    val openTasks = tasks.filterNot { it.completed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tomi", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = DateTimeFormatter.ofPattern("EEEE, MMM d").format(today),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                StepSummaryCard(
                    steps = steps,
                    goal = settings.stepGoal,
                    healthStatus = healthStatus,
                    syncState = stepSyncState,
                    onConnectHealth = onConnectHealth,
                    onRetrySteps = viewModel::refreshHealth,
                )
            }
            item {
                ReminderCard(
                    settings = settings,
                    runtime = runtime,
                    nextReminder = nextReminder,
                    now = now,
                    onStartMovement = { onStartMovement(MovementType.WALK) },
                    onSnooze = viewModel::snoozeReminder,
                    onOpenSettings = onOpenSettings,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickMovementButton(
                        label = "Walk",
                        icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartMovement(MovementType.WALK) },
                    )
                    QuickMovementButton(
                        label = "Push-ups",
                        icon = Icons.Outlined.FitnessCenter,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartMovement(MovementType.PUSHUPS) },
                    )
                    QuickMovementButton(
                        label = "Stretch",
                        icon = Icons.Outlined.SelfImprovement,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartMovement(MovementType.STRETCH) },
                    )
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Today", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${todayMovements.sumOf { it.durationSeconds } / 60} active min · $streak day streak",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "${todayMovements.size} movement ${if (todayMovements.size == 1) "break" else "breaks"} logged",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                Card {
                    Column {
                        ListItem(
                            headlineContent = { Text("Tasks") },
                            supportingContent = { Text("${openTasks.size} open") },
                            leadingContent = { Icon(Icons.Outlined.TaskAlt, contentDescription = null) },
                            trailingContent = { TextButton(onClick = onOpenTasks) { Text("View all") } },
                        )
                        if (openTasks.isEmpty()) {
                            ListItem(
                                headlineContent = { Text("Nothing pending") },
                                supportingContent = { Text("Add a task when you are ready.") },
                            )
                        } else {
                            openTasks.take(3).forEach { task ->
                                ListItem(
                                    headlineContent = { Text(task.title) },
                                    leadingContent = {
                                        Checkbox(checked = false, onCheckedChange = { viewModel.toggleTask(task) })
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                MoodCheckInCard(
                    mood = checkIn?.mood,
                    energy = checkIn?.energy,
                    onSave = viewModel::saveCheckIn,
                )
            }
            item {
                Card {
                    ListItem(
                        headlineContent = { Text("Water") },
                        supportingContent = { Text("${waterMl / 250} of ${settings.waterGoalMl / 250} glasses") },
                        leadingContent = { Icon(Icons.Outlined.LocalDrink, contentDescription = null) },
                        trailingContent = {
                            Row {
                                TextButton(onClick = viewModel::undoWater, enabled = canUndoWater) { Text("Undo") }
                                Button(onClick = { viewModel.addWater() }) { Text("+250 ml") }
                            }
                        },
                    )
                    LinearProgressIndicator(
                        progress = { (waterMl.toFloat() / settings.waterGoalMl).coerceIn(0f, 1f) },
                        drawStopIndicator = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepSummaryCard(
    steps: Long?,
    goal: Int,
    healthStatus: HealthStatus,
    syncState: StepSyncUiState,
    onConnectHealth: () -> Unit,
    onRetrySteps: () -> Unit,
) {
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Steps today", style = MaterialTheme.typography.titleMedium)
                Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = steps?.let { "%,d".format(it) } ?: "—",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "Goal: %,d".format(goal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { ((steps ?: 0L).toFloat() / goal).coerceIn(0f, 1f) },
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth(),
            )
            if (healthStatus != HealthStatus.AVAILABLE_GRANTED) {
                Text(
                    text = "Connect Health Connect to see steps from your phone and Google Fit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onConnectHealth) { Text("Connect steps") }
            } else if (syncState is StepSyncUiState.Error) {
                Text(
                    text = (syncState as StepSyncUiState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRetrySteps) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    settings: TomiSettings,
    runtime: ReminderRuntimeState,
    nextReminder: java.time.ZonedDateTime?,
    now: Instant,
    onStartMovement: () -> Unit,
    onSnooze: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Time to move", style = MaterialTheme.typography.titleMedium)
            }
            if (!settings.reminder.enabled) {
                Text("Reminders are off.", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onOpenSettings) { Text("Turn on reminders") }
            } else if (nextReminder == null) {
                Text("No reminders scheduled for the selected days.", style = MaterialTheme.typography.bodyLarge)
            } else {
                val millisUntil = nextReminder.toInstant().toEpochMilli() - now.toEpochMilli()
                val countdown = when {
                    millisUntil <= 0 -> "Due now"
                    millisUntil < 60_000 -> "Less than a minute"
                    millisUntil < 3_600_000 -> "${millisUntil / 60_000} min"
                    else -> "${millisUntil / 3_600_000} hr ${(millisUntil % 3_600_000) / 60_000} min"
                }
                Text(countdown, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = DateTimeFormatter.ofPattern("EEE · HH:mm").format(nextReminder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStartMovement) { Text("Start break") }
                    if (ReminderPolicy.isActiveAt(now.atZone(ZoneId.systemDefault()), settings.reminder)) {
                        TextButton(onClick = onSnooze) { Text("Snooze 10 min") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuickMovementButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = "$label movement", modifier = Modifier.size(20.dp))
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodCheckInCard(mood: Int?, energy: Int?, onSave: (Int, Int, String) -> Unit) {
    var selectedMood by remember(mood) { mutableStateOf(mood) }
    var selectedEnergy by remember(mood, energy) { mutableIntStateOf(energy ?: 3) }
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Mood, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Daily check-in", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = if (mood != null) "Checked in · mood $mood/5 · energy $energy/5" else "How are you feeling today?",
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("😞", "😕", "😐", "🙂", "😄").forEachIndexed { index, emoji ->
                    val value = index + 1
                    FilterChip(
                        selected = selectedMood == value,
                        onClick = { selectedMood = value },
                        label = { Text(emoji) },
                        modifier = Modifier.semantics { contentDescription = moodChipLabel(value) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low energy" to 1, "Okay" to 3, "High energy" to 5).forEach { (label, value) ->
                    FilterChip(
                        selected = selectedEnergy == value,
                        onClick = { selectedEnergy = value },
                        label = { Text(label) },
                    )
                }
            }
            Button(
                enabled = selectedMood != null,
                onClick = { selectedMood?.let { onSave(it, selectedEnergy, "") } },
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Text(if (mood == null) "Save check-in" else "Update check-in", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

internal fun moodChipLabel(value: Int): String {
    val label = listOf("very low", "low", "neutral", "good", "very good")[value - 1]
    return "Mood $value, $label"
}
