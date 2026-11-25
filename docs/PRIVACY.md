# Privacy model

Effective: 25 November 2026. Developer: [@okyssta](https://github.com/okyssta). Privacy/security contact: [private vulnerability reporting](https://github.com/okyssta/Tomi/security/advisories/new). The public policy is published at <https://okyssta.github.io/Tomi/privacy.html> and is available inside Tomi under Settings → Privacy policy.

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

The direct-download edition contacts:

- `https://api.github.com/repos/okyssta/Tomi/releases/latest`
- `https://github.com/okyssta/Tomi/releases/download/...`
- Google Play only when the user chooses to install or update Health Connect

Release downloads must use exact HTTPS GitHub repository URLs. The app verifies the published SHA-256 checksum, expected package/version, and pinned signing certificate before offering installation. The Android package installer separately verifies the APK signature.

The Google Play edition does not include the APK self-updater, installer activity, unknown-app permission, foreground data-sync permission, or Tomi internet permission. Play handles updates. Opening the privacy policy or Health Connect listing uses the browser or Play Store outside Tomi.

There is no analytics SDK, advertising SDK, crash-upload service, account system, or server-side personal-data storage.

## Permissions

- **Notifications:** movement and update notifications.
- **Exact alarms:** optional; improves reminder timing.
- **Boot completed:** restores reminder scheduling after restart.
- **Foreground data sync:** direct edition only; shows update-download progress.
- **Install unknown apps:** direct edition only; required solely for in-app APK installation.
- **Internet:** direct edition only; release checks and downloads.
- **Vibration:** optional reminder feedback.

Users can revoke permissions at any time in Android Settings.

## Retention and deletion

Local data is retained until the user deletes an item, imports a replacement backup, clears all Tomi data, or uninstalls the app. Clear all data cancels reminders and work, dismisses notifications, deletes downloaded update files, and clears Room and DataStore. Exported files and Health Connect source records are outside that local deletion scope.

## Security

Tomi uses app-private storage, parameterized Room queries, bounded backup import, read-only Health Connect access, HTTPS release downloads, checksum verification, and signing-identity verification. No security system is perfect; report suspected vulnerabilities through the private channel above.
