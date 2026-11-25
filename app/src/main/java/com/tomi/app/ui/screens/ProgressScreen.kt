package com.tomi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Delete

import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.tomi.app.data.local.MovementEntity
import com.tomi.app.data.local.MovementType
import com.tomi.app.ui.components.ConfirmDeleteDialog
import com.tomi.app.domain.movement.MovementStreak
import com.tomi.app.health.StepSyncPlanner
import com.tomi.app.ui.TomiViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: TomiViewModel) {
    val stepCache by viewModel.stepCache.collectAsStateWithLifecycle()
    val movements by viewModel.movements.collectAsStateWithLifecycle()
    val todayMovements by viewModel.todayMovements.collectAsStateWithLifecycle()
    val steps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val dayContext by viewModel.dayContext.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<MovementEntity?>(null) }
    val today = dayContext.date
    val days = remember(today) { StepSyncPlanner.lastDays(today, 7) }
    val weeklySteps = remember(stepCache, days) {
        days.map { day -> stepCache.firstOrNull { it.date == day.toString() }?.steps ?: 0L }
    }
    val streak = remember(movements, today) {
        MovementStreak.calculate(
            movements.map { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet(),
            today,
        )
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(weeklySteps) {
        modelProducer.runTransaction { columnSeries { series(weeklySteps) } }
    }
    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(),
        startAxis = VerticalAxis.rememberStart(),
        bottomAxis = HorizontalAxis.rememberBottom(),
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Progress") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Steps this week", style = MaterialTheme.typography.titleMedium)
                        }
                        CartesianChartHost(
                            chart = chart,
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .semantics {
                                    contentDescription = weeklyStepsDescription(days, weeklySteps)
                                },
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clearAndSetSemantics { },
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            days.forEach { day ->
                                Text(
                                    text = day.dayOfWeek.name.take(1),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Steps today",
                        value = steps?.let { "%,d".format(it) } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Movement streak",
                        value = "$streak ${if (streak == 1) "day" else "days"}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Breaks today",
                        value = todayMovements.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Active minutes",
                        value = (todayMovements.sumOf { it.durationSeconds } / 60).toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text("Movement history", style = MaterialTheme.typography.titleMedium)
            }
            if (movements.isEmpty()) {
                item {
                    Card {
                        ListItem(
                            headlineContent = { Text("No movement logged yet") },
                            supportingContent = { Text("Start a walk, stretch, or push-up set from Home.") },
                        )
                    }
                }
            } else {
                items(movements.take(50), key = { it.id }) { session ->
                    MovementRow(session = session, onDelete = { pendingDelete = session })
                }
            }
        }
    }
    pendingDelete?.let { session ->
        ConfirmDeleteDialog(
            title = "Delete movement?",
            message = "This ${session.type.label.lowercase()} session will be permanently deleted.",
            onConfirm = {
                viewModel.deleteMovement(session)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

internal fun weeklyStepsDescription(days: List<LocalDate>, steps: List<Long>): String {
    require(days.size == steps.size)
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
    return buildString {
        append("Weekly steps. Total ")
        append("%,d".format(Locale.US, steps.sum()))
        append('.')
        days.forEachIndexed { index, day ->
            append(' ')
            append(day.format(formatter))
            append(": ")
            append("%,d".format(Locale.US, steps[index]))
            append(if (steps[index] == 1L) " step." else " steps.")
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MovementRow(session: MovementEntity, onDelete: () -> Unit) {
    val icon = when (session.type) {
        MovementType.WALK -> Icons.AutoMirrored.Outlined.DirectionsWalk
        MovementType.PUSHUPS -> Icons.Outlined.FitnessCenter
        MovementType.STRETCH, MovementType.STAND -> Icons.Outlined.SelfImprovement
    }
    val time = DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(session.startedAt))
    val duration = if (session.durationSeconds >= 60) {
        "${session.durationSeconds / 60}m ${session.durationSeconds % 60}s"
    } else {
        "${session.durationSeconds}s"
    }
    ListItem(
        headlineContent = { Text(session.type.label) },
        supportingContent = { Text(listOf(time, duration, session.reps?.let { "$it reps" }).joinToString(" · ")) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete movement") }
        },
    )
}
