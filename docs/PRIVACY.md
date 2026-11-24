# Privacy model

## Data stored locally

- Tasks and notes
- Journal entries, tags, mood, and pin state
- Movement session type, time, duration, and reps
- Daily mood/energy check-ins
- Water entries
- Step totals cached from Health Connect
- Reminder settings, goals, theme, and update preferences

Tomi stores these in its app-private Room database and DataStore files. Android cloud backup and device-transfer extraction are disabled. The app provides explicit JSON export and import instead.

## Health Connect

Tomi requests:

- `android.permission.health.READ_STEPS`
- optional `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`

Tomi does not request write permissions and does not write, modify, or delete Health Connect records. It reads aggregated totals, not raw location or workout routes. Android 14+ can provide on-device phone steps through Health Connect. Other sources, including Google Fit when connected to Health Connect, can contribute to the total.

Background access is optional. Declining it does not disable reminders; it only limits background step refreshes and smart pause while the app is closed.

## Network

The app contacts:

- `https://api.github.com/repos/okyssta/Tomi/releases/latest`
- `https://github.com/okyssta/Tomi/releases/download/...`
- Google Play only when the user chooses to install or update Health Connect

Release downloads must use HTTPS GitHub URLs. The app verifies the published SHA-256 checksum before offering installation. The Android package installer separately verifies the APK signature.

There is no analytics SDK, advertising SDK, crash-upload service, account system, or server-side personal-data storage.

## Permissions

- **Notifications:** movement and update notifications.
- **Exact alarms:** optional; improves reminder timing.
- **Boot completed:** restores reminder scheduling after restart.
- **Foreground data sync:** shows progress while downloading an update.
- **Install unknown apps:** optional; required only for in-app APK installation.
- **Internet:** release checks and downloads only.
- **Vibration:** optional reminder feedback.

Users can revoke permissions at any time in Android Settings or clear all Tomi data from Settings → Data & privacy.
