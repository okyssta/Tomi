package com.tomi.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tomiSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "tomi_settings")

class SettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<TomiSettings> = dataStore.data.map { it.toTomiSettings() }
    val runtime: Flow<ReminderRuntimeState> = dataStore.data.map { it.toReminderRuntimeState() }

    suspend fun updateSettings(transform: (TomiSettings) -> TomiSettings) {
        dataStore.edit { preferences ->
            transform(preferences.toTomiSettings()).writeTo(preferences)
        }
    }

    suspend fun updateRuntime(transform: (ReminderRuntimeState) -> ReminderRuntimeState) {
        dataStore.edit { preferences ->
            transform(preferences.toReminderRuntimeState()).writeTo(preferences)
        }
    }

    suspend fun setLastUpdateCheckAt(timestamp: Long) {
        dataStore.edit { it[LAST_UPDATE_CHECK_AT] = timestamp }
    }

    suspend fun lastUpdateCheckAt(): Long? = dataStore.data.map { it[LAST_UPDATE_CHECK_AT] }.first()

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    companion object {
        fun from(context: Context): SettingsRepository =
            SettingsRepository(context.applicationContext.tomiSettingsStore)
    }
}
