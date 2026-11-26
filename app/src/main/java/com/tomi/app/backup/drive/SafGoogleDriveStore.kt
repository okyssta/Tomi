package com.tomi.app.backup.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream

class SafGoogleDriveStore(private val context: Context) : CloudBackupStore {
    private val resolver = context.contentResolver

    override fun persist(treeUri: Uri): String {
        resolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?.takeIf { it.isDirectory && it.canWrite() }
            ?: error("The selected folder is not writable")
        return root.name?.takeIf { it.isNotBlank() } ?: "Google Drive"
    }

    override fun release(treeUri: Uri) {
        runCatching {
            resolver.releasePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    override fun write(treeUri: Uri, content: String) {
        val file = backupFile(treeUri, create = true)
        resolver.openOutputStream(file.uri, "wt")?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("Could not open the Google Drive backup file")
    }

    override fun openInput(treeUri: Uri): InputStream {
        val file = backupFile(treeUri, create = false)
        return resolver.openInputStream(file.uri)
            ?: error("Could not open the Google Drive backup file")
    }

    private fun backupFile(treeUri: Uri, create: Boolean): DocumentFile {
        val folder = backupFolder(treeUri, create)
        folder.findFile(TOMI_BACKUP_FILE)?.let { existing ->
            if (existing.isDirectory) error("The Tomi backup name is occupied by a folder")
            return existing
        }
        if (!create) error("No Tomi backup was found in the selected Google Drive folder")
        return folder.createFile(MIME_JSON, TOMI_BACKUP_FILE)
            ?: error("Could not create the Google Drive backup file")
    }

    private fun backupFolder(treeUri: Uri, create: Boolean): DocumentFile {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?.takeIf { it.isDirectory }
            ?: error("The selected Google Drive folder is no longer available")
        root.findFile(TOMI_FOLDER)?.let { existing ->
            if (!existing.isDirectory) error("The Tomi backup name is occupied by a file")
            return existing
        }
        if (!create) error("No Tomi backup was found in the selected Google Drive folder")
        return root.createDirectory(TOMI_FOLDER)
            ?: error("Could not create the Tomi Backups folder in Google Drive")
    }

    companion object {
        const val TOMI_FOLDER = "Tomi Backups"
        const val TOMI_BACKUP_FILE = "tomi-backup.json"
        private const val MIME_JSON = "application/json"
    }
}
