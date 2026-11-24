package com.tomi.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueAt: Long? = null,
    val priority: Int = PRIORITY_NORMAL,
    val category: String = "",
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val PRIORITY_LOW = 0
        const val PRIORITY_NORMAL = 1
        const val PRIORITY_HIGH = 2
    }
}

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        ORDER BY completed ASC,
            CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END,
            dueAt ASC,
            priority DESC,
            updatedAt DESC
        """,
    )
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query(
        """
        UPDATE tasks
        SET completed = :completed,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun setCompleted(
        id: Long,
        completed: Boolean,
        completedAt: Long?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE tasks SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
