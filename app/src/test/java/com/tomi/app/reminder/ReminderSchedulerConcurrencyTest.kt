package com.tomi.app.reminder

import android.Manifest
import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.test.core.app.ApplicationProvider
import com.tomi.app.data.settings.ReminderRuntimeState
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.data.settings.toReminderRuntimeState
import com.tomi.app.data.settings.writeTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.time.DayOfWeek
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class ReminderSchedulerConcurrencyTest {

    @Test
    fun `snooze waits for an in-flight reminder delivery`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val now = Instant.parse("2026-11-23T10:00:00Z")
        val preferences = mutablePreferencesOf().apply {
            TomiSettings(
                reminder = com.tomi.app.domain.reminder.ReminderSettings(
                    activeStartMinutes = 0,
                    activeEndMinutes = 1_439,
                    activeDays = DayOfWeek.entries.toSet(),
                ),
            ).writeTo(this)
            ReminderRuntimeState(nextReminderAt = now.toEpochMilli()).writeTo(this)
        }
        val store = BlockingRuntimeReadStore(preferences)
        val repository = SettingsRepository(store)
        val deliveryScheduler = ReminderScheduler(context, repository, ReminderNotifier(context))
        val snoozeScheduler = ReminderScheduler(context, repository, ReminderNotifier(context))

        val delivery = async { deliveryScheduler.onAlarm(now) }
        store.runtimeReadStarted.await()
        val snooze = async { snoozeScheduler.snooze(10) }
        yield()

        val wroteSnoozeEarly = withTimeoutOrNull(1_000) {
            store.updateStarted.await()
            true
        } != null
        assertFalse(wroteSnoozeEarly)
        assertNull(store.state.value.toReminderRuntimeState().snoozeUntil)

        store.releaseRuntimeRead.complete(Unit)
        awaitAll(delivery, snooze)
        Unit
    }

    private class BlockingRuntimeReadStore(initial: Preferences) : DataStore<Preferences> {
        val state = MutableStateFlow(initial)
        val runtimeReadStarted = CompletableDeferred<Unit>()
        val releaseRuntimeRead = CompletableDeferred<Unit>()
        val updateStarted = CompletableDeferred<Unit>()
        private val collections = AtomicInteger()
        private val updates = Mutex()

        override val data: Flow<Preferences> = flow {
            if (collections.getAndIncrement() == 1) {
                runtimeReadStarted.complete(Unit)
                releaseRuntimeRead.await()
            }
            emit(state.value)
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            updates.withLock {
                updateStarted.complete(Unit)
                transform(state.value).also { state.value = it }
            }
    }
}
