#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFY="${VERIFY:-$ROOT/scripts/verify-release.sh}"
TEST_DIR="$(mktemp -d "${TMPDIR:-/tmp}/tomi-release-policy-test.XXXXXX")"
trap 'rm -rf "$TEST_DIR"' EXIT
mkdir -p "$TEST_DIR/bin" "$TEST_DIR/repo"

cat > "$TEST_DIR/bin/gh" <<'SH'
#!/usr/bin/env bash
touch "$GH_CALLED_MARKER"
exit 99
SH
chmod +x "$TEST_DIR/bin/gh"

cat > "$TEST_DIR/bin/apksigner" <<'SH'
#!/usr/bin/env bash
printf 'Signer #1 certificate SHA-256 digest: fee392206a600443c5ad6fa469a81e44dc378bd651d41971c8f888f6bdf935d8\n'
SH
cat > "$TEST_DIR/bin/aapt2" <<'SH'
#!/usr/bin/env bash
if [[ "$*" == *"dump badging"* ]]; then
  printf "package: name='com.tomi.app' versionCode='1002000' versionName='1.2.0'\n"
else
  printf 'minSdkVersion(0x0101020c)=26\ntargetSdkVersion(0x01010270)=36\n'
fi
SH
cat > "$TEST_DIR/bin/zipalign" <<'SH'
#!/usr/bin/env bash
exit 0
SH
cat > "$TEST_DIR/bin/jarsigner" <<'SH'
#!/usr/bin/env bash
printf 'jar verified.\n'
SH
cat > "$TEST_DIR/bin/keytool" <<'SH'
#!/usr/bin/env bash
printf 'SHA256: FEE392206A600443C5AD6FA469A81E44DC378BD651D41971C8F888F6BDF935D8\n'
SH
cat > "$TEST_DIR/bin/java" <<'SH'
#!/usr/bin/env bash
touch "$JAVA_CALLED_MARKER"
exit 0
SH
chmod +x "$TEST_DIR/bin/"*
export JAVA_CALLED_MARKER="$TEST_DIR/java-called"
export PATH="$TEST_DIR/bin:$PATH"
export GH_CALLED_MARKER="$TEST_DIR/gh-called"

cd "$TEST_DIR/repo"
git init -q
git config user.email tests@example.invalid
git config user.name Tests
git commit --allow-empty -qm initial
git tag v1.0.1000
git tag v1.1.1

assert_rejected_before_download() {
  local tag="$1" expected="$2" output
  rm -f "$GH_CALLED_MARKER"
  if output="$(bash "$VERIFY" "$tag" 2>&1)"; then
    echo "Expected $tag to be rejected: $output" >&2
    exit 1
  fi
  if [[ "$output" != *"$expected"* ]]; then
    echo "Expected rejection containing: $expected" >&2
    echo "Actual output: $output" >&2
    exit 1
  fi
  if [[ -e "$GH_CALLED_MARKER" ]]; then
    echo "$tag reached release download before rejection" >&2
    exit 1
  fi
}

assert_rejected_before_download v0.0.0 "Derived versionCode must be greater than 0"
assert_rejected_before_download v1.1.0 "must be greater than all published versionCodes"
assert_rejected_before_download v1.0.1000 "Version components must be between 0 and 999"
assert_rejected_before_download v1.0.999 "must be greater than all published versionCodes"
assert_rejected_before_download v1.01.1 "Tag must use canonical vMAJOR.MINOR.PATCH form"
assert_rejected_before_download v1.2.0-rc.1 "Expected a stable vMAJOR.MINOR.PATCH tag"
assert_rejected_before_download v1000.0.0 "Version components must be between 0 and 999"

git tag -d v1.0.1000 >/dev/null
git tag v2.0.0
assert_rejected_before_download v1.999.999 "must be greater than all published versionCodes"
git tag -d v2.0.0 >/dev/null

mkdir -p "$TEST_DIR/release"
printf 'apk\n' > "$TEST_DIR/release/tomi-release.apk"
printf 'aab\n' > "$TEST_DIR/release/tomi-release.aab"
(
  cd "$TEST_DIR/release"
  sha256sum tomi-release.apk tomi-release.aab > SHA256SUMS.txt
)
printf 'tampered bundletool\n' > "$TEST_DIR/bundletool.jar"
rm -f "$JAVA_CALLED_MARKER"
if output="$(RELEASE_DIR="$TEST_DIR/release" BUNDLETOOL_JAR="$TEST_DIR/bundletool.jar" APKSIGNER="$TEST_DIR/bin/apksigner" AAPT2="$TEST_DIR/bin/aapt2" ZIPALIGN="$TEST_DIR/bin/zipalign" bash "$VERIFY" v1.2.0 2>&1)"; then
  echo "Expected a tampered Bundletool jar to be rejected" >&2
  exit 1
fi
if [[ "$output" != *"Bundletool SHA-256 mismatch"* ]]; then
  echo "Expected Bundletool hash rejection" >&2
  echo "Actual output: $output" >&2
  exit 1
fi
if [[ -e "$JAVA_CALLED_MARKER" ]]; then
  echo "Bundletool was executed before its hash was verified" >&2
  exit 1
fi

rm -f "$JAVA_CALLED_MARKER"
if output="$(RELEASE_DIR="$TEST_DIR/release" BUNDLETOOL_JAR="" APKSIGNER="$TEST_DIR/bin/apksigner" AAPT2="$TEST_DIR/bin/aapt2" ZIPALIGN="$TEST_DIR/bin/zipalign" bash "$VERIFY" v1.2.0 2>&1)"; then
  echo "Expected a missing Bundletool jar to be rejected" >&2
  exit 1
fi
if [[ "$output" != *"Set BUNDLETOOL_JAR to a verified bundletool-all jar"* ]]; then
  echo "Expected missing Bundletool jar rejection" >&2
  echo "Actual output: $output" >&2
  exit 1
fi
if [[ -e "$JAVA_CALLED_MARKER" ]]; then
  echo "Bundletool was executed without a supplied jar" >&2
  exit 1
fi

echo "verify-release policy tests passed"
