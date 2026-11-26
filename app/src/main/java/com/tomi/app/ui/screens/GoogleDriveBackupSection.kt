package com.tomi.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiViewModel
import com.tomi.app.ui.components.SettingsToggleRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GoogleDriveBackupSection(
    viewModel: TomiViewModel,
    settings: TomiSettings,
) {
    val context = LocalContext.current
    var showRestoreDialog by rememberSaveable { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let(viewModel::connectGoogleDrive)
    }
    val connected = settings.googleDriveTreeUri != null
    val folderName = settings.googleDriveFolderName ?: "Google Drive"
    val lastBackup = settings.googleDriveLastBackupAt?.let(::formatBackupTime) ?: "No backup yet"

    SettingsSection("Google Drive backup")
    if (!connected) {
        ListItem(
            headlineContent = { Text("Google Drive") },
            supportingContent = {
                Text(
                    "Choose a folder in Google Drive. Tomi writes Tomi Backups/tomi-backup.json there; " +
                        "the folder grant is all Android shares, not your Google password.",
                )
            },
            leadingContent = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
            trailingContent = {
                Button(onClick = { folderPicker.launch(null) }) { Text("Connect") }
            },
        )
        Text(
            "Google Drive must be available in Android's folder picker. Other document providers also work.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    } else {
        ListItem(
            headlineContent = { Text("Google Drive") },
            supportingContent = {
                Text("Connected to $folderName · Last backup: $lastBackup")
            },
            leadingContent = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
            trailingContent = {
                TextButton(onClick = { folderPicker.launch(null) }) { Text("Change") }
            },
        )
        ListItem(
            headlineContent = { Text("Back up now") },
            supportingContent = { Text("Upload the current Tomi data to Tomi Backups/tomi-backup.json") },
            leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
            trailingContent = {
                TextButton(onClick = viewModel::backupToGoogleDrive) { Text("Back up") }
            },
        )
        ListItem(
            headlineContent = { Text("Restore from Google Drive") },
            supportingContent = { Text("Replace local Tomi data with the latest Drive backup") },
            leadingContent = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
            trailingContent = {
                TextButton(onClick = { showRestoreDialog = true }) { Text("Restore") }
            },
        )
        SettingsToggleRow(
            title = "Sync automatically",
            supportingText = "Back up when Tomi closes and every six hours while connected",
            checked = settings.googleDriveAutoSync,
            onCheckedChange = viewModel::setGoogleDriveAutoSync,
            leadingIcon = Icons.Outlined.Cloud,
        )
        ListItem(
            headlineContent = { Text("Disconnect Google Drive") },
            supportingContent = { Text("Stop syncing. The backup file remains in your Drive.") },
            leadingContent = {
                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            trailingContent = {
                TextButton(onClick = viewModel::disconnectGoogleDrive) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Replace Tomi data from Google Drive?") },
            text = {
                Text(
                    "This replaces all current tasks, journal entries, movement, check-ins, water, steps, and settings " +
                        "with the latest backup in Tomi Backups/tomi-backup.json. Export a local backup first if you want to keep them.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        viewModel.restoreFromGoogleDrive()
                    },
                ) { Text("Replace data") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            },
        )
    }
}

private fun formatBackupTime(value: Long): String =
    DateTimeFormatter.ofPattern("MMM d · HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
