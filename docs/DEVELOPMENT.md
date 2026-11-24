# Development notes

## Toolchain

- JDK 17
- Android Gradle Plugin 8.9.1
- Gradle 8.11.1
- Kotlin 2.1.20
- compile/target SDK 36, minimum SDK 26

The dependency stack is pinned for reproducible builds. Dependabot proposes minor and patch updates; major toolchain changes should be reviewed separately.

## Tests

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Room tests use `BundledSQLiteDriver`. On JVM test hosts, Gradle swaps the Android SQLite artifact for `sqlite-bundled-jvm`; this is necessary because Android's Bionic SQLite library cannot load in a desktop JVM.

Compose UI tests use Robolectric. DAO tests disable the custom `TomiApp` application so WorkManager is not initialized by the test harness.

## ARM64 Linux hosts

Google's Linux AAPT2 binary is x86_64. On an ARM64 host without a native AAPT2 package, a local QEMU wrapper can be configured in the user-level Gradle properties:

```properties
android.aapt2FromMavenOverride=/path/to/qemu-aapt2-wrapper
```

This setting is intentionally not committed. GitHub's x86_64 runners use the normal Maven AAPT2 binary.

## Manual verification checklist

- Complete onboarding and grant notifications.
- Set a short test reminder and use **Settings → Test reminder**.
- Connect Health Connect and confirm today's steps populate.
- Log a walk, a push-up set, and a stretch.
- Add, edit, complete, search, and delete a task.
- Create, pin, search, edit, and delete a journal entry.
- Save a mood/energy check-in and add water.
- Export a backup, clear data, and import the backup.
- Add the home-screen widget.
- Check for updates; with no newer release, Tomi should report up to date.

No physical-device or emulator claim should be made unless these steps were actually run on one.
