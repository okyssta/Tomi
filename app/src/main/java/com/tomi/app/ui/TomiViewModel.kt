package com.tomi.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.tomi.app.BuildConfig
import com.tomi.app.backup.BackupManager
import com.tomi.app.backup.BackupRestoreCoordinator
import com.tomi.app.backup.DataMaintenanceCoordinator
import com.tomi.app.backup.drive.DriveBackupScheduler
import com.tomi.app.backup.drive.GoogleDriveBackupRepository
import com.tomi.app.backup.drive.SafGoogleDriveStore
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
import com.tomi.app.health.StepSyncResult
import com.tomi.app.health.StepSyncPlanner
import com.tomi.app.reminder.MovementSessionRecorder
import com.tomi.app.reminder.ReminderNotifier
import com.tomi.app.reminder.ReminderScheduler
import com.tomi.app.update.AppVersion
import com.tomi.app.update.ApkIdentityVerifier
import com.tomi.app.update.HttpUrlConnectionUpdateHttp
import com.tomi.app.update.InstallUpdateContract
import com.tomi.app.update.UpdateDownloadScheduler
import com.tomi.app.update.UpdateInfo
import com.tomi.app.update.UpdateRepository
import com.tomi.app.update.UpdateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class DownloadQueued(val info: UpdateInfo) : UpdateUiState
    data class DownloadReady(val info: UpdateInfo) : UpdateUiState
    data class Error(val message: String, val info: UpdateInfo? = null) : UpdateUiState
}

internal fun updateDownloadUiState(info: UpdateInfo, state: WorkInfo.State): UpdateUiState = when (state) {
    WorkInfo.State.SUCCEEDED -> UpdateUiState.DownloadReady(info)
    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> UpdateUiState.Error("Update download failed. Tap Download to retry.", info)
    else -> UpdateUiState.DownloadQueued(info)
}

sealed interface StepSyncUiState {
    data object Idle : StepSyncUiState
    data object Loading : StepSyncUiState
    data object Ready : StepSyncUiState
    data object PermissionRequired : StepSyncUiState
    data class Error(val message: String) : StepSyncUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TomiViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TomiDatabase.get(application)
    private val settingsRepository = SettingsRepository.from(application)
    private val healthManager = HealthConnectManager(application, settingsRepository, database.stepCacheDao())
    private val recorder = MovementSessionRecorder(application, settingsRepository, database)
    private val backupManager = BackupManager(application, database, settingsRepository)
    private val googleDriveBackup = GoogleDriveBackupRepository(
        settingsRepository = settingsRepository,
        backup = backupManager,
        store = SafGoogleDriveStore(application),
    )
    private val currentDay = CurrentDay()
    val dayContext = currentDay.context

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
    val stepCache = currentDay.context.flatMapLatest { context ->
        database.stepCacheDao().observeBetween(context.date.minusDays(29).toString(), context.date.toString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val checkIn = currentDay.context.flatMapLatest { context ->
        database.checkInDao().observeDate(context.date.toString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val waterMl = currentDay.context.flatMapLatest { context ->
        val bounds = StepSyncPlanner.dayBounds(context.date, context.zone)
        database.waterDao().observeTotalMl(bounds.start.toEpochMilli(), bounds.endExclusive.toEpochMilli())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val todaySteps = combine(currentDay.context, stepCache) { context, entries ->
        entries.firstOrNull { it.date == context.date.toString() }?.steps
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val todayMovements = combine(currentDay.context, movements) { context, entries ->
        entries.filter {
            Instant.ofEpochMilli(it.startedAt).atZone(context.zone).toLocalDate() == context.date
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _healthStatus = MutableStateFlow(HealthStatus.UNKNOWN)
    val healthStatus: StateFlow<HealthStatus> = _healthStatus

    private val _backgroundHealthGranted = MutableStateFlow(false)
    val backgroundHealthGranted: StateFlow<Boolean> = _backgroundHealthGranted

    private val _stepSyncState = MutableStateFlow<StepSyncUiState>(StepSyncUiState.Idle)
    val stepSyncState: StateFlow<StepSyncUiState> = _stepSyncState

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState
    private var updateDownloadJob: Job? = null

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private val _dataCleared = MutableSharedFlow<Unit>()
    val dataCleared = _dataCleared.asSharedFlow()

    private var lastWaterId: Long? = null
    private val _canUndoWater = MutableStateFlow(false)
    val canUndoWater: StateFlow<Boolean> = _canUndoWater

    init {
        viewModelScope.launch {
            while (true) {
                currentDay.refresh()
                delay(60_000L)
            }
        }
        viewModelScope.launch {
            try {
                settingsRepository.settings.first()
                _ready.value = true
                refreshHealth()
                if (BuildConfig.UPDATES_ENABLED) {
                    com.tomi.app.update.UpdateCheckScheduler.checkIfDue(getApplication())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _ready.value = true
                _healthStatus.value = HealthStatus.UNKNOWN
                emit("Startup check failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun refreshHealth() {
        viewModelScope.launch {
            DataMaintenanceCoordinator.exclusive {
                currentDay.refresh()
                _healthStatus.value = healthManager.status()
                _backgroundHealthGranted.value = healthManager.hasBackgroundReadPermission()
                if (_healthStatus.value == HealthStatus.AVAILABLE_GRANTED) {
                    _stepSyncState.value = StepSyncUiState.Loading
                    _stepSyncState.value = when (val result = healthManager.syncStepsExclusive()) {
                        is StepSyncResult.Success -> StepSyncUiState.Ready
                        StepSyncResult.PermissionRequired -> StepSyncUiState.PermissionRequired
                        is StepSyncResult.Error -> StepSyncUiState.Error(result.cause.message ?: "Could not refresh steps")
                    }
                } else {
                    _stepSyncState.value = StepSyncUiState.PermissionRequired
                }
            }
        }
    }

    fun saveTask(task: TaskEntity, onResult: (Boolean) -> Unit) {
        val title = task.title.trim()
        if (title.isEmpty()) {
            emit("Task title cannot be empty")
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                if (task.id == 0L) {
                    database.taskDao().insert(task.copy(title = title, createdAt = now, updatedAt = now))
                } else {
                    database.taskDao().update(task.copy(title = title, updatedAt = now))
                }
                onResult(true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                onResult(false)
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

    fun saveJournal(entry: JournalEntity, onResult: (Boolean) -> Unit) {
        val title = entry.title.trim()
        val body = entry.body.trim()
        if (title.isEmpty() && body.isEmpty()) {
            emit("Write something before saving")
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
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
                onResult(true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                onResult(false)
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
            currentDay.refresh()
            database.checkInDao().upsert(
                DailyCheckInEntity(
                    date = currentDay.context.value.date.toString(),
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
            _canUndoWater.value = true
        }
    }

    fun undoWater() {
        val id = lastWaterId ?: return
        _canUndoWater.value = false
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
            com.tomi.app.reminder.ReminderWorker.enqueue(getApplication())
            if (BuildConfig.UPDATES_ENABLED) {
                com.tomi.app.update.UpdateCheckScheduler.scheduleDaily(getApplication())
            }
        }
    }

    fun checkForUpdates() {
        if (!BuildConfig.UPDATES_ENABLED) return
        viewModelScope.launch {
            try {
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
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _updateState.value = UpdateUiState.Error(error.message ?: "Update check failed")
            }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        if (!BuildConfig.UPDATES_ENABLED) return
        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch {
            try {
                val workId = UpdateDownloadScheduler.enqueue(getApplication(), info, unmeteredOnly = false)
                _updateState.value = UpdateUiState.DownloadQueued(info)
                observeUpdateDownload(workId, info)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _updateState.value = UpdateUiState.Error(error.message ?: "Update download failed", info)
            }
        }
    }

    fun resumeUpdateDownloadState() {
        if (!BuildConfig.UPDATES_ENABLED) return
        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch {
            try {
                val snapshot = UpdateDownloadScheduler.latestDownload(getApplication()) ?: return@launch
                observeUpdateDownload(snapshot.pending.workId, snapshot.pending.info)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _updateState.value = UpdateUiState.Error(error.message ?: "Update state could not be restored")
            }
        }
    }

    private suspend fun observeUpdateDownload(workId: String, info: UpdateInfo) {
        val id = runCatching { UUID.fromString(workId) }.getOrNull() ?: return
        WorkManager.getInstance(getApplication<Application>())
            .getWorkInfoByIdFlow(id)
            .collect { work ->
                val current = work ?: return@collect
                _updateState.value = updateDownloadUiState(info, current.state)
            }
    }

    fun installUpdate(info: UpdateInfo) {
        if (!BuildConfig.UPDATES_ENABLED) return
        val context = getApplication<Application>()
        val apk = File(context.cacheDir, "updates/tomi-${info.versionName}.apk")
        if (!ApkIdentityVerifier.verify(context, apk, BuildConfig.APPLICATION_ID, info.versionName)) {
            _updateState.value = UpdateUiState.Error("Downloaded update is missing or failed verification", info)
            return
        }
        val intent = Intent().apply {
            setClassName(context, InstallUpdateContract.ACTIVITY_CLASS)
            putExtra(InstallUpdateContract.EXTRA_APK_PATH, apk.absolutePath)
            putExtra(InstallUpdateContract.EXTRA_VERSION, info.versionName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (error: Exception) {
            _updateState.value = UpdateUiState.Error(
                "Could not open the installer: ${error.message ?: "unknown error"}",
                info,
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val driveUri = settingsRepository.settings.first().googleDriveTreeUri
                DataMaintenanceCoordinator.exclusive {
                    val context = getApplication<Application>()
                    DataMaintenanceCoordinator.clear(
                        deleteUpdateCache = {
                            val cache = File(context.cacheDir, "updates")
                            cache.deleteRecursively() || !cache.exists()
                        },
                        clearDatabase = { database.clearAllTables() },
                        clearSettings = {
                            settingsRepository.clearAll()
                        },
                        afterPersistentDataCleared = {
                            val notifier = ReminderNotifier(context)
                            ReminderScheduler(context, settingsRepository, notifier).cancel()
                            UpdateDownloadScheduler.cancelAndClear(context)
                            androidx.core.app.NotificationManagerCompat.from(context).cancelAll()
                            updateDownloadJob?.cancel()
                            updateDownloadJob = null
                            runCatching { googleDriveBackup.release(driveUri) }
                            DriveBackupScheduler.cancel(context)
                        },
                    )
                    currentDay.refresh()
                    lastWaterId = null
                    _canUndoWater.value = false
                    _updateState.value = UpdateUiState.Idle
                    _dataCleared.emit(Unit)
                }
                emit("All Tomi data cleared")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Clear failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun connectGoogleDrive(treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var folderName = "Google Drive"
                var backupError: Exception? = null
                DataMaintenanceCoordinator.exclusive {
                    folderName = googleDriveBackup.connect(treeUri)
                    try {
                        googleDriveBackup.backupNow()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        backupError = error
                    }
                }
                DriveBackupScheduler.schedule(getApplication())
                emit(
                    if (backupError == null) {
                        "Google Drive connected: $folderName"
                    } else {
                        "Connected to $folderName, but the first backup failed: ${backupError?.message ?: "unknown error"}"
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Google Drive connection failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DataMaintenanceCoordinator.exclusive { googleDriveBackup.disconnect() }
                DriveBackupScheduler.cancel(getApplication())
                emit("Google Drive disconnected")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Could not disconnect Google Drive: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun backupToGoogleDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DataMaintenanceCoordinator.exclusive { googleDriveBackup.backupNow() }
                emit("Backed up to Google Drive")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Google Drive backup failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun restoreFromGoogleDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DataMaintenanceCoordinator.exclusive {
                    val input = googleDriveBackup.openBackupInput()
                        ?: error("No Tomi backup was found in Google Drive")
                    BackupRestoreCoordinator.completeImport(
                        commit = { input.use { backupManager.import(it) } },
                        maintenance = {
                            val importedSettings = settingsRepository.settings.first()
                            ReminderScheduler(
                                getApplication(),
                                settingsRepository,
                                ReminderNotifier(getApplication()),
                            ).reschedule()
                            if (importedSettings.reminder.enabled) {
                                com.tomi.app.reminder.ReminderWorker.enqueue(getApplication())
                            }
                        },
                    )
                }
                refreshHealth()
                emit("Google Drive backup restored")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Google Drive restore failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun setGoogleDriveAutoSync(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.updateSettings { it.copy(googleDriveAutoSync = enabled) }
                if (enabled) {
                    DriveBackupScheduler.schedule(getApplication())
                } else {
                    DriveBackupScheduler.cancel(getApplication())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Could not change Google Drive sync: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            try {
                DataMaintenanceCoordinator.exclusive {
                    val content = backupManager.export()
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(content.toByteArray())
                    } ?: error("Could not open backup file")
                }
                emit("Backup exported")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Export failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            try {
                DataMaintenanceCoordinator.exclusive {
                    BackupRestoreCoordinator.completeImport(
                        commit = {
                            val input = getApplication<Application>().contentResolver.openInputStream(uri)
                                ?: error("Could not open backup file")
                            input.use { backupManager.import(it) }
                        },
                        maintenance = {
                            val importedSettings = settingsRepository.settings.first()
                            ReminderScheduler(
                                getApplication(),
                                settingsRepository,
                                ReminderNotifier(getApplication()),
                            ).reschedule()
                            if (importedSettings.reminder.enabled) {
                                com.tomi.app.reminder.ReminderWorker.enqueue(getApplication())
                            }
                        },
                    )
                }
                refreshHealth()
                emit("Backup imported")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit("Import failed: ${error.message ?: "invalid backup"}")
            }
        }
    }

    private fun emit(message: String) {
        viewModelScope.launch { _events.emit(message) }
    }
}
