# Changelog

All notable changes to Tomi are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and releases use semantic versioning.

## [Unreleased]

### Added

- Offline-first Material 3 Android app.
- Health Connect step aggregation and Google Fit-compatible data sharing.
- Configurable sedentary reminders with smart pause and notification actions.
- Walk, stretch, stand-up, and push-up movement logging.
- Tasks, journal, daily mood/energy check-ins, water tracking, and progress charts.
- Home-screen widget.
- JSON backup export/import and local data reset.
- GitHub release checker, SHA-256-verified downloads, and install prompt.
- CI, signed release workflow, CodeQL, Dependabot, issue forms, and repository documentation.

### Changed

- Added direct and Play distribution flavors; the Play AAB excludes the APK self-updater and installer-only permissions.
- Release automation now publishes a signed direct APK, signed Play AAB, both mapping files, and signature/checksum evidence.
- Added a public privacy policy, in-app policy link, prominent Health Connect disclosure, and bundled open-source notices.
- Hardened direct updates with exact repository URL validation, expected package/version checks, and pinned signing-certificate verification.

### Fixed

- Alarm channels now honor sound/vibration settings on Android O+.
- Reminder recovery handles boot, package replacement, clock/timezone changes, overdue delivery, and snooze boundaries.
- Health Connect reads clamp today's range to the current time; smart pause preserves checkpoints on permission/provider failures and races.
- Date-scoped UI and widget data now roll over at local midnight; external navigation routes are allowlisted.
- Backups are bounded, validated, consistently exported, streamed on import, and protected by destructive-action confirmation.
- Task due dates use UTC picker semantics and can be removed; task and journal saves retain drafts on failure.
- Clear-all now cancels alarms/work, removes notifications and update cache, and resets local runtime state.
- Added accessible page indicators, chart summaries, mood labels, named setting toggles, slider semantics, and large-font layout fixes.
- Added a configurable reminder start and end window; reminders run only within that window and on selected days.
- Corrected clock-change broadcasts, ten-minute snooze timing, repeated alarm delivery, legacy notification-channel migration, and smart-pause synchronization.
- Added task/journal editor delete confirmation, local step-cache disclosure, exact release metadata/signer checks, and bounded backup export/import symmetry.
- Restored imported reminder scheduling, terminal update-download state after process restarts, and row-specific accessibility labels.
- Preserved task/journal editor identity and drafts, backup-import confirmation, reminder time-picker state, and destructive confirmations across recreation.
- Weekday chips now announce full localized names, and the privacy-policy row has a single click target.
- Home quick actions wrap as stacked equal-width buttons, and misleading progress stop dots were removed.
- Release verification rejects zero or regressed version codes, authenticates the Bundletool jar before execution, checks both APK and AAB signer digests, and verifies the published privacy-policy content hash.
- Backup export/import/clear now share one maintenance lock; rollback failures, incomplete backups, future-dated step entries, cancellation, and clear/update races are handled explicitly.
- Updater metadata rolls back work enqueue failures, update clears are serialized, and process-death editor restoration no longer strands Save.
- Added an independent-review response at `docs/AUDIT-RESPONSE.md`.
