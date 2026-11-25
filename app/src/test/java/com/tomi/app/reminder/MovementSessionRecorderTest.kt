package com.tomi.app.reminder

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.tomi.app.data.local.MovementType
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.health.StepSyncResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class MovementSessionRecorderTest {
    private lateinit var database: TomiDatabase
    private lateinit var repository: SettingsRepository
    private lateinit var store: MutablePreferencesStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TomiDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        store = MutablePreferencesStore()
        repository = SettingsRepository(store)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `recording older movement cannot regress a newer movement checkpoint`() = runBlocking {
        repository.updateRuntime { ReminderRuntimeState(lastMovementAt = 5_000L, lastStepCheckAt = 5_000L) }

        MovementSessionRecorder(
            ApplicationProvider.getApplicationContext(),
            repository,
            database,
            syncSteps = { _, _ -> StepSyncResult.Success(0, 0L) },
        ).log(MovementType.WALK, startedAt = 1_000L, durationSeconds = 1)

        val runtime = repository.runtime.first()
        assertEquals(5_000L, runtime.lastMovementAt)
        assertEquals(5_000L, runtime.lastStepCheckAt)
        Unit
    }

    @Test
    fun `movement recording holds the shared checkpoint lock`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = MovementSessionRecorder(
            context,
            repository,
            database,
            syncSteps = { _, _ -> StepSyncResult.Success(0, 0L) },
        )
        store.blockNextUpdate()

        val recording = async {
            recorder.log(MovementType.WALK, startedAt = 1_000L, durationSeconds = 1)
        }
        store.updateStarted.await()
        val lockAcquired = CompletableDeferred<Unit>()
        val concurrentSync = async {
            MovementCheckpointLock.mutex.withLock {
                lockAcquired.complete(Unit)
                repository.updateRuntime { ReminderRuntimeState(lastMovementAt = 5_000L, lastStepCheckAt = 5_000L) }
            }
        }
        yield()

        val completedEarly = withTimeoutOrNull(1_000) {
            lockAcquired.await()
            true
        } != null
        store.releaseUpdate.complete(Unit)
        awaitAll(recording, concurrentSync)

        assertFalse(completedEarly)
        assertEquals(5_000L, repository.runtime.first().lastStepCheckAt)
        Unit
    }

    private class MutablePreferencesStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(mutablePreferencesOf())
        private val updates = Mutex()
        private var blockUpdates = false
        val updateStarted = CompletableDeferred<Unit>()
        val releaseUpdate = CompletableDeferred<Unit>()

        override val data: Flow<Preferences> = flow { emit(state.value) }

        fun blockNextUpdate() {
            blockUpdates = true
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            updates.withLock {
                if (blockUpdates) {
                    blockUpdates = false
                    updateStarted.complete(Unit)
                    releaseUpdate.await()
                }
                transform(state.value).also { state.value = it }
            }
    }
}
