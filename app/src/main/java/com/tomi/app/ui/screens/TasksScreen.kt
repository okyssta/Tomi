package com.tomi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomi.app.data.local.TaskEntity
import com.tomi.app.ui.components.ConfirmDeleteDialog
import com.tomi.app.ui.TomiViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private enum class TaskFilter { ALL, OPEN, DONE }

private data class PendingTaskDelete(val id: Long, val title: String) {
    fun toSaveable(): List<Any> = listOf(id, title)

    companion object {
        fun fromSaveable(value: List<*>): PendingTaskDelete =
            PendingTaskDelete(value[0] as Long, value[1] as String)
    }
}

private val PendingTaskDeleteSaver: Saver<PendingTaskDelete?, Any> = Saver(
    save = { it?.toSaveable() ?: false },
    restore = { value ->
        when (value) {
            false -> null
            is List<*> -> PendingTaskDelete.fromSaveable(value)
            else -> error("Invalid pending task delete state")
        }
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TomiViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    TasksScreen(
        tasks = tasks,
        onToggleTask = viewModel::toggleTask,
        onSaveTask = viewModel::saveTask,
        onDeleteTask = viewModel::deleteTask,
        editorContent = { task, onDismiss, onSave, onDelete ->
            TaskEditorSheet(task, onDismiss, onSave, onDelete)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private val DefaultTaskEditorContent: @Composable (TaskEntity, () -> Unit, (TaskEntity, (Boolean) -> Unit) -> Unit, (TaskEntity) -> Unit) -> Unit =
    { task, onDismiss, onSave, onDelete -> TaskEditorSheet(task, onDismiss, onSave, onDelete) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasksScreen(
    tasks: List<TaskEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onSaveTask: (TaskEntity, (Boolean) -> Unit) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    editorContent: @Composable (TaskEntity, () -> Unit, (TaskEntity, (Boolean) -> Unit) -> Unit, (TaskEntity) -> Unit) -> Unit = DefaultTaskEditorContent,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(TaskFilter.ALL) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDelete by rememberSaveable(stateSaver = PendingTaskDeleteSaver) { mutableStateOf<PendingTaskDelete?>(null) }
    var editorGeneration by remember { mutableStateOf(0L) }
    val editingTask = when (editingTaskId) {
        null -> null
        0L -> TaskEntity(
            title = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        else -> tasks.firstOrNull { it.id == editingTaskId }
    }

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
                        onClick = { editingTaskId = 0L; editorGeneration++ },
                    ) { Icon(Icons.Outlined.Add, contentDescription = "Add task") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingTaskId = 0L; editorGeneration++ },
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
                            onToggle = { onToggleTask(task) },
                            onEdit = { editingTaskId = task.id; editorGeneration++ },
                            onDelete = { pendingDelete = PendingTaskDelete(task.id, task.title) },
                        )
                    }
                }
            }
        }
    }

    editingTask?.let { task ->
        editorContent(
            task,
            { editingTaskId = null; editorGeneration++ },
            { taskToSave, onSaved ->
                val saveGeneration = editorGeneration
                onSaveTask(taskToSave) { saved ->
                    if (editorGeneration != saveGeneration) return@onSaveTask
                    onSaved(saved)
                    if (saved) editingTaskId = null
                }
            },
            { taskToDelete ->
                val deleteGeneration = editorGeneration
                onDeleteTask(taskToDelete)
                if (editorGeneration == deleteGeneration) editingTaskId = null
            },
        )
    }
    pendingDelete?.let { pending ->
        val task = tasks.firstOrNull { it.id == pending.id }
            ?: TaskEntity(id = pending.id, title = pending.title, createdAt = 0L, updatedAt = 0L)
        ConfirmDeleteDialog(
            title = "Delete task?",
            message = "“${task.title}” will be permanently deleted.",
            onConfirm = {
                onDeleteTask(task)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TaskEditorSheet(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (TaskEntity, (Boolean) -> Unit) -> Unit,
    onDelete: (TaskEntity) -> Unit,
) {
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    var category by rememberSaveable(task.id) { mutableStateOf(task.category) }
    var priority by rememberSaveable(task.id) { mutableIntStateOf(task.priority) }
    val initialDue = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    var dueDate by rememberSaveable(task.id) { mutableStateOf(initialDue?.toLocalDate()) }
    var dueTime by rememberSaveable(task.id) { mutableStateOf(initialDue?.toLocalTime() ?: LocalTime.of(18, 0)) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveInFlight by remember { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SnackbarHost(snackbarHostState)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (dueDate != null) {
                            TextButton(
                                onClick = { dueDate = null },
                                modifier = Modifier.semantics { contentDescription = "Remove due date" },
                            ) { Text("Clear") }
                        }
                        TextButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.semantics { contentDescription = "Choose due date" },
                        ) { Text("Choose") }
                    }
                },
            )
            ListItem(
                headlineContent = { Text("Due time") },
                supportingContent = {
                    Text(dueTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                },
                leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                trailingContent = {
                    TextButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.semantics { contentDescription = "Choose due time" },
                    ) { Text("Choose") }
                },
            )
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    TextButton(onClick = { showDeleteDialog = true }) { Text("Delete") }
                }
                Box(Modifier.weight(1f))
                Button(
                    enabled = title.isNotBlank() && !saving && !saveInFlight,
                    onClick = {
                        if (saving || saveInFlight) return@Button
                        val dueAt = dueDate?.let { date ->
                            date.atTime(dueTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        saving = true
                        saveInFlight = true
                        onSave(
                            task.copy(
                                title = title,
                                notes = notes,
                                category = category,
                                priority = priority,
                                dueAt = dueAt,
                            ),
                        ) { saved ->
                            saving = false
                            saveInFlight = false
                            if (saved) {
                                onDismiss()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Could not save task. Your draft is still here.") }
                            }
                        }
                    },
                ) { Text(if (saving) "Saving…" else "Save") }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete task?",
            message = "“${task.title}” will be permanently deleted.",
            onConfirm = {
                showDeleteDialog = false
                onDelete(task)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate?.let(TaskDueDate::toPickerMillis))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            dueDate = TaskDueDate.fromPickerMillis(it)
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
            initialHour = dueTime.hour,
            initialMinute = dueTime.minute,
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
