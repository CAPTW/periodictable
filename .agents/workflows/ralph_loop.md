---
description: Ralph Loop — verify→fix 자동 반복 루프 실행
---

# Ralph Loop 실행

## Windows (PowerShell)

1. 사전조건 확인:
```powershell
.\scripts\check-prereqs.ps1
```

2. Ralph Loop 실행:
```powershell
.\scripts\ralph-loop.ps1
```

3. 드라이런 (codex 호출 없이 프롬프트만 저장):
```powershell
.\scripts\ralph-loop.ps1 -DryRun
```

4. 옵션:
```powershell
.\scripts\ralph-loop.ps1 -MaxIters 5 -AutoCommit
```

## Unix (Bash)
```bash
bash scripts/ralph-loop.sh
```

## 실패 시 대응
- `agent/logs/verify-*.txt` — 각 iteration의 verify 출력
- `agent/logs/codex-*.md` — AI 에이전트에 전달된 프롬프트 / 출력
- BACKLOG를 더 잘게 쪼개서 재시도
