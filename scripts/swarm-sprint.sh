#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
bash "$(dirname "$0")/check-prereqs.sh" >/dev/null

# Swarm requires git worktree
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "❌ git repo required for swarm"; exit 2; }

# Detect base branch
ROOT_BRANCH="${ROOT_BRANCH:-}"
if [[ -z "$ROOT_BRANCH" ]]; then
  ROOT_BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
  if [[ -z "$ROOT_BRANCH" || "$ROOT_BRANCH" == "HEAD" ]]; then
    ROOT_BRANCH="main"
  fi
fi

SWARM_BASE_DIR="${SWARM_BASE_DIR:-agent/swarm-worktrees}"
mkdir -p "$SWARM_BASE_DIR" "agent/logs"

echo "▶ Swarm Sprint start"
echo "   - base branch: $ROOT_BRANCH"
echo "   - worktrees:   $SWARM_BASE_DIR"

declare -A SWARMS=(
  ["ui"]="UI/Compose: Periodic Table(zoom/pan/tap) + Element Detail skeleton + property visualization(1 property)"
  ["data"]="Data: Room schema + offline dataset import pipeline + repository + search index baseline"
  ["search"]="Search: name/symbol/atomicNumber + 1~2 property filters + result list visualization"
  ["calc"]="Calculator: formula parser v1 + molar mass compute + input helper"
  ["glossary"]="Glossary+Notes: glossary search/detail linking + element notes CRUD"
)

# Create/refresh worktrees
for name in "${!SWARMS[@]}"; do
  wt="$SWARM_BASE_DIR/$name"
  branch="swarm/$name"

  # Remove existing worktree if present
  if git worktree list --porcelain | grep -q "worktree ${wt}"; then
    echo " - removing existing worktree: $wt"
    git worktree remove -f "$wt" || true
  fi
  rm -rf "$wt" || true

  if git show-ref --verify --quiet "refs/heads/$branch"; then
    git branch -f "$branch" "$ROOT_BRANCH"
    git worktree add "$wt" "$branch"
  else
    git worktree add -b "$branch" "$wt" "$ROOT_BRANCH"
  fi
done

# Run swarms in parallel
pids=()
for name in "${!SWARMS[@]}"; do
  wt="$SWARM_BASE_DIR/$name"
  branch="swarm/$name"
  scope="${SWARMS[$name]}"
  log="agent/logs/swarm-${name}.md"

  (
    cd "$wt"
    # per-worktree caches
    export GRADLE_USER_HOME="${PWD}/.gradle-user-home"
    export ANDROID_USER_HOME="${PWD}/.android-home"
    mkdir -p "$GRADLE_USER_HOME" "$ANDROID_USER_HOME" "agent/logs" || true

    prompt="당신은 '$name' 스웜 에이전트입니다.
반드시 읽을 문서: AGENTS.md, agent/PRD.md, agent/BACKLOG.md, agent/ACCEPTANCE.md

범위(scope):
- ${scope}

규칙:
- scope를 벗어난 변경은 최소화.
- 필요하면 interface/placeholder를 만들고 통합은 merge 단계에서 해결 가능하게.
- 마지막에 'bash scripts/verify.sh'를 통과시키거나, 실패 시 원인/다음 액션을 정확히 기록.

출력:
- 완료한 것, 변경 파일, verify 요약, 남은 위험/충돌 포인트
"

    codex exec --full-auto --ephemeral "$prompt" | tee "$log"

    # Try verify (best-effort; if fails, still commit to share progress)
    bash scripts/verify.sh || true

    git add -A
    git commit -m "swarm($name): ${scope}" || true
  ) &

  pids+=($!)
done

for pid in "${pids[@]}"; do
  wait "$pid"
done

echo "▶ Swarm workers finished. Start merge into $ROOT_BRANCH"

git checkout "$ROOT_BRANCH"

# Merge sequentially (stop on conflict)
for name in ui data search calc glossary; do
  branch="swarm/$name"
  echo " - merging $branch"
  git merge --no-ff "$branch" || {
    echo "❌ merge conflict on $branch"
    echo "   해결 후: bash scripts/verify.sh"
    exit 1
  }
done

echo "▶ Final verify"
bash scripts/verify.sh

echo "✅ Swarm Sprint done (merged + verified)"
