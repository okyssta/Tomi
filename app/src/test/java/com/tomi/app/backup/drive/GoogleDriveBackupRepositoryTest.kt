package com.tomi.app.backup.drive

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.tomi.app.backup.BackupContentSource
import com.tomi.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class GoogleDriveBackupRepositoryTest {
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var settings: SettingsRepository
    private lateinit var store: FakeCloudStore
    private lateinit var backup: FakeBackup
    private lateinit var repository: GoogleDriveBackupRepository
    private val treeUri = Uri.parse("content://com.google.android.apps.docs.storage/tree/root%3ATomi")

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        file = Files.createTempFile("tomi-drive-settings", ".preferences_pb").toFile()
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        settings = SettingsRepository(dataStore)
        store = FakeCloudStore()
        backup = FakeBackup()
        repository = GoogleDriveBackupRepository(settings, backup, store, now = { 1_234L })
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun `connect persists the folder and first backup updates the timestamp`() = runBlocking {
        val name = repository.connect(treeUri)
        assertEquals("Tomi Drive", name)
        assertEquals(treeUri.toString(), settings.settings.first().googleDriveTreeUri)
        assertEquals("Tomi Drive", settings.settings.first().googleDriveFolderName)

        val at = repository.backupNow()

        assertEquals(1_234L, at)
        assertEquals("{\"backup\":true}", store.written)
        assertEquals(1_234L, settings.settings.first().googleDriveLastBackupAt)
    }

    @Test
    fun `automatic sync is a no-op when disabled`() = runBlocking {
        repository.connect(treeUri)
        settings.updateSettings { it.copy(googleDriveAutoSync = false) }

        assertNull(repository.syncIfEnabled())
        assertNull(store.written)

        settings.updateSettings { it.copy(googleDriveAutoSync = true) }
        assertEquals(1_234L, repository.syncIfEnabled())
        assertEquals("{\"backup\":true}", store.written)
    }

    @Test
    fun `disconnect releases the folder and clears connection state`() = runBlocking {
        repository.connect(treeUri)
        repository.disconnect()

        assertTrue(store.released)
        val current = settings.settings.first()
        assertNull(current.googleDriveTreeUri)
        assertNull(current.googleDriveFolderName)
        assertNull(current.googleDriveLastBackupAt)
    }

    @Test
    fun `restore opens the stored backup input`() = runBlocking {
        repository.connect(treeUri)
        store.input = ByteArrayInputStream("{\"restore\":true}".toByteArray())

        val content = repository.openBackupInput()?.use { it.readBytes().decodeToString() }

        assertEquals("{\"restore\":true}", content)
    }

    private class FakeCloudStore : CloudBackupStore {
        var written: String? = null
        var released = false
        var input: InputStream = ByteArrayInputStream(ByteArray(0))

        override fun persist(treeUri: Uri): String = "Tomi Drive"
        override fun release(treeUri: Uri) {
            released = true
        }

        override fun write(treeUri: Uri, content: String) {
            written = content
        }

        override fun openInput(treeUri: Uri): InputStream = input
    }

    private class FakeBackup : BackupContentSource {
        override suspend fun export(): String = "{\"backup\":true}"
        override suspend fun import(input: InputStream) = Unit
    }
}
