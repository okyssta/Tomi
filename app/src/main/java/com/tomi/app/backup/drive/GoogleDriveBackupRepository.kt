package com.tomi.app.backup.drive

import android.net.Uri
import androidx.core.net.toUri
import com.tomi.app.backup.BackupContentSource
import com.tomi.app.backup.DataMaintenanceCoordinator
import com.tomi.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.io.InputStream

class GoogleDriveBackupRepository(
    private val settingsRepository: SettingsRepository,
    private val backup: BackupContentSource,
    private val store: CloudBackupStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun connect(treeUri: Uri): String {
        val previous = settingsRepository.settings.first().googleDriveTreeUri
            ?.toUri()
        val folderName = store.persist(treeUri)
        if (previous != null && previous != treeUri) {
            store.release(previous)
        }
        settingsRepository.updateSettings {
            it.copy(
                googleDriveTreeUri = treeUri.toString(),
                googleDriveFolderName = folderName,
                googleDriveLastBackupAt = null,
            )
        }
        return folderName
    }

    suspend fun disconnect() {
        val treeUri = settingsRepository.settings.first().googleDriveTreeUri
            ?.toUri()
        treeUri?.let(store::release)
        settingsRepository.updateSettings {
            it.copy(
                googleDriveTreeUri = null,
                googleDriveFolderName = null,
                googleDriveLastBackupAt = null,
            )
        }
    }

    suspend fun backupNow(): Long {
        val treeUri = requireConfiguredUri()
        val content = backup.export()
        store.write(treeUri, content)
        val timestamp = now()
        settingsRepository.updateSettings { it.copy(googleDriveLastBackupAt = timestamp) }
        return timestamp
    }

    suspend fun syncIfEnabled(): Long? {
        val settings = settingsRepository.settings.first()
        if (!settings.googleDriveAutoSync || settings.googleDriveTreeUri == null) return null
        return DataMaintenanceCoordinator.exclusive { backupNow() }
    }

    suspend fun openBackupInput(): InputStream? {
        val treeUri = requireConfiguredUri()
        return store.openInput(treeUri)
    }

    suspend fun release(treeUri: String?) {
        treeUri?.let { store.release(it.toUri()) }
    }

    private suspend fun requireConfiguredUri(): Uri {
        return settingsRepository.settings.first().googleDriveTreeUri
            ?.toUri()
            ?: error("Google Drive backup is not connected")
    }
}
