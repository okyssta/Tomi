# Releases

## Tag flow

1. Update `CHANGELOG.md`.
2. Run `./gradlew :app:testDirectDebugUnitTest :app:testPlayDebugUnitTest :app:lintDirectDebug :app:lintPlayDebug`.
3. Commit and push to `main`.
4. Create and push a semantic version tag:

   ```sh
   git tag v1.0.1
   git push origin v1.0.1
   ```

5. The release workflow validates a stable semantic tag and its bounded, strictly increasing numeric `versionCode`, runs both flavor test suites and release lint, builds a minified signed direct APK and signed Play AAB, verifies both signatures, writes `SHA256SUMS.txt`, and publishes a GitHub release.

Only stable tags are published. Version components are limited to 0–999 and the derived code must be greater than every previous stable tag. This avoids collisions such as `v1.1.0` versus `v1.0.1000`.

## Distribution flavors

- `direct`: APK with GitHub release checks, verified downloads, and package-installer flow. Includes internet, foreground data sync, and unknown-app install permissions.
- `play`: AAB without the self-updater, installer activity, Tomi internet permission, or foreground data-sync permissions. Play manages updates.

Both flavors use the same application ID and release signing identity so direct and Play installations can upgrade one another when Play App Signing preserves that certificate. Upload the Play AAB to Play Console; the direct APK is for GitHub distribution.

## Required repository secrets

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore |
| `ANDROID_STORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | Signing key password |

The workflow decodes the keystore into the runner's temporary directory and writes an untracked `keystore.properties` file. The key is never committed.

## Privacy policy publication

The repository's GitHub Pages source is the `main` branch `/docs` directory. Every push that changes `docs/privacy.html` republishes the public policy at `https://okyssta.github.io/Tomi/privacy.html`. The release workflow downloads that URL and requires its SHA-256 to match the repository file before publishing artifacts.

## Local signing

Create a keystore and an untracked `keystore.properties`:

```properties
storeFile=/absolute/path/tomi-release.jks
storePassword=...
keyAlias=tomi-release
keyPassword=...
```

Then run:

```sh
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest lintDirectRelease lintPlayRelease assembleDirectRelease bundlePlayRelease -PVERSION_CODE=1001000 -PVERSION_NAME=1.1.0
```

The release workflow and local release must use the same keystore if the APKs are expected to update one another.

## Published assets

- `tomi-release.apk`
- `tomi-release.aab` (Play flavor)
- `mapping.txt` (direct APK)
- `mapping-play.txt` (Play AAB)
- `signature.txt`
- `aab-signature.txt`
- `SHA256SUMS.txt`

Verify a published release:

```sh
export BUNDLETOOL_JAR=/absolute/path/bundletool-all-1.18.3.jar
./scripts/verify-release.sh v1.0.1
```

The script downloads the assets into a temporary directory, runs `sha256sum --check SHA256SUMS.txt`, checks exact APK/AAB package/version/SDK metadata and both pinned signing certificates, verifies APK 16 KB alignment, and uses the supplied Bundletool jar to authenticate the AAB manifest and Play exclusions. It rejects `v0.0.0` and version codes that are not greater than every previous valid stable tag. Before executing Bundletool it requires SHA-256 `a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29` (Bundletool 1.18.3).

Run the negative CLI policy tests with:

```sh
bash scripts/test-verify-release.sh
```

## Updater behavior

The direct edition checks GitHub at most once per day in the background and whenever the user taps **Check**. Automatic downloads are Wi-Fi-only and can be disabled. Downloads are size-limited and must pass SHA-256, package/version, and pinned signing-certificate checks. Settings observes WorkManager and offers Install or Retry even when notifications are denied. Installation always opens Android's package installer and requires user confirmation. The Play edition relies on Play updates.

## Play submission gates

The signed AAB is a build artifact, not a completed Play launch. Before production rollout, complete Play App Signing with certificate continuity, developer verification, Data safety, Health apps declarations, privacy-policy URL, IARC rating, ads/accounts declarations, store listing assets, physical-device QA, pre-launch report, and a staged rollout. See `docs/STORE.md`.
