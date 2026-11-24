package com.tomi.app.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Add

import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomi.app.data.local.MovementType
import com.tomi.app.domain.movement.MovementTimer
import com.tomi.app.ui.TomiViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoveScreen(
    initialType: MovementType,
    viewModel: TomiViewModel,
    onDone: () -> Unit,
) {
    var type by remember { mutableStateOf(initialType) }
    val timer = remember { MovementTimer() }
    var running by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var reps by remember { mutableIntStateOf(0) }
    var startedAtMillis by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    val targetSeconds = when (type) {
        MovementType.WALK, MovementType.STRETCH -> 120
        MovementType.STAND -> 60
        MovementType.PUSHUPS -> 0
    }

    LaunchedEffect(running) {
        while (running) {
            elapsedSeconds = timer.elapsedSeconds(SystemClock.elapsedRealtime())
            delay(1_000L)
        }
    }

    fun startTimer() {
        if (startedAtMillis == 0L) startedAtMillis = System.currentTimeMillis()
        timer.start(SystemClock.elapsedRealtime())
        running = true
        error = null
    }

    fun pauseTimer() {
        timer.pause(SystemClock.elapsedRealtime())
        elapsedSeconds = timer.elapsedSeconds(SystemClock.elapsedRealtime())
        running = false
    }

    fun reset() {
        if (running) timer.pause(SystemClock.elapsedRealtime())
        timer.finish(SystemClock.elapsedRealtime())
        running = false
        elapsedSeconds = 0
        reps = 0
        startedAtMillis = 0L
        error = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movement break") },
                actions = {
                    IconButton(onClick = ::reset) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Reset")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = {
                            reset()
                            type = option
                        },
                        label = { Text(option.label) },
                    )
                }
            }

            when (type) {
                MovementType.PUSHUPS -> PushUpCounter(
                    reps = reps,
                    enabled = startedAtMillis != 0L,
                    onStart = ::startTimer,
                    onIncrement = { if (startedAtMillis != 0L) reps++ },
                    onDecrement = { if (reps > 0) reps-- },
                )
                else -> TimedMovement(
                    type = type,
                    elapsedSeconds = elapsedSeconds,
                    targetSeconds = targetSeconds,
                    running = running,
                    onStart = ::startTimer,
                    onPause = ::pauseTimer,
                )
            }

            if (type == MovementType.PUSHUPS) {
                LinearProgressIndicator(
                    progress = { (reps / 10f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Target: 10 reps", style = MaterialTheme.typography.bodyMedium)
            } else if (targetSeconds > 0) {
                LinearProgressIndicator(
                    progress = { (elapsedSeconds / targetSeconds.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Suggested: $targetSeconds seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val nowElapsed = SystemClock.elapsedRealtime()
                    val duration = if (startedAtMillis == 0L) 0 else timer.finish(nowElapsed)
                    val valid = if (type == MovementType.PUSHUPS) reps > 0 else duration >= 1
                    if (!valid) {
                        error = if (type == MovementType.PUSHUPS) {
                            "Start a set and log at least one rep."
                        } else {
                            "Start the timer before finishing."
                        }
                    } else {
                        viewModel.logMovement(type, startedAtMillis, duration, reps.takeIf { type == MovementType.PUSHUPS })
                        onDone()
                    }
                },
            ) { Text(if (type == MovementType.PUSHUPS) "Finish set" else "Finish & log") }

            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            Text(
                text = "Move gently and stop if you feel pain or dizziness.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PushUpCounter(
    reps: Int,
    enabled: Boolean,
    onStart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!enabled) {
            Button(onClick = onStart) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text("Start set", Modifier.padding(start = 8.dp))
            }
        }
        Text(text = reps.toString(), style = MaterialTheme.typography.displayLarge)
        Text("reps", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onDecrement, enabled = reps > 0) {
                Icon(Icons.Outlined.Remove, contentDescription = "Subtract rep")
            }
            FilledIconButton(onClick = onIncrement, enabled = enabled) {
                Icon(Icons.Outlined.Add, contentDescription = "Add rep")
            }
        }
    }
}

@Composable
private fun TimedMovement(
    type: MovementType,
    elapsedSeconds: Int,
    targetSeconds: Int,
    running: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
) {
    val icon = when (type) {
        MovementType.WALK -> Icons.AutoMirrored.Outlined.DirectionsWalk
        MovementType.STRETCH, MovementType.STAND -> Icons.Outlined.SelfImprovement
        MovementType.PUSHUPS -> Icons.Outlined.FitnessCenter
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = formatElapsed(elapsedSeconds),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = when (type) {
                MovementType.WALK -> "Walk at a comfortable pace."
                MovementType.STRETCH -> "Roll your shoulders and stretch gently."
                MovementType.STAND -> "Stand tall and change position."
                MovementType.PUSHUPS -> "Complete controlled reps."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.padding(8.dp)) {
            if (running) {
                OutlinedButton(onClick = onPause) {
                    Icon(Icons.Outlined.Pause, contentDescription = null)
                    Text("Pause", Modifier.padding(start = 8.dp))
                }
            } else {
                Button(onClick = onStart) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Text(if (elapsedSeconds == 0) "Start" else "Resume", Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private fun formatElapsed(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
