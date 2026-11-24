package com.tomi.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_check_ins")
data class DailyCheckInEntity(
    @PrimaryKey
    val date: String,
    val mood: Int,
    val energy: Int,
    val note: String = "",
    val updatedAt: Long,
)

@Dao
interface CheckInDao {
    @Query("SELECT * FROM daily_check_ins WHERE date = :date")
    fun observeDate(date: String): Flow<DailyCheckInEntity?>

    @Query("SELECT * FROM daily_check_ins ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<DailyCheckInEntity>>

    @Upsert
    suspend fun upsert(checkIn: DailyCheckInEntity)

    @Query("DELETE FROM daily_check_ins")
    suspend fun deleteAll()
}
