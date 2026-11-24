#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-}"
if [[ -z "$TAG" ]]; then
  echo "Usage: $0 vMAJOR.MINOR.PATCH" >&2
  exit 2
fi

REPO="${GITHUB_REPOSITORY:-okyssta/Tomi}"
WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/tomi-release-check.XXXXXX")"
trap 'rm -rf "$WORKDIR"' EXIT

gh release download "$TAG" --repo "$REPO" --dir "$WORKDIR"
(
  cd "$WORKDIR"
  sha256sum --check SHA256SUMS.txt
)

APK="$WORKDIR/tomi-release.apk"
if command -v apksigner >/dev/null 2>&1; then
  apksigner verify --verbose --print-certs "$APK"
else
  echo "apksigner not found; checksum verification completed." >&2
fi

echo "Verified release $TAG from $REPO in $WORKDIR"
