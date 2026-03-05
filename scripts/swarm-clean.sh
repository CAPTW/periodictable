#!/usr/bin/env bash
set -euo pipefail

SWARM_BASE_DIR="${SWARM_BASE_DIR:-agent/swarm-worktrees}"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "❌ git repo required"
  exit 2
fi

if [[ ! -d "$SWARM_BASE_DIR" ]]; then
  echo "No swarm worktrees found at $SWARM_BASE_DIR"
  exit 0
fi

for wt in "$SWARM_BASE_DIR"/*; do
  [[ -d "$wt" ]] || continue
  echo "Removing worktree: $wt"
  git worktree remove -f "$wt" || true
done

rm -rf "$SWARM_BASE_DIR" || true
echo "✅ swarm worktrees cleaned"
