#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd -P)"
export PROJECT_ROOT

# Workspace-local caches (sandbox friendly)
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-${PROJECT_ROOT}/.gradle-user-home}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-${PROJECT_ROOT}/.android-home}"

mkdir -p "$GRADLE_USER_HOME" "$ANDROID_USER_HOME" || true
