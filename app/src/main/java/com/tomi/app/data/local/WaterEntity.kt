package com.tomi.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "water_entries")
data class WaterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val timestamp: Long,
)

@Dao
interface WaterDao {
    @Query(
        "SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE timestamp >= :start AND timestamp < :end",
    )
    fun observeTotalMl(start: Long, end: Long): Flow<Long>

    @Insert
    suspend fun insert(entry: WaterEntity): Long

    @Query("SELECT * FROM water_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WaterEntity>>

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM water_entries")
    suspend fun deleteAll()
}
