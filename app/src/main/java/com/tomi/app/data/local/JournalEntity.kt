package com.tomi.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "journal_entries")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val mood: Int? = null,
    val tags: String = "",
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY pinned DESC, updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<JournalEntity>>

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE title LIKE '%' || :query || '%'
            OR body LIKE '%' || :query || '%'
            OR tags LIKE '%' || :query || '%'
        ORDER BY pinned DESC, updatedAt DESC, id DESC
        """,
    )
    fun search(query: String): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Long): JournalEntity?

    @Insert
    suspend fun insert(entry: JournalEntity): Long

    @Update
    suspend fun update(entry: JournalEntity)

    @Query("UPDATE journal_entries SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Delete
    suspend fun delete(entry: JournalEntity)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}
