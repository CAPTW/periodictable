# AGENTS.md (Antigravity / Codex 공통 안내)

## 실행

### Unix (Bash)
- Ralph Loop: `bash scripts/ralph-loop.sh`
- Agent Swarm: `bash scripts/swarm-sprint.sh`
- Verify: `bash scripts/verify.sh`

### Windows (PowerShell)
- Ralph Loop: `.\scripts\ralph-loop.ps1`
- Ralph Loop (드라이런): `.\scripts\ralph-loop.ps1 -DryRun`
- Verify: `.\scripts\verify.ps1`
- 사전조건 확인: `.\scripts\check-prereqs.ps1`

### Gradle Wrapper
- Unix: `./gradlew <task>`
- Windows: `.\gradlew.bat <task>`
- 빌드 예시: `.\gradlew.bat assembleDebug`

## 규칙(요약)
- Kotlin + Jetpack Compose + Material 3
- Offline-first (원소 DB/리소스는 앱 번들)
- 필수: 빠른 검색(이름/기호 + 속성 필터), 원소 상세 물성/핵특성, 메모, 속성 시각화, 몰질량 계산기, 용어집
- UI/UX: Stitch MCP 기반 산출물을 참고하여 Compose로 구현

## 환경 요구사항
- **JDK 17+** 필수 (AGP 8.3.2 + Kotlin 2.0)
- `JAVA_HOME`을 JDK 17 경로로 설정
- Gradle Wrapper가 자동으로 Gradle 8.7을 다운로드함

## 로그
- `agent/logs/`에 verify 로그와 Codex 출력이 저장됨
