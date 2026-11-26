package com.tomi.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "prayer_logs", primaryKeys = ["date", "prayer"])
data class PrayerLogEntity(
    val date: String,
    val prayer: String,
    val offered: Boolean,
    val updatedAt: Long,
)

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs WHERE date = :date ORDER BY prayer ASC")
    fun observeForDate(date: String): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs ORDER BY date DESC, prayer ASC")
    fun observeAll(): Flow<List<PrayerLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PrayerLogEntity)

    @Query("DELETE FROM prayer_logs")
    suspend fun deleteAll()
}
