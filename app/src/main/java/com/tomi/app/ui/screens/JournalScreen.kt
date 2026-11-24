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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomi.app.data.local.JournalEntity
import com.tomi.app.ui.TomiViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    var query by remember { mutableStateOf("") }
    var pinnedOnly by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntity?>(null) }

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
                onClick = {
                    val now = System.currentTimeMillis()
                    editingEntry = JournalEntity(title = "", body = "", createdAt = now, updatedAt = now)
                    editorOpen = true
                },
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
                            onEdit = {
                                editingEntry = entry
                                editorOpen = true
                            },
                            onPin = { viewModel.toggleJournalPin(entry) },
                            onDelete = { viewModel.deleteJournal(entry) },
                        )
                    }
                }
            }
        }
    }

    if (editorOpen) {
        editingEntry?.let { entry ->
            JournalEditorSheet(
                entry = entry,
                onDismiss = { editorOpen = false },
                onSave = {
                    viewModel.saveJournal(it)
                    editorOpen = false
                },
                onDelete = {
                    viewModel.deleteJournal(it)
                    editorOpen = false
                },
            )
        }
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
private fun JournalEditorSheet(
    entry: JournalEntity,
    onDismiss: () -> Unit,
    onSave: (JournalEntity) -> Unit,
    onDelete: (JournalEntity) -> Unit,
) {
    var title by remember(entry.id) { mutableStateOf(entry.title) }
    var body by remember(entry.id) { mutableStateOf(entry.body) }
    var tags by remember(entry.id) { mutableStateOf(entry.tags) }
    var mood by remember(entry.id) { mutableStateOf(entry.mood) }
    var pinned by remember(entry.id) { mutableStateOf(entry.pinned) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
            ListItem(
                headlineContent = { Text("Pin entry") },
                supportingContent = { Text("Keep it at the top of your journal") },
                trailingContent = { Switch(checked = pinned, onCheckedChange = { pinned = it }) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entry.id != 0L) {
                    TextButton(onClick = { onDelete(entry) }) { Text("Delete") }
                }
                Box(Modifier.weight(1f))
                Button(
                    onClick = {
                        onSave(entry.copy(title = title, body = body, tags = tags, mood = mood, pinned = pinned))
                    },
                ) { Text("Save") }
            }
        }
    }
}
