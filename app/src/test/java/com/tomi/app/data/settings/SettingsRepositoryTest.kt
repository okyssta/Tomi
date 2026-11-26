package com.tomi.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SettingsRepositoryTest {

    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        file = Files.createTempFile("tomi-settings", ".preferences_pb").toFile()
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        repository = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun `updates settings without replacing unrelated values`() = runBlocking {
        repository.updateSettings { it.copy(stepGoal = 8_000, waterGoalMl = 2_500) }
        repository.updateSettings { it.copy(reminder = it.reminder.copy(intervalMinutes = 35)) }

        val settings = repository.settings.first()
        assertEquals(8_000, settings.stepGoal)
        assertEquals(2_500, settings.waterGoalMl)
        assertEquals(35, settings.reminder.intervalMinutes)
    }

    @Test
    fun `updates reminder runtime state`() = runBlocking {
        repository.updateRuntime { it.copy(lastMovementAt = 100L, snoozeUntil = 200L) }

        val state = repository.runtime.first()
        assertEquals(100L, state.lastMovementAt)
        assertEquals(200L, state.snoozeUntil)
    }

    @Test
    fun `persists google drive connection without disturbing other settings`() = runBlocking {
        repository.updateSettings {
            it.copy(
                stepGoal = 7_500,
                googleDriveTreeUri = "content://drive/tree/root",
                googleDriveFolderName = "Tomi Drive",
                googleDriveLastBackupAt = 999L,
                googleDriveAutoSync = false,
            )
        }

        val settings = repository.settings.first()
        assertEquals(7_500, settings.stepGoal)
        assertEquals("content://drive/tree/root", settings.googleDriveTreeUri)
        assertEquals("Tomi Drive", settings.googleDriveFolderName)
        assertEquals(999L, settings.googleDriveLastBackupAt)
        assertEquals(false, settings.googleDriveAutoSync)
    }
}
