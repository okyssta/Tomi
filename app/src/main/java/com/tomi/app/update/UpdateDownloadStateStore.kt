package com.tomi.app.update

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
internal data class PendingUpdateDownload(
    val info: UpdateInfo,
    val workId: String,
    val enqueuedAt: Long,
)

private val Context.updateDownloadStore by preferencesDataStore(
    name = "update_download_state",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
private val PENDING_UPDATE = stringPreferencesKey("pending_update")

internal object UpdateDownloadStateStore {
    suspend fun save(context: Context, pending: PendingUpdateDownload) {
        val content = UpdateDownloadStateCodec.encode(pending)
        context.applicationContext.updateDownloadStore.edit { preferences ->
            preferences[PENDING_UPDATE] = content
        }
    }

    suspend fun read(context: Context): PendingUpdateDownload? {
        val content = context.applicationContext.updateDownloadStore.data.first()[PENDING_UPDATE] ?: return null
        return UpdateDownloadStateCodec.decode(content)
    }

    suspend fun clear(context: Context) {
        context.applicationContext.updateDownloadStore.edit { it.remove(PENDING_UPDATE) }
    }
}
