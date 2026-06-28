#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "$0")/env.sh"

fail() { echo "ERROR: $1" >&2; exit 2; }
ok() { echo "OK: $1"; }
warn() { echo "WARN: $1"; }

command -v bash >/dev/null 2>&1 || fail "bash not found"
command -v git >/dev/null 2>&1 || fail "git not found"
command -v java >/dev/null 2>&1 || warn "java not found; Gradle/Android builds may fail"

if [[ -x "$PROJECT_ROOT/gradlew" ]]; then
  export GRADLE_CMD="$PROJECT_ROOT/gradlew"
elif command -v gradle >/dev/null 2>&1; then
  export GRADLE_CMD="$(command -v gradle)"
else
  fail "Neither gradlew nor gradle was found. Install Android Gradle project tools or add gradle to PATH."
fi

if [[ -x "$PROJECT_ROOT/gradlew" ]]; then
  ok "using Gradle wrapper: $GRADLE_CMD"
else
  ok "gradlew unavailable; using system gradle: $GRADLE_CMD"
fi

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  ok "git repo detected"
else
  warn "not a git repo; build verification can still run, but repository checks are unavailable"
fi

ok "prereqs check done"
