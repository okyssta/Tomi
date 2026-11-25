# Architecture

Tomi is a single-module Android app built with Kotlin and Jetpack Compose.

## Layers

- **UI:** Jetpack Compose and Material 3. `MainActivity` hosts one `NavHost`; top-level destinations use the standard `NavigationBar`. Screens collect `StateFlow` values from `TomiViewModel`.
- **State:** `TomiViewModel` exposes Room flows and DataStore settings, and coordinates user actions. No UI composable accesses a DAO directly.
- **Persistence:** Room stores tasks, journal entries, movement sessions, daily check-ins, water entries, and cached step totals. Production uses Android's framework SQLite through Room's `SupportSQLiteOpenHelper`, including transactional backup replacement. DataStore stores settings and reminder runtime timestamps. DAO tests use the bundled SQLite driver for ARM64 JVM compatibility; restore-order logic is tested as a pure coroutine coordinator.
- **Health:** `HealthConnectManager` checks provider availability and permissions, reads aggregated `StepsRecord.COUNT_TOTAL` without a source filter, and writes daily totals to the local step cache. Aggregation avoids double-counting raw records from multiple sources.
- **Reminders:** `ReminderPolicy` is pure Kotlin and computes the next occurrence. `ReminderScheduler` stores that occurrence and registers an `AlarmManager` alarm. `ReminderWorker` provides an hourly backup check. A boot receiver restores scheduling after reboot, app replacement, time changes, and timezone changes.
- **Smart pause:** Screen-on/user-present broadcasts and the hourly worker query steps since the last check. About 50 new steps reset the inactivity baseline.
- **Movement:** `MovementTimer` excludes paused time. `MovementSessionRecorder` stores a session, resets the reminder baseline, reschedules, and refreshes today's cached steps.
- **Updates:** `UpdateRepository` checks GitHub's latest release. `UpdateDownloadWorker` downloads only HTTPS GitHub assets, verifies the published SHA-256 checksum, and posts an install notification. Android's package installer verifies the APK signature before updating.
- **Widget:** Glance reads the same DataStore and Room sources. Scheduler and step-sync changes refresh all widget instances.

## Data flow

```text
Health Connect -> HealthConnectManager -> StepCacheDao -> Home / Progress / Widget
User input -> TomiViewModel -> Room / DataStore -> Flow -> Compose
DataStore reminder settings -> ReminderPolicy -> AlarmManager -> ReminderReceiver -> Notification
GitHub release -> UpdateRepository -> UpdateDownloadWorker -> SHA-256 -> Package installer
```

## Failure behavior

- Missing Health Connect: the app remains usable; steps show a connect prompt.
- Denied health permission: cached totals remain visible and no new reads occur.
- Missing notification permission: reminders are scheduled silently; Settings shows the missing permission.
- Missing exact-alarm permission: inexact alarms are used.
- Network failure: update checking reports an error and does not alter installed data.
- Checksum mismatch: the downloaded APK is deleted and no install notification is posted.
- Invalid backup: import stops before replacing data when codec validation fails.
