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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class DailyLogDaoTest {

    private lateinit var database: TomiDatabase
    private val dayStart = 1_700_000_000_000L
    private val dayEnd = dayStart + 86_400_000L

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
    fun `movement sessions are summarized for a day`() = runBlocking {
        val dao = database.movementDao()
        dao.insert(
            MovementEntity(
                type = MovementType.WALK,
                startedAt = dayStart + 3_600_000L,
                endedAt = dayStart + 3_900_000L,
                durationSeconds = 300,
            ),
        )
        dao.insert(
            MovementEntity(
                type = MovementType.PUSHUPS,
                startedAt = dayStart + 7_200_000L,
                endedAt = dayStart + 7_320_000L,
                durationSeconds = 120,
                reps = 10,
            ),
        )

        val sessions = dao.observeBetween(dayStart, dayEnd).first()
        assertEquals(2, sessions.size)
        assertEquals(420L, dao.totalDurationSeconds(dayStart, dayEnd))
        assertEquals(2L, dao.countBetween(dayStart, dayEnd))
    }

    @Test
    fun `daily check-in replaces the same date`() = runBlocking {
        val dao = database.checkInDao()
        dao.upsert(DailyCheckInEntity("2026-11-24", mood = 2, energy = 3, note = "Tired", updatedAt = 1L))
        dao.upsert(DailyCheckInEntity("2026-11-24", mood = 4, energy = 4, note = "Better", updatedAt = 2L))

        val checkIn = dao.observeDate("2026-11-24").first()
        assertEquals(4, checkIn?.mood)
        assertEquals("Better", checkIn?.note)
    }

    @Test
    fun `water entries sum into a daily total`() = runBlocking {
        val dao = database.waterDao()
        dao.insert(WaterEntity(amountMl = 250, timestamp = dayStart + 1_000L))
        dao.insert(WaterEntity(amountMl = 500, timestamp = dayStart + 2_000L))

        assertEquals(750L, dao.observeTotalMl(dayStart, dayEnd).first())
    }

    @Test
    fun `step cache replaces an existing day instead of double counting`() = runBlocking {
        val dao = database.stepCacheDao()
        dao.upsert(StepCacheEntity("2026-11-24", steps = 5_000L, updatedAt = 1L))
        dao.upsert(StepCacheEntity("2026-11-24", steps = 6_000L, updatedAt = 2L))

        assertEquals(6_000L, dao.observeDay("2026-11-24").first()?.steps)
    }
}
