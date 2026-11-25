#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-}"
if [[ -z "$TAG" ]]; then
  echo "Usage: $0 vMAJOR.MINOR.PATCH" >&2
  exit 2
fi
if [[ ! "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Expected a stable vMAJOR.MINOR.PATCH tag" >&2
  exit 2
fi

REPO="${GITHUB_REPOSITORY:-okyssta/Tomi}"
VERSION_NAME="${TAG#v}"
VERSION_CODE=$(TAG="$TAG" python3 - <<'PY'
import os
import re
import subprocess
match = re.fullmatch(r"v(\d+)\.(\d+)\.(\d+)", os.environ["TAG"])
major, minor, patch = map(int, match.groups())
if os.environ["TAG"] != f"v{major}.{minor}.{patch}":
    raise SystemExit("Tag must use canonical vMAJOR.MINOR.PATCH form")
if max(major, minor, patch) > 999:
    raise SystemExit("Version components must be between 0 and 999")
code = major * 1_000_000 + minor * 1_000 + patch
if code <= 0 or code > 2_100_000_000:
    raise SystemExit("Derived versionCode must be greater than 0 and no more than 2100000000")
previous_codes = []
for previous in subprocess.check_output(["git", "tag", "--list"], text=True).splitlines():
    if previous == os.environ["TAG"]:
        continue
    old = re.fullmatch(r"v(\d+)\.(\d+)\.(\d+)", previous)
    if old:
        a, b, c = map(int, old.groups())
        previous_codes.append(a * 1_000_000 + b * 1_000 + c)
if previous_codes and code <= max(previous_codes):
    raise SystemExit(f"versionCode {code} must be greater than all published versionCodes")
print(code)
PY
)
if [[ ! "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
  echo "Version validation failed: $VERSION_CODE" >&2
  exit 1
fi
WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/tomi-release-check.XXXXXX")"
trap 'rm -rf "$WORKDIR"' EXIT
EXPECTED_CERT_SHA256="${EXPECTED_CERT_SHA256:-fee392206a600443c5ad6fa469a81e44dc378bd651d41971c8f888f6bdf935d8}"

if [[ -n "${RELEASE_DIR:-}" ]]; then
  cp "$RELEASE_DIR"/* "$WORKDIR/"
else
  gh release download "$TAG" --repo "$REPO" --dir "$WORKDIR"
fi
(
  cd "$WORKDIR"
  sha256sum --check SHA256SUMS.txt
)

APK="$WORKDIR/tomi-release.apk"
AAB="$WORKDIR/tomi-release.aab"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/root/android-sdk}}"
APKSIGNER="${APKSIGNER:-$SDK/build-tools/36.0.0/apksigner}"
AAPT2="${AAPT2:-$SDK/build-tools/36.0.0/aapt2}"
ZIPALIGN="${ZIPALIGN:-$SDK/build-tools/36.0.0/zipalign}"

"$APKSIGNER" verify --verbose --print-certs "$APK" > "$WORKDIR/apksigner.txt"
CERT_SHA256="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$WORKDIR/apksigner.txt" | tr -d ':\r' | tr '[:upper:]' '[:lower:]')"
test "$CERT_SHA256" = "$EXPECTED_CERT_SHA256"
"$AAPT2" dump badging "$APK" > "$WORKDIR/badging.txt"
python3 - "$WORKDIR/badging.txt" "$VERSION_CODE" "$VERSION_NAME" <<'PY'
import re, sys
text = open(sys.argv[1], encoding="utf-8").read()
match = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", text, re.MULTILINE)
if not match or match.groups() != ("com.tomi.app", sys.argv[2], sys.argv[3]):
    raise SystemExit(f"APK package/version mismatch: {match.groups() if match else None}")
PY
"$AAPT2" dump xmltree "$APK" --file AndroidManifest.xml > "$WORKDIR/manifest-tree.txt"
grep -q "minSdkVersion(0x0101020c)=26" "$WORKDIR/manifest-tree.txt"
grep -q "targetSdkVersion(0x01010270)=36" "$WORKDIR/manifest-tree.txt"
"$ZIPALIGN" -c -P 16 4 "$APK"
jarsigner -verify -verbose -certs "$AAB" > "$WORKDIR/jarsigner.txt" 2>&1
grep -q "jar verified" "$WORKDIR/jarsigner.txt"
AAB_CERT_SHA256="$(keytool -printcert -jarfile "$AAB" | sed -n 's/^[[:space:]]*SHA256: //p' | head -1 | tr -d ':\r' | tr '[:upper:]' '[:lower:]')"
test "$AAB_CERT_SHA256" = "$EXPECTED_CERT_SHA256"

if [[ -z "${BUNDLETOOL_JAR:-}" ]]; then
  echo "Set BUNDLETOOL_JAR to a verified bundletool-all jar to authenticate the AAB manifest." >&2
  exit 2
fi
BUNDLETOOL_SHA256="a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29"
read -r ACTUAL_BUNDLETOOL_SHA256 _ < <(sha256sum "$BUNDLETOOL_JAR")
if [[ "$ACTUAL_BUNDLETOOL_SHA256" != "$BUNDLETOOL_SHA256" ]]; then
  echo "Bundletool SHA-256 mismatch: expected $BUNDLETOOL_SHA256, got $ACTUAL_BUNDLETOOL_SHA256" >&2
  exit 1
fi
java -jar "$BUNDLETOOL_JAR" dump manifest --bundle="$AAB" > "$WORKDIR/aab-manifest.xml"
grep -q "package=\"com.tomi.app\"" "$WORKDIR/aab-manifest.xml"
grep -q "android:versionCode=\"$VERSION_CODE\"" "$WORKDIR/aab-manifest.xml"
grep -q "android:versionName=\"$VERSION_NAME\"" "$WORKDIR/aab-manifest.xml"
grep -q 'android:minSdkVersion="26"' "$WORKDIR/aab-manifest.xml"
grep -q 'android:targetSdkVersion="36"' "$WORKDIR/aab-manifest.xml"
if grep -q -E 'REQUEST_INSTALL_PACKAGES|FOREGROUND_SERVICE_DATA_SYNC|InstallUpdateActivity' "$WORKDIR/aab-manifest.xml"; then
  echo "Play AAB unexpectedly contains direct-update components or permissions." >&2
  exit 1
fi

echo "Verified release $TAG from $REPO in $WORKDIR"
echo "APK/AAB package/version/SDK, 16 KB alignment, both signing certificates, Play exclusions, and all checksums passed."
