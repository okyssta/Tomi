package com.tomi.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomi.app.BuildConfig
import com.tomi.app.backup.BackupManager
import com.tomi.app.data.local.DailyCheckInEntity
import com.tomi.app.data.local.JournalEntity
import com.tomi.app.data.local.MovementEntity
import com.tomi.app.data.local.MovementType
import com.tomi.app.data.local.TaskEntity
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.local.WaterEntity
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.health.HealthConnectManager
import com.tomi.app.health.HealthStatus
import com.tomi.app.health.StepSyncPlanner
import com.tomi.app.reminder.MovementSessionRecorder
import com.tomi.app.reminder.ReminderNotifier
import com.tomi.app.reminder.ReminderScheduler
import com.tomi.app.update.AppVersion
import com.tomi.app.update.HttpUrlConnectionUpdateHttp
import com.tomi.app.update.UpdateDownloadScheduler
import com.tomi.app.update.UpdateInfo
import com.tomi.app.update.UpdateRepository
import com.tomi.app.update.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class DownloadQueued(val info: UpdateInfo) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class TomiViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TomiDatabase.get(application)
    private val settingsRepository = SettingsRepository.from(application)
    private val healthManager = HealthConnectManager(application, settingsRepository, database.stepCacheDao())
    private val recorder = MovementSessionRecorder(application, settingsRepository, database)
    private val backupManager = BackupManager(application, database, settingsRepository)
    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()
    private val todayBounds = StepSyncPlanner.dayBounds(today, zone)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TomiSettings(),
    )
    val runtime = settingsRepository.runtime.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        com.tomi.app.data.settings.ReminderRuntimeState(),
    )
    val tasks = database.taskDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val journal = database.journalDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val movements = database.movementDao().observeRecent(365).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val stepCache = database.stepCacheDao()
        .observeBetween(today.minusDays(29).toString(), today.toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val checkIn = database.checkInDao().observeDate(today.toString()).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    val waterMl = database.waterDao()
        .observeTotalMl(todayBounds.start.toEpochMilli(), todayBounds.endExclusive.toEpochMilli())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val todaySteps = stepCache.map { entries ->
        entries.firstOrNull { it.date == today.toString() }?.steps
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val todayMovements = movements.map { entries ->
        entries.filter {
            Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == today
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _healthStatus = MutableStateFlow(HealthStatus.UNKNOWN)
    val healthStatus: StateFlow<HealthStatus> = _healthStatus

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private var lastWaterId: Long? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.first()
            _ready.value = true
            refreshHealth()
            com.tomi.app.update.UpdateCheckScheduler.checkIfDue(getApplication())
        }
    }

    fun refreshHealth() {
        viewModelScope.launch {
            _healthStatus.value = healthManager.status()
            if (_healthStatus.value == HealthStatus.AVAILABLE_GRANTED) {
                healthManager.syncSteps()
            }
        }
    }

    fun saveTask(task: TaskEntity) {
        val title = task.title.trim()
        if (title.isEmpty()) {
            emit("Task title cannot be empty")
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (task.id == 0L) {
                database.taskDao().insert(task.copy(title = title, createdAt = now, updatedAt = now))
            } else {
                database.taskDao().update(task.copy(title = title, updatedAt = now))
            }
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            database.taskDao().setCompleted(
                id = task.id,
                completed = !task.completed,
                completedAt = if (task.completed) null else now,
                updatedAt = now,
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { database.taskDao().delete(task) }
    }

    fun saveJournal(entry: JournalEntity) {
        val title = entry.title.trim()
        val body = entry.body.trim()
        if (title.isEmpty() && body.isEmpty()) {
            emit("Write something before saving")
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val saved = entry.copy(
                title = title.ifEmpty { "Untitled entry" },
                body = body,
                updatedAt = now,
            )
            if (entry.id == 0L) {
                database.journalDao().insert(saved.copy(createdAt = now))
            } else {
                database.journalDao().update(saved)
            }
        }
    }

    fun toggleJournalPin(entry: JournalEntity) {
        viewModelScope.launch {
            database.journalDao().setPinned(entry.id, !entry.pinned, System.currentTimeMillis())
        }
    }

    fun deleteJournal(entry: JournalEntity) {
        viewModelScope.launch { database.journalDao().delete(entry) }
    }

    fun saveCheckIn(mood: Int, energy: Int, note: String) {
        require(mood in 1..5 && energy in 1..5)
        viewModelScope.launch {
            database.checkInDao().upsert(
                DailyCheckInEntity(
                    date = today.toString(),
                    mood = mood,
                    energy = energy,
                    note = note.trim(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            emit("Check-in saved")
        }
    }

    fun addWater(amountMl: Int = 250) {
        viewModelScope.launch {
            lastWaterId = database.waterDao().insert(
                WaterEntity(amountMl = amountMl, timestamp = System.currentTimeMillis()),
            )
        }
    }

    fun undoWater() {
        val id = lastWaterId ?: return
        viewModelScope.launch {
            database.waterDao().deleteById(id)
            lastWaterId = null
        }
    }

    fun logMovement(type: MovementType, startedAt: Long, durationSeconds: Int, reps: Int? = null) {
        viewModelScope.launch {
            recorder.log(type, startedAt, durationSeconds, reps)
            emit("Movement logged")
        }
    }

    fun deleteMovement(session: MovementEntity) {
        viewModelScope.launch { database.movementDao().deleteById(session.id) }
    }

    fun updateSettings(transform: (TomiSettings) -> TomiSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSettings(transform)
            } catch (_: IllegalArgumentException) {
                emit("Choose a valid reminder window")
                return@launch
            }
            ReminderScheduler(
                getApplication(),
                settingsRepository,
                ReminderNotifier(getApplication()),
            ).reschedule()
        }
    }

    fun snoozeReminder() {
        viewModelScope.launch {
            ReminderScheduler(
                getApplication(),
                settingsRepository,
                ReminderNotifier(getApplication()),
            ).snooze(10)
            emit("Reminder snoozed for 10 minutes")
        }
    }

    fun sendTestReminder() {
        ReminderNotifier(getApplication()).showTestReminder()
    }

    fun completeOnboarding(intervalMinutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    reminder = it.reminder.copy(intervalMinutes = intervalMinutes),
                    onboardingComplete = true,
                )
            }
            ReminderScheduler(
                getApplication(),
                settingsRepository,
                ReminderNotifier(getApplication()),
            ).reschedule()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            val repository = UpdateRepository(
                http = HttpUrlConnectionUpdateHttp(),
                currentVersion = AppVersion.parse(BuildConfig.VERSION_NAME.substringBefore('-')),
                repository = BuildConfig.GITHUB_REPOSITORY,
            )
            _updateState.value = when (val result = repository.check()) {
                is UpdateResult.Available -> UpdateUiState.Available(result.info)
                UpdateResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateResult.Error -> UpdateUiState.Error(result.cause.message ?: "Update check failed")
            }
            settingsRepository.setLastUpdateCheckAt(System.currentTimeMillis())
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        UpdateDownloadScheduler.enqueue(getApplication(), info, unmeteredOnly = false)
        _updateState.value = UpdateUiState.DownloadQueued(info)
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            settingsRepository.clearAll()
            _updateState.value = UpdateUiState.Idle
            emit("All Tomi data cleared")
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            try {
                val content = backupManager.export()
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(content.toByteArray())
                } ?: error("Could not open backup file")
                emit("Backup exported")
            } catch (error: Exception) {
                emit("Export failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: error("Could not open backup file")
                backupManager.import(content)
                refreshHealth()
                emit("Backup imported")
            } catch (error: Exception) {
                emit("Import failed: ${error.message ?: "invalid backup"}")
            }
        }
    }

    private fun emit(message: String) {
        viewModelScope.launch { _events.emit(message) }
    }
}
