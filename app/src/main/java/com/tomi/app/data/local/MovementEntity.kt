package com.tomi.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class MovementType(val label: String) {
    WALK("Walk"),
    PUSHUPS("Push-ups"),
    STRETCH("Stretch"),
    STAND("Stand & move"),
}

@Entity(tableName = "movement_sessions")
data class MovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: MovementType,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Int,
    val reps: Int? = null,
    val note: String = "",
)

@Dao
interface MovementDao {
    @Query("SELECT * FROM movement_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<MovementEntity>>

    @Query(
        "SELECT * FROM movement_sessions WHERE startedAt >= :start AND startedAt < :end ORDER BY startedAt DESC",
    )
    fun observeBetween(start: Long, end: Long): Flow<List<MovementEntity>>

    @Query(
        "SELECT COALESCE(SUM(durationSeconds), 0) FROM movement_sessions WHERE startedAt >= :start AND startedAt < :end",
    )
    suspend fun totalDurationSeconds(start: Long, end: Long): Long

    @Query(
        "SELECT COUNT(*) FROM movement_sessions WHERE startedAt >= :start AND startedAt < :end",
    )
    suspend fun countBetween(start: Long, end: Long): Long

    @Insert
    suspend fun insert(session: MovementEntity): Long

    @Query("DELETE FROM movement_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM movement_sessions")
    suspend fun deleteAll()
}
