package com.tomi.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "step_cache")
data class StepCacheEntity(
    @PrimaryKey
    val date: String,
    val steps: Long,
    val updatedAt: Long,
)

@Dao
interface StepCacheDao {
    @Query("SELECT * FROM step_cache WHERE date = :date")
    fun observeDay(date: String): Flow<StepCacheEntity?>

    @Query("SELECT * FROM step_cache WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeBetween(startDate: String, endDate: String): Flow<List<StepCacheEntity>>

    @Query("SELECT * FROM step_cache ORDER BY date ASC")
    fun observeAll(): Flow<List<StepCacheEntity>>

    @Upsert
    suspend fun upsert(entry: StepCacheEntity)

    @Query("DELETE FROM step_cache")
    suspend fun deleteAll()
}
