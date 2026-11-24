package com.tomi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomi.app.data.local.TaskEntity
import com.tomi.app.ui.TomiViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TaskFilter { ALL, OPEN, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TomiViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val visibleTasks = tasks.filter { task ->
        val matchesQuery = query.isBlank() ||
            task.title.contains(query, ignoreCase = true) ||
            task.notes.contains(query, ignoreCase = true) ||
            task.category.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            TaskFilter.ALL -> true
            TaskFilter.OPEN -> !task.completed
            TaskFilter.DONE -> task.completed
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            editingTask = TaskEntity(title = "", createdAt = now, updatedAt = now)
                            editorOpen = true
                        },
                    ) { Icon(Icons.Outlined.Add, contentDescription = "Add task") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    editingTask = TaskEntity(title = "", createdAt = now, updatedAt = now)
                    editorOpen = true
                },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Add task") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search tasks") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
            if (visibleTasks.isEmpty()) {
                EmptyTasksState(
                    hasAnyTasks = tasks.isNotEmpty(),
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                ) {
                    items(visibleTasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleTask(task) },
                            onEdit = {
                                editingTask = task
                                editorOpen = true
                            },
                            onDelete = { viewModel.deleteTask(task) },
                        )
                    }
                }
            }
        }
    }

    if (editorOpen) {
        editingTask?.let { task ->
            TaskEditorSheet(
                task = task,
                onDismiss = { editorOpen = false },
                onSave = {
                    viewModel.saveTask(it)
                    editorOpen = false
                },
                onDelete = {
                    viewModel.deleteTask(it)
                    editorOpen = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dueText = task.dueAt?.let {
        DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm").withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(it))
    }
    ListItem(
        headlineContent = {
            Text(
                text = task.title,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            )
        },
        supportingContent = {
            val details = listOfNotNull(dueText, task.notes.takeIf { it.isNotBlank() }, task.category.takeIf { it.isNotBlank() })
            if (details.isNotEmpty()) Text(details.joinToString(" · "))
        },
        leadingContent = {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Task options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun EmptyTasksState(hasAnyTasks: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Checklist,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = if (hasAnyTasks) "No matching tasks" else "No tasks yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = if (hasAnyTasks) {
                "Try another search or filter."
            } else {
                "Add a task and keep your day in one place."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var notes by remember(task.id) { mutableStateOf(task.notes) }
    var category by remember(task.id) { mutableStateOf(task.category) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    val initialDue = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    var dueDate by remember(task.id) { mutableStateOf(initialDue?.toLocalDate()) }
    var dueTime by remember(task.id) { mutableStateOf(initialDue?.toLocalTime()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (task.id == 0L) "New task" else "Edit task",
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text("Work, personal, health…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            ListItem(
                headlineContent = { Text("Due date") },
                supportingContent = {
                    Text(
                        dueDate?.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                            ?: "No due date",
                    )
                },
                leadingContent = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                trailingContent = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Choose") }
                },
            )
            if (dueDate != null) {
                ListItem(
                    headlineContent = { Text("Due time") },
                    supportingContent = {
                        Text(dueTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "End of day")
                    },
                    leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = { showTimePicker = true }) { Text("Choose") }
                    },
                )
            }
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low" to TaskEntity.PRIORITY_LOW, "Normal" to TaskEntity.PRIORITY_NORMAL, "High" to TaskEntity.PRIORITY_HIGH)
                    .forEach { (label, value) ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(label) },
                        )
                    }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (task.id != 0L) {
                    TextButton(onClick = { onDelete(task) }) { Text("Delete") }
                }
                Box(Modifier.weight(1f))
                Button(
                    onClick = {
                        val dueAt = dueDate?.let { date ->
                            val time = dueTime ?: LocalTime.of(23, 59)
                            date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        onSave(
                            task.copy(
                                title = title,
                                notes = notes,
                                category = category,
                                priority = priority,
                                dueAt = dueAt,
                            ),
                        )
                    },
                ) { Text("Save") }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            dueDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = dueTime?.hour ?: 18,
            initialMinute = dueTime?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Due time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueTime = LocalTime.of(state.hour, state.minute)
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }
}
