# Verification

Verification is recorded here only after the corresponding command or artifact check actually runs.

## Current candidate: 1.1.4 (version code 1001004)

### Published release

Release: <https://github.com/okyssta/Tomi/releases/tag/v1.1.4>

- Release workflow run `45863479666` completed successfully at commit `e6436afc00b852db1279bc648e6638fb50b63b66`.
- All seven published assets downloaded; checksums passed.
- Published `tomi-release.apk` SHA-256: `1d8a1b5d3dcecb260df97e5b3679747e92a7dedd3bda220a48853882fda48aca`.
- `scripts/verify-release.sh v1.1.4` passed package/version/SDK identity, APK and AAB signer identity, 16 KB alignment, Play exclusions, and all checksums.

### Namaz addon

- Added a generic addons setting and the optional Namaz prayer-time/tracker addon.
- Prayer times are calculated on-device with Adhan 1.2.1 using coarse location requested only on demand; no prayer network service or Tomi server is involved.
- Hanafi/Shafi Asr and Karachi/MWL/ISNA methods are selectable.
- Prayer offered/not-offered records are stored in the new `prayer_logs` Room table and included in JSON/Drive backups.
- The database migration from version 1 to 2 creates the table without dropping existing records.
- Direct tests: **195 passed, 0 failures, 0 errors, 0 skipped**.
- Play tests: **191 passed, 0 failures, 0 errors, 0 skipped**.
- Lint: direct and Play debug lint passed.

## Previous release: 1.1.3 (version code 1001003)

### Google Drive backup

- Added a Storage Access Framework Google Drive backup mode; the user selects a folder, Tomi writes `Tomi Backups/tomi-backup.json`, and no Google OAuth client or Play Services account is involved.
- Manual backup, restore with replacement confirmation, disconnect, six-hour periodic sync, and app-close sync are implemented.
- Direct tests: **187 passed, 0 failures, 0 errors, 0 skipped**.
- Play tests: **183 passed, 0 failures, 0 errors, 0 skipped**.
- `GoogleDriveBackupRepositoryTest` covers connect/backup, disabled auto-sync, disconnect, and restore input.
- `SettingsRepositoryTest` covers persistence of the Drive folder grant without disturbing other settings.

### Published release

Release: <https://github.com/okyssta/Tomi/releases/tag/v1.1.3>

- Release workflow run `45832107265` completed successfully at commit `3d442640b9e867035e0a96275d51de08ea4a9bd0`.
- All seven published assets downloaded; checksums passed.
- Published `tomi-release.apk` SHA-256: `1c6b382107838a932a0b5ae3fdd0b9caf48c701499171fb98f5f495b4463b7b3`.
- `scripts/verify-release.sh v1.1.3` passed package/version/SDK identity, APK and AAB signer identity, 16 KB alignment, Play exclusions, and all checksums.

## Previous release: 1.1.2 (version code 1001002)

### Published release

Release: <https://github.com/okyssta/Tomi/releases/tag/v1.1.2>

- Release workflow run `45818302007` completed successfully at commit `d38cf002cd00b09cccf7ac3b5c402ac37bcda568`.
- All seven published assets downloaded; `sha256sum --check SHA256SUMS.txt` passed.
- Published `tomi-release.apk` SHA-256: `fa01d70edf19e76ac55c3a46f292f889807c2fdce501b161ff45abf12e0da5c8`.
- `scripts/verify-release.sh v1.1.2` passed package/version/SDK identity, APK and AAB signer identity, 16 KB alignment, Play exclusions, and all checksums.

### Adaptive launcher icon fit

- The combined T + steps foreground was scaled to 80% and recentered on the adaptive-icon canvas, keeping both foreground and monochrome artwork inside Android's 66dp safe circle.
- `LauncherIconGeometryTest` checks both foreground variants for safe-circle containment and horizontal/vertical centering.
- Direct tests: **182 passed, 0 failures, 0 errors, 0 skipped**.
- Play tests: **178 passed, 0 failures, 0 errors, 0 skipped**.
- Local signed direct APK: `dist/Tomi-1.1.2.apk`, SHA-256 `81a518f067ac76cac266eca4def05e2c3c1bc036f84a7e5bfc97f63af5920bdd`, version `1.1.2`, code `1001002`, signed by the existing Tomi release certificate.

## Previous release: 1.1.1 (version code 1001001)

### Published release

Release: <https://github.com/okyssta/Tomi/releases/tag/v1.1.1>

- Release workflow run `45786195820` completed successfully at commit `0fbc931761fbce792f92c0c409c3cdf86a2f9153`.
- All seven published assets downloaded; `sha256sum --check SHA256SUMS.txt` passed.
- Published `tomi-release.apk` SHA-256: `1d780df2141a37c41f43e35218eab6a98ae7718774ccdea2f9621dda7bd9ec65`.
- `scripts/verify-release.sh v1.1.1` passed package/version/SDK identity, APK and AAB signer identity, 16 KB alignment, Play exclusions, and all checksums.

### Update/startup crash hardening

- Startup reminder and update scheduling failures no longer escape `TomiApp.onCreate`.
- Settings and update-download DataStore files recover from corruption with default values instead of terminating the app.
- Update-check and installer launch failures now surface as in-app errors; the installer activity also catches unresolvable system intents.
- `SettingsCorruptionTest` writes a deliberately corrupt preferences file and verifies that `SettingsRepository` falls back to defaults.
- Direct tests: **180 passed, 0 failures, 0 errors, 0 skipped**.
- Play tests: **176 passed, 0 failures, 0 errors, 0 skipped**.
- Local signed direct APK: `dist/Tomi-1.1.1.apk`, SHA-256 `260160eec1f00e27f2e8195a97799791070aab34be2a2eabbb67b446a9dc7a43`, version `1.1.1`, code `1001001`, signed by the existing Tomi release certificate.

## Previous release: 1.1.0 (version code 1001000)

### Published release

Release: <https://github.com/okyssta/Tomi/releases/tag/v1.1.0>

- Release workflow run `45668315427` completed successfully at commit `70b52430ef8e644f8e7336f17f50aa4cb8aa6c35`.
- All seven published assets downloaded successfully.
- `sha256sum --check SHA256SUMS.txt` passed from the downloaded directory.
- Published `tomi-release.apk` SHA-256: `035cc7a3734c08b448bc0e4e83828c33ab7153492c08804f66b4011336c3b88a`.
- Published APK/AAB exact package, version, SDK, certificate, Play exclusions, and APK 16 KB alignment passed `scripts/verify-release.sh v1.1.0`.

### Automated checks

| Check | Command | Result |
| --- | --- | --- |
| Direct-flavor unit, Room, update, and Compose UI tests | `./gradlew :app:testDirectDebugUnitTest` | Passed: 179 tests, 0 failures, 0 errors, 0 skipped |
| Play-flavor unit, Room, update, and Compose UI tests | `./gradlew :app:testPlayDebugUnitTest` | Passed: 175 tests, 0 failures, 0 errors, 0 skipped |
| Direct debug lint | `./gradlew :app:lintDirectDebug` | Passed |
| Play debug lint | `./gradlew :app:lintPlayDebug` | Passed |
| Direct release lint | `./gradlew :app:lintDirectRelease` | Passed |
| Play release lint | `./gradlew :app:lintPlayRelease` | Passed |
| Workflow syntax | `actionlint v1.7.12` | Passed for CI, release, Pages, and CodeQL workflows |
| Signed direct APK | `./gradlew :app:assembleDirectRelease -PVERSION_CODE=1001000 -PVERSION_NAME=1.1.0` | Passed |
| Signed Play AAB | `./gradlew :app:bundlePlayRelease -PVERSION_CODE=1001000 -PVERSION_NAME=1.1.0` | Passed |
| Published-asset verifier | `scripts/verify-release.sh` | Negative CLI tests pass for `v0.0.0`, regressed version codes, and a tampered Bundletool jar; remote 1.1.0 verification pending publication |

Raw JUnit XML, lint text reports, test totals, artifact metadata, and hashes are collected under `dist/evidence/` by `dist/collect_evidence.py`.

### Release-policy and publication evidence

- `bash scripts/test-verify-release.sh` passed. It proves `v0.0.0` and version-code regressions are rejected before download, and a Bundletool jar with the wrong SHA-256 is rejected before Java executes it.
- The release workflow explicitly compares both APK and AAB signer digests with the pinned certificate.
- GitHub Pages publishes the `main` branch `/docs` directory; the release workflow compares the public policy SHA-256 with the repository file before publishing.
- The canonical URL `https://okyssta.github.io/Tomi/privacy.html` returned **HTTP 200** and its downloaded bytes matched `docs/privacy.html`.

### Local artifact identity

```text
fe0e0437bb6182b32617f1cce2d48217c0de6a9155374b3863a30f08dceb9c14  tomi-release.apk
b9f95c520be317e6240be673b07cbff34bad5e4faebd671c065b7a55421adfc0  tomi-release.aab
bb9ed2395832cde40683228dccda18a275c36413940efd474e3a1ae7d8c97f36  mapping.txt
b927216844897974b67bf6211a2edebb10abee86bbc106cedf9fce1acf9d85b0  mapping-play.txt
```

- APK size: 10,885,435 bytes.
- AAB size: 10,140,211 bytes.
- Package: `com.tomi.app`; version `1.1.0`; version code `1001000`.
- Minimum SDK 26; target/compile SDK 36.
- APK Signature Scheme v2; one 4096-bit RSA signer.
- Signing certificate SHA-256: `fee392206a600443c5ad6fa469a81e44dc378bd651d41971c8f888f6bdf935d8`.
- `apksigner verify`, AAB `jarsigner -verify`, and APK 16 KB `zipalign -c -P 16 4` passed.
- The Play AAB contains no direct updater worker, notifier, installer activity, installer permission, or foreground data-sync permission.
- The direct APK contains the updater and its installer-only permissions.

### Correctness coverage added for the polished release

- Configurable reminder start/end boundaries, inactive days, ten-minute snooze timing, manifest clock-change routing, boot/time catch-up, repeated alarm delivery, and alarm-channel migration.
- DST gap/fold handling, backward-clock normalization, reminder delivery locking, Health Connect current-day query clamping, and monotonic smart-pause checkpoints.
- Local-midnight date rollover and external-route allowlisting.
- UTC task date-picker conversion and task due-date removal.
- Bounded symmetric backup input/output, safe AUTOINCREMENT IDs, field validation, duplicate IDs, restore ordering, rollback, cancellation, and scheduler-failure behavior.
- Direct update URL validation, size limits, package/version/signing identity, persisted retry metadata, nonterminal/terminal WorkManager state, unique temporary files, and atomic promotion.
- Compose recreation restoration, save-failure draft retention, narrow Home quick actions, named toggles, page indicators, chart summary, mood labels, slider semantics, and license assets.

## Runtime limits

The build host is ARM64 and has no `/dev/kvm` and no attached Android device. A full Android emulator was not used. Compose/Robolectric tests cover UI semantics and editor behavior; notification delivery, Health Connect permission screens, package installation, and real step sensor data still require physical-device verification.

The host's Robolectric native SQLite runtime is unsupported on Linux ARM64. DAO tests therefore use `BundledSQLiteDriver`; production Room uses Android framework SQLite, and backup restore ordering is tested as a pure coroutine coordinator. This distinction is intentional and documented in `docs/DEVELOPMENT.md`.

## Play readiness limits

A signed Play AAB and store icon/feature graphic exist. Google Play production submission is **not** claimed complete: Play App Signing, Console declarations, real screenshots, physical-device QA, pre-launch report, and staged rollout remain manual gates. See `docs/STORE.md`.

## Historical published baseline: 1.0.0

Release: <https://github.com/okyssta/Tomi/releases/tag/v1.0.0>

- Tag `v1.0.0` resolves to commit `edc2af8febebec57fbfff008227822c9c3024a39`.
- Release workflow run `45457930601` completed successfully.
- All four published assets downloaded successfully.
- `sha256sum --check SHA256SUMS.txt` passed from the downloaded asset directory.
- Published `tomi-release.apk` SHA-256: `6891addce1a52fb00afc0cce2dff8208b685f1087d69af15be824ec5c30a3efb`.
- The downloaded APK passed `apksigner verify` and reported the expected package, version, and SDK levels.
- The published APK hash differs from the local build hash because the two build environments did not produce byte-identical APKs. Both verify with the same signing certificate.
