package com.tomi.app.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
class JournalDaoTest {

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
    fun tearDown() = database.close()

    @Test
    fun `journal entries can be searched pinned and deleted`() = runBlocking {
        val dao = database.journalDao()
        val firstId = dao.insert(
            JournalEntity(
                title = "Morning walk",
                body = "Felt clearer after a short walk.",
                mood = 4,
                tags = "health, morning",
                pinned = false,
                createdAt = 1_000L,
                updatedAt = 1_000L,
            ),
        )
        dao.insert(
            JournalEntity(
                title = "Work notes",
                body = "Finished the report.",
                mood = 3,
                tags = "work",
                pinned = true,
                createdAt = 2_000L,
                updatedAt = 2_000L,
            ),
        )

        assertEquals(1, dao.search("walk").first().size)
        assertEquals("Work notes", dao.observeAll().first().first().title)

        dao.setPinned(firstId, true, updatedAt = 3_000L)
        assertEquals("Morning walk", dao.observeAll().first().first().title)

        dao.delete(requireNotNull(dao.getById(firstId)))
        assertTrue(dao.search("walk").first().isEmpty())
    }
}
