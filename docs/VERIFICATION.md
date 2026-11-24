# Verification

Verification is recorded here only after the corresponding command or artifact check actually runs.

## Automated checks

| Check | Command | Result |
| --- | --- | --- |
| Unit, Room, update, and Compose UI tests | `./gradlew :app:testDebugUnitTest` | Passed: 54 tests, 0 failures, 0 errors, 0 skipped |
| Android lint | `./gradlew :app:lintDebug` | Passed |
| Release lint | `./gradlew :app:lintRelease` | Passed |
| Workflow syntax | `actionlint -color` | Passed |
| Debug APK | `./gradlew :app:assembleDebug` | Passed |
| APK signature | `apksigner verify --verbose --print-certs app-debug.apk` | Passed for debug APK |
| Release APK | `./gradlew :app:assembleRelease -PVERSION_CODE=1000000 -PVERSION_NAME=1.0.0` | Passed |
| GitHub CI | GitHub Actions `ci.yml`, run `45457678152` at `edc2af8` | Passed |
| CodeQL | GitHub Actions `codeql.yml`, run `45456824882` at `1ef6293` | Passed |
| Signed GitHub release | GitHub Actions `release.yml`, run `45457930601` | Passed and published |

## Runtime limits

The build host is ARM64 and has no `/dev/kvm` and no attached Android device. A full Android emulator was not used. UI behavior is covered by a Compose/Robolectric onboarding test; notification delivery, Health Connect permission screens, package installation, and real step sensor data still require physical-device verification.

## Artifact identity

Local artifacts verified on the ARM64 build host:

```text
debug  25cc4237990265ce5c2f9dd7e4a098c1399ccfc51d0103c5c8085b29210461aa  app-debug.apk
release 3fe637219f2146e7cf4473f78ddd8b19dd1a0117363d2bdae2a5ca7bcc1dd1fa  app-release.apk
```

The release APK reports package `com.tomi.app`, version code `1000000`, version name `1.0.0`, minimum SDK 26, and target SDK 36. `apksigner` verified APK Signature Scheme v2 with one 4096-bit RSA signer (certificate SHA-256 `fee392206a600443c5ad6fa469a81e44dc378bd651d41971c8f888f6bdf935d8`).

## Published release verification

Release: <https://github.com/okyssta/Tomi/releases/tag/v1.0.0>

- Tag `v1.0.0` resolves to commit `edc2af8febebec57fbfff008227822c9c3024a39`.
- Release workflow run `45457930601` completed successfully.
- All four published assets downloaded successfully.
- `sha256sum --check SHA256SUMS.txt` passed from the downloaded asset directory.
- Published `tomi-release.apk` SHA-256: `6891addce1a52fb00afc0cce2dff8208b685f1087d69af15be824ec5c30a3efb`.
- The downloaded APK passed `apksigner verify` and reported the expected package, version, and SDK levels.
- The published APK hash differs from the local build hash; these two build environments did not produce byte-identical APKs. Both verify with the same signing certificate.
