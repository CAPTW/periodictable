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

## 환경 요구사항 (검증됨: 2026-06-27)
- **빌드 JDK: 21** — Gradle 데몬 JVM은 `gradle.properties`의 `org.gradle.java.home`로
  Android Studio 번들 JBR(JDK 21)에 고정됨. 시스템 JDK(예: 25)는 빌드에 사용되지 않음.
  `verify.ps1`도 `JAVA_HOME`을 해당 JBR 경로(`C:\Program Files\Android\Android Studio\jbr`)로 강제 설정함.
- 툴체인: Gradle **9.0**, AGP **8.13.0**, Kotlin **2.1.10** (KSP 2.1.10-1.0.29).
  `kotlin.jvmToolchain(21)` + `jvmTarget = JVM_17` (source/target 호환성 17).
- SDK: `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`. Android SDK 경로는 `local.properties`(`sdk.dir`)에서 읽음.
- Gradle Wrapper가 Gradle 9.0을 자동 다운로드(또는 캐시 사용)함. 오프라인 빌드는 Gradle 배포판/의존성 캐시 필요.
- 검증 게이트: `.\scripts\verify.ps1` (Windows) / `bash scripts/verify.sh` =
  `validate_assets.py` + `gradlew --no-daemon lintRelease testReleaseUnitTest assembleRelease`.

## 로그
- `agent/logs/`에 verify 로그와 Codex 출력이 저장됨
