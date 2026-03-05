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
"$GRADLE_EXE" --no-daemon clean
"$GRADLE_EXE" --no-daemon lint test assembleDebug
