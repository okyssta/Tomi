# Releases

## Tag flow

1. Update `CHANGELOG.md`.
2. Run `./gradlew testDebugUnitTest lintDebug assembleDebug`.
3. Commit and push to `main`.
4. Create and push a semantic version tag:

   ```sh
   git tag v1.0.1
   git push origin v1.0.1
   ```

5. The release workflow validates the tag, derives a monotonically increasing numeric `versionCode`, runs tests and release lint, builds a minified signed APK, verifies its signature, writes `SHA256SUMS.txt`, and publishes a GitHub release.

Tags containing a hyphen, such as `v1.1.0-beta.1`, are published as prereleases.

## Required repository secrets

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore |
| `ANDROID_STORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | Signing key password |

The workflow decodes the keystore into the runner's temporary directory and writes an untracked `keystore.properties` file. The key is never committed.

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
./gradlew testDebugUnitTest lintRelease assembleRelease
```

The release workflow and local release must use the same keystore if the APKs are expected to update one another.

## Published assets

- `tomi-release.apk`
- `mapping.txt`
- `signature.txt`
- `SHA256SUMS.txt`

Verify a published release:

```sh
./scripts/verify-release.sh v1.0.1
```

The script downloads the assets into a temporary directory and runs `sha256sum --check SHA256SUMS.txt` from that directory.

## Updater behavior

Tomi checks GitHub at most once per day in the background and whenever the user taps **Check**. Automatic downloads are Wi-Fi-only and can be disabled. Downloads are SHA-256 verified. Installation always opens Android's package installer and requires user confirmation.
