#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"

if [[ -x "${GRADLE_CMD:-}" ]]; then
  GRADLE_EXE="$GRADLE_CMD"
elif [[ -f "$PROJECT_ROOT/gradlew" ]]; then
  chmod +x "$PROJECT_ROOT/gradlew" 2>/dev/null || true
  GRADLE_EXE="$PROJECT_ROOT/gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_EXE="$(command -v gradle)"
else
  echo "Neither gradlew nor gradle was found. Install Android Gradle project tools or add gradle to PATH." >&2
  exit 2
fi

# Basic Android validation (adjust to your project if needed)
cd "$PROJECT_ROOT"

if command -v python >/dev/null 2>&1; then
  python scripts/validate_assets.py
elif command -v python3 >/dev/null 2>&1; then
  python3 scripts/validate_assets.py
else
  echo "python runtime not found for asset validation" >&2
  exit 2
fi

remove_generated_dirs() {
  rm -rf \
    "$PROJECT_ROOT/app/build/generated/ksp" \
    "$PROJECT_ROOT/app/build/generated/hilt" \
    "$PROJECT_ROOT/app/build/generated/ap_generated_sources"
}

rm -rf "$PROJECT_ROOT/app/build" "$PROJECT_ROOT/build"
remove_generated_dirs
"$GRADLE_EXE" --no-daemon lintRelease testReleaseUnitTest assembleRelease
