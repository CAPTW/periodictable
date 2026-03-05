# Periodic Table Android App — Codex Automation Kit (Ralph Loop + Agent Swarm)

이 ZIP은 **기존 Android(Gradle) 프로젝트 루트**에 그대로 풀어서, 아래 **한 줄 명령어**로
Codex가 자동으로 개발 루프를 돌릴 수 있게 해주는 “오케스트레이션/에이전트 구성”입니다.

> ✅ 이 키트는 “앱 코드” 자체가 아니라, **자동 개발을 위한 규칙/프롬프트/스크립트/설정**을 제공합니다.
> (당신의 프로젝트에 이미 `./gradlew`가 있어야 합니다.)

---

## 1) 빠른 시작

### (선택) Antigravity 안전장치 설치
```bash
bash scripts/setup-antigravity.sh
```

### Ralph Loop 실행 (1줄)
```bash
bash scripts/ralph-loop.sh
```

### Agent Swarm 실행 (병렬 worktree + merge까지, 1줄)
```bash
bash scripts/swarm-sprint.sh
```

---

## 2) 필수 조건(Prereqs)

### 필수
- Git 저장소( Swarm는 git worktree를 사용 )
- Android Gradle 프로젝트 루트( `./gradlew` 존재 )
- `codex` CLI 설치 및 PATH 등록  
  - 확인: `codex --version`

### 권장
- JDK 17(프로젝트 요구 버전에 맞추세요)
- 충분한 디스크(Gradle 캐시가 `.gradle-user-home/`에 생성됨)

---

## 3) 환경변수(옵션)

### Stitch MCP (UI/UX 생성용)
```bash
export STITCH_API_KEY="YOUR_KEY"
```
- Stitch를 사용하지 않거나 키가 없으면: 그대로 동작(단, UI 생성 자동화는 생략될 수 있음)

### Ralph Loop
```bash
export MAX_ITERS=12        # 기본 12
export AUTO_COMMIT=false  # 기본 false (true면 매 루프 자동 커밋 시도)
```

### Swarm
```bash
export ROOT_BRANCH=main    # 기본: 현재 브랜치 자동 감지 실패 시 main
export SWARM_BASE_DIR="agent/swarm-worktrees"  # 기본 값
```

---

## 4) 생성되는 로그/폴더

- 로그: `agent/logs/`
- Swarm worktree: `agent/swarm-worktrees/<role>/`
- 캐시(샌드박스 친화): `.gradle-user-home/`, `.android-home/`

---

## 5) 안전/통제

- `.codex/config.toml`은 기본적으로 **approval_policy = "never"** (완전 자동)입니다.  
  필요하면 `on-request` 등으로 바꾸세요.
- Antigravity는 denylist/allowlist를 통해 위험 명령/도메인을 제한하도록 샘플을 제공합니다.

---

## 6) 파일 구조

- `agent/` : PRD/Backlog/Acceptance + Codex 프롬프트
- `.codex/` : Codex 설정 + 역할별 에이전트 설정
- `.agents/` : Antigravity 규칙/워크플로 템플릿
- `scripts/` : one-liner 실행 스크립트

---

## 7) 흔한 문제

### `./gradlew`가 없어요
- 이 키트는 **Android 프로젝트 위에 얹는 용도**입니다.  
  Android Studio로 프로젝트를 생성한 뒤 루트에 풀어주세요.

### Swarm가 worktree를 못 만들어요
- git repo가 아니거나, 브랜치가 이상한 상태(detached HEAD)일 수 있습니다.  
  `git status` 확인 후 `git checkout -b main` 같은 방식으로 정상 브랜치를 만든 뒤 실행하세요.

---

## 8) 원하는 동작으로 커스터마이즈
- `agent/BACKLOG.md` : 스프린트 단위 태스크를 더 잘게 쪼개면 자동 루프 성공률이 올라갑니다.
- `agent/prompts/*.md` : Codex에게 “어떤 방식으로 작업할지”를 강하게 유도합니다.
