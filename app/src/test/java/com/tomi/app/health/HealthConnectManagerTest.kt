package com.tomi.app.health

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.test.core.app.ApplicationProvider
import com.tomi.app.backup.DataMaintenanceCoordinator
import com.tomi.app.data.local.StepCacheDao
import com.tomi.app.data.local.StepCacheEntity
import com.tomi.app.data.settings.SettingsRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class HealthConnectManagerTest {
    private lateinit var originalTimeZone: TimeZone
    private lateinit var dao: FakeStepCacheDao
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        dao = FakeStepCacheDao()
        repository = SettingsRepository(MutablePreferencesStore())
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `older recorder sync cannot overwrite newer step totals`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val today = LocalDate.of(2026, 11, 24)
        val olderEnd = Instant.parse("2026-11-24T12:00:00Z")
        val newerEnd = Instant.parse("2026-11-24T13:00:00Z")
        val manager = HealthConnectManager(
            context = context,
            settingsRepository = repository,
            stepCacheDao = dao,
            permissionChecker = { true },
            stepsReader = { _, end -> if (end == olderEnd) 5_000L else 9_000L },
            widgetUpdater = { },
        )

        manager.syncSteps(days = 1, now = newerEnd)
        manager.syncSteps(days = 1, now = olderEnd)

        val cached = requireNotNull(dao.observeDay(today.toString()).first())
        assertEquals(9_000L, cached.steps)
        assertEquals(newerEnd.toEpochMilli(), cached.updatedAt)
        Unit
    }

    @Test
    fun `clear waits for an in-flight sync before deleting its cache writes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = Instant.parse("2026-11-24T12:00:00Z")
        dao.blockNextWrite()
        val manager = HealthConnectManager(
            context = context,
            settingsRepository = repository,
            stepCacheDao = dao,
            permissionChecker = { true },
            stepsReader = { _, _ -> 8_000L },
            widgetUpdater = { },
        )

        val sync = async { manager.syncSteps(days = 1, now = now) }
        dao.writeStarted.await()
        val clear = async {
            DataMaintenanceCoordinator.exclusive {
                dao.deleteAll()
                repository.clearAll()
            }
        }
        yield()

        val clearedEarly = withTimeoutOrNull(100) { clear.await(); true } != null
        dao.releaseWrite.complete(Unit)
        awaitAll(sync, clear)

        assertFalse(clearedEarly)
        assertEquals(emptyList<StepCacheEntity>(), dao.observeAll().first())
        Unit
    }

    private class FakeStepCacheDao : StepCacheDao {
        private val state = MutableStateFlow<Map<String, StepCacheEntity>>(emptyMap())
        private var blockWrites = false
        val writeStarted = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()

        fun blockNextWrite() {
            blockWrites = true
        }

        override fun observeDay(date: String): Flow<StepCacheEntity?> = state.map { it[date] }

        override fun observeBetween(startDate: String, endDate: String): Flow<List<StepCacheEntity>> =
            state.map { values -> values.values.filter { it.date in startDate..endDate }.sortedBy { it.date } }

        override fun observeAll(): Flow<List<StepCacheEntity>> = state.map { it.values.sortedBy { entry -> entry.date } }

        override suspend fun upsert(entry: StepCacheEntity) {
            if (blockWrites) {
                blockWrites = false
                writeStarted.complete(Unit)
                releaseWrite.await()
            }
            state.value = state.value + (entry.date to entry)
        }

        override suspend fun deleteAll() {
            state.value = emptyMap()
        }
    }

    private class MutablePreferencesStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(mutablePreferencesOf())
        private val updates = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            updates.withLock {
                transform(state.value).also { state.value = it }
            }
    }
}
