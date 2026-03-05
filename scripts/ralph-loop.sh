#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
bash "$(dirname "$0")/check-prereqs.sh" >/dev/null

MAX_ITERS="${MAX_ITERS:-12}"
AUTO_COMMIT="${AUTO_COMMIT:-false}"
LOG_DIR="agent/logs"
mkdir -p "$LOG_DIR"

echo "▶ Ralph Loop start (MAX_ITERS=$MAX_ITERS, AUTO_COMMIT=$AUTO_COMMIT)"
echo "   - logs: $LOG_DIR"
echo "   - run: bash scripts/verify.sh"

for i in $(seq 1 "$MAX_ITERS"); do
  echo ""
  echo "=== Iteration $i / $MAX_ITERS ==="

  if bash "$(dirname "$0")/verify.sh" >/dev/null 2>&1; then
    echo "✅ verify PASS — loop 종료"
    exit 0
  fi

  VERIFY_OUT="$LOG_DIR/verify-$i.txt"
  ( bash "$(dirname "$0")/verify.sh" ) >"$VERIFY_OUT" 2>&1 || true

  echo "❌ verify FAIL — Codex에게 수정 요청"
  echo "   - verify log: $VERIFY_OUT"

  STATUS_OUT="$LOG_DIR/git-status-$i.txt"
  DIFFSTAT_OUT="$LOG_DIR/git-diffstat-$i.txt"
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git status --porcelain >"$STATUS_OUT" || true
    git diff --stat >"$DIFFSTAT_OUT" || true
  else
    : >"$STATUS_OUT"
    : >"$DIFFSTAT_OUT"
  fi

  PROMPT_FILE="agent/prompts/ralph-loop.md"
  RUN_LOG="$LOG_DIR/codex-$i.md"

  codex exec --full-auto --ephemeral \
"$(cat "$PROMPT_FILE")

## Context
- iteration: $i / $MAX_ITERS

### git status (porcelain)
$(cat "$STATUS_OUT")

### git diff --stat
$(cat "$DIFFSTAT_OUT")

### verify output (tail 220)
$(tail -n 220 "$VERIFY_OUT")
" | tee "$RUN_LOG"

  if [[ "$AUTO_COMMIT" == "true" ]] && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git add -A
    git commit -m "codex: ralph-loop iteration $i" || true
  fi
done

echo ""
echo "🛑 MAX_ITERS 도달 — 실패 로그 확인: $LOG_DIR"
exit 1
