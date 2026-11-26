package com.tomi.app.data.local

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
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class PrayerLogDaoTest {
    private lateinit var database: TomiDatabase
    private lateinit var dao: PrayerLogDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TomiDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.prayerLogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upserts and replaces a prayer for one date`() = runBlocking {
        dao.upsert(PrayerLogEntity("2026-11-26", "FAJR", true, 1L))
        dao.upsert(PrayerLogEntity("2026-11-26", "FAJR", false, 2L))
        dao.upsert(PrayerLogEntity("2026-11-26", "ASR", true, 3L))

        val entries = dao.observeForDate("2026-11-26").first()

        assertEquals(2, entries.size)
        assertEquals(false, entries.first { it.prayer == "FAJR" }.offered)
        assertEquals(true, entries.first { it.prayer == "ASR" }.offered)
    }

    @Test
    fun `date query does not leak other days`() = runBlocking {
        dao.upsert(PrayerLogEntity("2026-11-26", "FAJR", true, 1L))
        dao.upsert(PrayerLogEntity("2026-11-27", "FAJR", true, 2L))

        val entries = dao.observeForDate("2026-11-26").first()

        assertEquals(1, entries.size)
        assertEquals("2026-11-26", entries.single().date)
    }

    @Test
    fun `delete all clears the tracker`() = runBlocking {
        dao.upsert(PrayerLogEntity("2026-11-26", "FAJR", true, 1L))
        dao.deleteAll()

        assertTrue(dao.observeAll().first().isEmpty())
    }
}
