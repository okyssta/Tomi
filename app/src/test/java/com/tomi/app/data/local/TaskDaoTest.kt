package com.tomi.app.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class TaskDaoTest {

    private lateinit var database: TomiDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TomiDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `task can be inserted observed completed and deleted`() = runBlocking {
        val dao = database.taskDao()
        val id = dao.insert(
            TaskEntity(
                title = "Take a walk",
                notes = "Around the block",
                dueAt = 1_800_000_000_000L,
                priority = 2,
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_000_000L,
            ),
        )

        val inserted = dao.observeAll().first().single()
        assertEquals("Take a walk", inserted.title)
        assertEquals(id, inserted.id)
        assertEquals(false, inserted.completed)

        dao.setCompleted(id, completed = true, completedAt = 1_800_000_000_001L)
        val completed = dao.getById(id)
        assertTrue(requireNotNull(completed).completed)
        assertEquals(1_800_000_000_001L, completed.completedAt)

        dao.delete(completed)
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun `task details can be edited`() = runBlocking {
        val dao = database.taskDao()
        val id = dao.insert(
            TaskEntity(
                title = "Old title",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        val task = requireNotNull(dao.getById(id))

        dao.update(task.copy(title = "New title", notes = "Updated notes", updatedAt = 2L))

        val edited = requireNotNull(dao.getById(id))
        assertEquals("New title", edited.title)
        assertEquals("Updated notes", edited.notes)
    }
}
