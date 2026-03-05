#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "$0")/env.sh"

fail() { echo "??$1" >&2; exit 2; }
ok() { echo "??$1"; }

command -v bash >/dev/null 2>&1 || fail "bash not found"
command -v git  >/dev/null 2>&1 || fail "git not found (required)"
command -v codex >/dev/null 2>&1 || echo "⚠️ codex CLI not found (optional — ralph-loop will save prompts to agent/logs)"
command -v java >/dev/null 2>&1 || echo "⚠️ java not found — Gradle/Android builds may fail"

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

# Git repo check (swarm requires it; ralph-loop can still work without, but recommended)
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  ok "git repo detected"
else
  echo "?醫묓닔 Not a git repo. Swarm will fail. Ralph Loop can still run, but commit/merge features won't work."
fi

ok "prereqs check done"
