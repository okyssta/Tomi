# Tomi

Tomi is an offline-first Android companion for people who spend long workdays at a desk. It combines gentle movement reminders, Health Connect step tracking, guided movement breaks, tasks, journaling, mood check-ins, water tracking, and local backups in one Material 3 app.

[![Release](https://img.shields.io/github/v/release/okyssta/Tomi)](https://github.com/okyssta/Tomi/releases/latest)
[![CI](https://github.com/okyssta/Tomi/actions/workflows/ci.yml/badge.svg)](https://github.com/okyssta/Tomi/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/okyssta/Tomi)](LICENSE)

## Features

- **Sedentary reminders:** configurable 30–45 minute intervals, active days, active hours, sound, vibration, snooze, and smart pause after detected movement.
- **Step tracking:** aggregated daily and weekly steps through Google Health Connect. Includes phone steps and connected sources such as Google Fit.
- **Movement breaks:** walk and stretch timers, a stand-up reset, and a push-up rep counter. Completed sessions are stored locally.
- **Tasks:** create, edit, search, filter, prioritize, categorize, schedule, complete, and delete tasks.
- **Journal:** searchable entries with mood, tags, pinning, and editing.
- **Daily check-in:** mood and energy tracking.
- **Water:** 250 ml quick-add with undo and a configurable daily goal.
- **Progress:** weekly step chart, active minutes, movement history, and a movement-day streak.
- **Widget:** steps, next reminder, and quick break actions on the home screen.
- **Backups:** JSON export/import for tasks, journal, movement, check-ins, water, cached steps, and settings.
- **Updates:** automatic release checks, optional Wi-Fi downloads, SHA-256 verification, and an explicit Android install prompt.
- **Material 3:** standard Android components, dynamic color, light/dark/system themes, and no custom decorative UI framework.

## Install

Download `tomi-release.apk` from the [latest release](https://github.com/okyssta/Tomi/releases/latest).

- Android 8.0 (API 26) or newer is required.
- Allow notifications for movement reminders.
- Allow exact alarms if you want reminders to fire at the configured minute. Without this optional permission, Android may delay them.
- Install Health Connect from Google Play on Android 8–13. It is built into Android 14+.
- In Tomi, open **Settings → Permissions & steps → Health Connect** and allow step access.
- To include Google Fit data, connect Google Fit to Health Connect. Tomi reads the combined Health Connect total; it does not require a Google account or OAuth client.

The release APK is signed. Android will only install an update signed with the same key. Tomi never installs an update without showing the system package-installer confirmation.

## Google Fit status

Google deprecated the Google Fit APIs in 2024 and plans to turn them down at the end of 2026. New apps can no longer register for the Fit Android API. Google recommends **Health Connect** for mobile step tracking. Tomi therefore uses the supported Health Connect integration rather than building on an API scheduled for removal.

## Privacy

Tomi has no account, ads, analytics, location tracking, or cloud sync. Tasks, journal entries, settings, movement sessions, and cached step totals are stored in the app's local database. Health Connect remains read-only. The only network requests are:

1. GitHub release metadata and release-asset downloads for the in-app updater.
2. Google Play links when Health Connect must be installed or updated.

See [docs/PRIVACY.md](docs/PRIVACY.md) for the full data and permission model.

## Development

Requirements: JDK 17, Android SDK 36, and the Gradle wrapper included in the repository.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

For a signed local release, create an untracked `keystore.properties` file as described in [docs/RELEASE.md](docs/RELEASE.md), then run:

```sh
./gradlew testDebugUnitTest lintRelease assembleRelease
```

## Repository layout

```text
app/src/main/java/com/tomi/app/
  backup/       JSON backup models, codec, and import/export
  data/         Room entities, DAOs, database, and DataStore settings
  domain/       Reminder scheduling and movement timer/streak logic
  health/       Health Connect availability, permissions, and step sync
  reminder/     Alarm scheduler, notifications, receivers, workers
  ui/           Material 3 theme, navigation, ViewModel, screens
  update/       Release checking, verified downloads, install prompt
  widget/       Glance home-screen widget
app/src/test/   Unit, Room, update, and Compose UI tests
.github/        CI, signed releases, CodeQL, Dependabot, templates
docs/           Privacy, architecture, development, release, verification
scripts/        Published-release verification
```

## Verification

The project runs unit tests, Room integration tests on bundled SQLite, a Compose onboarding UI test, Android lint, and debug/release APK builds. See [docs/VERIFICATION.md](docs/VERIFICATION.md) and the [CI workflow](.github/workflows/ci.yml).

## Known limits

- Reminder timing depends on Android battery optimization and the optional exact-alarm permission.
- Health Connect access requires Android 14+ or the separate Health Connect app.
- Google Fit cloud history that has not been written to Health Connect is not available to Tomi.
- Smart pause needs Health Connect read access; without it, reminders continue on the fixed interval.
- Updates require a network connection and the user's approval to install from unknown apps.

## License

MIT. See [LICENSE](LICENSE).
