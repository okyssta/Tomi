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
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomi.app.data.local.JournalEntity
import com.tomi.app.ui.components.ConfirmDeleteDialog
import com.tomi.app.ui.components.SettingsToggleRow
import com.tomi.app.ui.TomiViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private data class PendingJournalDelete(val id: Long, val title: String) {
    fun toSaveable(): List<Any> = listOf(id, title)

    companion object {
        fun fromSaveable(value: List<*>): PendingJournalDelete =
            PendingJournalDelete(value[0] as Long, value[1] as String)
    }
}

private val PendingJournalDeleteSaver: Saver<PendingJournalDelete?, Any> = Saver(
    save = { it?.toSaveable() ?: false },
    restore = { value ->
        when (value) {
            false -> null
            is List<*> -> PendingJournalDelete.fromSaveable(value)
            else -> error("Invalid pending journal delete state")
        }
    },
)

private val moodOptions = listOf(
    1 to "😞 Low",
    2 to "😕 Meh",
    3 to "😐 Okay",
    4 to "🙂 Good",
    5 to "😄 Great",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: TomiViewModel) {
    val entries by viewModel.journal.collectAsStateWithLifecycle()
    JournalScreen(
        entries = entries,
        onSaveEntry = viewModel::saveJournal,
        onDeleteEntry = viewModel::deleteJournal,
        onTogglePin = viewModel::toggleJournalPin,
        editorContent = { entry, onDismiss, onSave, onDelete ->
            JournalEditorSheet(entry, onDismiss, onSave, onDelete)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private val DefaultJournalEditorContent: @Composable (JournalEntity, () -> Unit, (JournalEntity, (Boolean) -> Unit) -> Unit, (JournalEntity) -> Unit) -> Unit =
    { entry, onDismiss, onSave, onDelete -> JournalEditorSheet(entry, onDismiss, onSave, onDelete) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalScreen(
    entries: List<JournalEntity>,
    onSaveEntry: (JournalEntity, (Boolean) -> Unit) -> Unit,
    onDeleteEntry: (JournalEntity) -> Unit,
    onTogglePin: (JournalEntity) -> Unit,
    editorContent: @Composable (JournalEntity, () -> Unit, (JournalEntity, (Boolean) -> Unit) -> Unit, (JournalEntity) -> Unit) -> Unit = DefaultJournalEditorContent,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var pinnedOnly by rememberSaveable { mutableStateOf(false) }
    var editingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDelete by rememberSaveable(stateSaver = PendingJournalDeleteSaver) { mutableStateOf<PendingJournalDelete?>(null) }
    var editorGeneration by remember { mutableStateOf(0L) }
    val editingEntry = when (editingEntryId) {
        null -> null
        0L -> JournalEntity(
            title = "",
            body = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        else -> entries.firstOrNull { it.id == editingEntryId }
    }

    val visibleEntries = entries.filter { entry ->
        val matchesQuery = query.isBlank() ||
            entry.title.contains(query, ignoreCase = true) ||
            entry.body.contains(query, ignoreCase = true) ||
            entry.tags.contains(query, ignoreCase = true)
        !pinnedOnly || entry.pinned
    }.let { list -> if (query.isBlank()) list else list }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Journal") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingEntryId = 0L; editorGeneration++ },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("New entry") },
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
                placeholder = { Text("Search journal") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = pinnedOnly,
                    onClick = { pinnedOnly = !pinnedOnly },
                    label = { Text("Pinned") },
                    leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                )
            }
            if (visibleEntries.isEmpty()) {
                EmptyJournalState(hasEntries = entries.isNotEmpty(), modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
                    items(visibleEntries, key = { it.id }) { entry ->
                        JournalRow(
                            entry = entry,
                            onEdit = { editingEntryId = entry.id; editorGeneration++ },
                            onPin = { onTogglePin(entry) },
                            onDelete = { pendingDelete = PendingJournalDelete(entry.id, entry.title) },
                        )
                    }
                }
            }
        }
    }

    editingEntry?.let { entry ->
        editorContent(
            entry,
            { editingEntryId = null; editorGeneration++ },
            { entryToSave, onSaved ->
                val saveGeneration = editorGeneration
                onSaveEntry(entryToSave) { saved ->
                    if (editorGeneration != saveGeneration) return@onSaveEntry
                    onSaved(saved)
                    if (saved) editingEntryId = null
                }
            },
            { entryToDelete ->
                val deleteGeneration = editorGeneration
                onDeleteEntry(entryToDelete)
                if (editorGeneration == deleteGeneration) editingEntryId = null
            },
        )
    }
    pendingDelete?.let { pending ->
        val entry = entries.firstOrNull { it.id == pending.id }
            ?: JournalEntity(id = pending.id, title = pending.title, body = "", createdAt = 0L, updatedAt = 0L)
        ConfirmDeleteDialog(
            title = "Delete journal entry?",
            message = "“${entry.title}” will be permanently deleted.",
            onConfirm = {
                onDeleteEntry(entry)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalRow(
    entry: JournalEntity,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateText = DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(entry.updatedAt))
    val moodText = moodOptions.firstOrNull { it.first == entry.mood }?.second
    ListItem(
        overlineContent = { Text(listOfNotNull(dateText, moodText).joinToString(" · ")) },
        headlineContent = { Text(entry.title) },
        supportingContent = {
            Text(
                text = entry.body.lineSequence().firstOrNull { it.isNotBlank() } ?: "No additional text",
                maxLines = 2,
            )
        },
        leadingContent = {
            if (entry.pinned) Icon(Icons.Outlined.PushPin, contentDescription = "Pinned")
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Journal options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (entry.pinned) "Unpin" else "Pin") },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                        onClick = { menuExpanded = false; onPin() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        },
    )
}

@Composable
private fun EmptyJournalState(hasEntries: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(if (hasEntries) "No matching entries" else "Your journal is empty", style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (hasEntries) "Try another search." else "Capture a thought, a win, or how today felt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalEditorSheet(
    entry: JournalEntity,
    onDismiss: () -> Unit,
    onSave: (JournalEntity, (Boolean) -> Unit) -> Unit,
    onDelete: (JournalEntity) -> Unit,
) {
    var title by rememberSaveable(entry.id) { mutableStateOf(entry.title) }
    var body by rememberSaveable(entry.id) { mutableStateOf(entry.body) }
    var tags by rememberSaveable(entry.id) { mutableStateOf(entry.tags) }
    var mood by rememberSaveable(entry.id) { mutableStateOf(entry.mood) }
    var pinned by rememberSaveable(entry.id) { mutableStateOf(entry.pinned) }
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
            Text(if (entry.id == 0L) "New entry" else "Edit entry", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Write what is on your mind") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags") },
                placeholder = { Text("work, gratitude, ideas…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Mood", style = MaterialTheme.typography.titleSmall)
            moodOptions.forEach { (value, label) ->
                FilterChip(
                    selected = mood == value,
                    onClick = { mood = if (mood == value) null else value },
                    label = { Text(label) },
                )
            }
            SettingsToggleRow(
                title = "Pin entry",
                supportingText = "Keep it at the top of your journal",
                checked = pinned,
                onCheckedChange = { pinned = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entry.id != 0L) {
                    TextButton(onClick = { showDeleteDialog = true }) { Text("Delete") }
                }
                Box(Modifier.weight(1f))
                Button(
                    enabled = (title.isNotBlank() || body.isNotBlank()) && !saving && !saveInFlight,
                    onClick = {
                        if (saving || saveInFlight) return@Button
                        saving = true
                        saveInFlight = true
                        onSave(entry.copy(title = title, body = body, tags = tags, mood = mood, pinned = pinned)) { saved ->
                            saving = false
                            saveInFlight = false
                            if (saved) {
                                onDismiss()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Could not save entry. Your draft is still here.") }
                            }
                        }
                    },
                ) { Text(if (saving) "Saving…" else "Save") }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete journal entry?",
            message = "“${entry.title}” will be permanently deleted.",
            onConfirm = {
                showDeleteDialog = false
                onDelete(entry)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}
