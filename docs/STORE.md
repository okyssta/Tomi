# Google Play submission checklist

Tomi's build pipeline produces a signed Play-flavor AAB. This file separates artifacts that exist from Console and device work that still requires a human account or physical hardware.

## Build identity

- Package: `com.tomi.app`
- Version name for the next release: set by the stable Git tag
- Version code: `major * 1_000_000 + minor * 1_000 + patch`; components 0–999; must exceed every previous stable tag
- Minimum SDK: 26
- Target/compile SDK: 36
- Direct/Play signing certificate SHA-256: `fee392206a600443c5ad6fa469a81e44dc378bd651d41971c8f888f6bdf935d8`
- Play AAB: `tomi-release.aab` from the `playRelease` variant
- Direct APK: `tomi-release.apk` from the `directRelease` variant

## Ready assets

| Asset | Path | Notes |
| --- | --- | --- |
| App icon | `docs/store/icon-512.png` | 512×512 RGBA PNG; source SVG beside it |
| Feature graphic | `docs/store/feature-1024x500.png` | 1024×500 RGB PNG; source SVG beside it |
| Privacy policy | `docs/privacy.html` | Published through GitHub Pages at `https://okyssta.github.io/Tomi/privacy.html` |
| License notices | `app/src/main/assets/licenses.txt` | Bundled and shown in Settings → About & licenses |

Screenshots are intentionally not fabricated. Capture them from a real installed build on a phone.

## Listing copy

**Title:** Tomi: Move & Focus

**Short description (80 characters max):** Offline movement reminders, steps, tasks, journal, water, and daily check-ins.

**Full description:**

Tomi helps people who sit at a desk build a healthier rhythm without an account or cloud sync. Set a movement reminder every 30–45 minutes during your active hours, then take a walk, stretch, stand, or log a push-up set.

Tomi can read aggregate step totals through Android Health Connect, show daily and weekly progress, and optionally reset the reminder when walking is detected. Tasks support both a due date and a due time. Keep journal notes, mood and energy check-ins, water, and movement history in one offline-first app.

Your tasks, journal, check-ins, and settings stay in Tomi's app-private storage. Export and import JSON backups explicitly. No ads, analytics SDK, account, or Tomi cloud upload.

Movement reminders use Android notifications and alarms. Exact-alarm access is optional; Tomi falls back to inexact alarms when it is not granted. Health Connect background access is optional and requested separately with an in-app disclosure.

The Play edition relies on Play updates. The direct GitHub edition includes an optional verified APK updater.

## Play Console — required before production

1. Create/reserve `com.tomi.app`; complete developer identity verification and agreements.
2. Configure Play App Signing with the existing certificate above for continuity with direct-install users. Register a separate upload key if Play requires one; preserve the app-signing key.
3. Upload `tomi-release.aab` to internal testing and inspect the merged manifest, package, version, SDKs, and signing identity.
4. Set the privacy-policy URL to `https://okyssta.github.io/Tomi/privacy.html`.
5. Complete App content: Data safety, data deletion, ads declaration, target audience, IARC rating, Health apps declaration, and any permission declarations shown by Play Console.
6. Declare no ads and no user accounts only after confirming the final dependency/network metadata report.
7. In Health apps, request Steps only. Justify daily/weekly progress and smart pause; separately justify optional background access for closed-app smart pause and widget refresh. Runtime disclosure and policy must match the Console declaration.
8. Add at least two real phone screenshots. Recommended: Home, active reminder, task editor with date and time, Progress, and Settings/privacy.
9. Run the Play pre-launch report and physical-device QA before staged production rollout.

## Data safety worksheet

| Data type | Collected? | Shared? | Processing |
| --- | --- | --- | --- |
| Tasks, notes, journal, mood, energy, water, movement | Stored on device only | No | Optional explicit user export/import |
| Steps | Read from Health Connect; cached on device | No | Progress display and optional smart pause |
| App activity/performance | No first-party analytics or crash upload | No | None |
| Device or other identifiers | No first-party collection | No | None |
| Direct-edition release check | GitHub receives the ordinary request metadata for a release lookup | Request to GitHub | Update check only |

Health Connect source records remain with Health Connect and their original apps. Clearing Tomi does not delete them.

## Physical-device QA matrix

Test representative Android 8, 13, 14, and 16 devices where practical:

- Health Connect absent, update required, foreground denied/granted/revoked, background denied/granted/revoked.
- Notification permission denied/granted; exact alarms denied/granted; Doze and vendor battery optimization.
- Reminder interval, sound-only, vibration-only, sound+vibration, snooze crossing active hours/day boundaries.
- Reboot, app replacement, manual clock change, timezone change, and overdue catch-up.
- Real step totals, smart pause race behavior, widget refresh, and midnight rollover while Tomi stays open.
- Task due date/time across positive and negative time zones; backup export/import; invalid oversized import; clear-all cancellation.
- Direct updater only: network failure, redirect, checksum mismatch, wrong signing key, notification denial, install confirmation, and upgrade over an existing GitHub-installed build.
- Play update over an existing GitHub-installed build after Play App Signing is configured.
- TalkBack, 200% font scale, landscape, short screens, and large-text task/onboarding layouts.

## Release gate

Do not mark Tomi as Play-live until the Console declaration, physical-device QA, pre-launch report, and staged rollout all pass. A signed AAB alone is not a production submission.
