# Mini-game Phase 1 Status — Build Gate (Phase 1D)

> 2026-06-09 · Phase 1A(engine) → 1B(ViewModel/UI) → 1C(Navigation) → **1D(Build Gate)** 까지의 검증 상태 인계 문서.

## 결론

신규 미니게임 코드(model/engine/ViewModel/Compose/Navigation)에서 **이 환경에서 검증 가능한 모든 레이어가 통과**했고, **Phase 1D에서 소스 수정은 필요하지 않았다**. 남은 한 가지는 Compose UI + Navigation 레이어의 실제 Android 컴파일인데, 이 샌드박스의 toolchain 제약으로 실행할 수 없어 사용자 Windows/Android Studio 환경에서 `./gradlew assembleDebug` 1회로 마무리하면 된다.

## 이 샌드박스에서 Android 빌드가 불가능한 이유

- `local.properties` 의 `sdk.dir = C:/Users/USER/AppData/Local/Android/Sdk` — Windows 경로(Linux 샌드박스에 없음).
- `gradle.properties` 의 `org.gradle.java.home = C:/Program Files/Android/Android Studio/jbr` — Windows JBR 경로.
- 샌드박스 JDK 는 11 뿐(프로젝트는 17/21 요구), Android SDK 없음, `gradlew` 는 CRLF.

즉 빌드는 원래 Windows/Android Studio 에서 돌도록 구성돼 있다. (위 설정은 수정하지 않았다 — 사용자 환경에서 정상이므로.)

## 검증 결과 (이 환경에서 수행)

| 레이어 | 방법 | 결과 |
|---|---|---|
| engine + model (순수 Kotlin) | kotlinc 2.1.10 직접 컴파일 | ✅ 성공 |
| RecipeBookTest + BoardEngineTest | JUnit 실행 | ✅ **15/15 OK** |
| MoleculeGameViewModel | 실제 `androidx.lifecycle.ViewModel`/`viewModelScope` + dagger `@HiltViewModel` + `javax.inject` + coroutines + 앱 도메인 소스에 대해 타입체크 | ✅ 컴파일 성공 |
| MoleculeGameScreen (Compose) | 정적 감사(import/시그니처/의존성 대조) | ⚠️ 감사만 — Compose 컴파일러 플러그인 필요로 미컴파일 |
| Screen.kt / ChemTableNavHost.kt / VisualizationScreen.kt | 정적 감사(라우트 패턴/콜백/하단바 분기) | ⚠️ 감사만 — Android 컴파일 필요 |

## 정적 감사 체크리스트 (Compose/Navigation)

- `Screen.MoleculeGame` = `game/molecule?startElement={atomicNumber}`, `createRoute(Int? = null)` → 인자 없으면 `game/molecule`. ✅ 기존 sealed `Screen` 패턴과 일치.
- NavHost: `navArgument("atomicNumber"){ type=IntType; defaultValue=-1 }` → 인자 없이도 매칭. ✅
- 하단바 숨김: `currentRoute?.startsWith("game/") != true`. `destination.route` 는 채워진 URL 이 아니라 **라우트 패턴**을 반환하므로 게임 화면에서 항상 `game/` 시작 → 숨김. ✅
- `VisualizationScreen(onPlayMiniGame: () -> Unit = {}, ...)` 기본값 보유 → 기존 호출부 비파괴. NavHost 는 콜백 전달. 기존 FilterChip/Canvas UI 유지. ✅
- `MoleculeGameScreen(onExit, viewModel=hiltViewModel())` → stateless `MoleculeGameContent` 분리, `@Preview` 는 Hilt 없이 `MoleculeGameContent` 직접 렌더. ✅
- imports: `BackHandler`(activity-compose 1.10), `detectDragGestures`/`pointerInput`/`Offset`, `collectAsState`, `hiltViewModel`(hilt-navigation-compose 1.2), `@Preview`(ui-tooling-preview), `BorderStroke`/`background` 모두 존재 dependency 로 충족. ✅

## 사용자 환경에서 실행할 명령 (Windows / Android Studio)

```
./gradlew :app:testDebugUnitTest   # 엔진 unit test (15)
./gradlew :app:compileDebugKotlin  # 전체 Kotlin 컴파일 (Compose 포함)
./gradlew :app:assembleDebug       # APK 빌드
```

## 런타임 smoke 체크리스트 (에뮬레이터/기기)

시각화 탭 진입 → "분자 만들기" 카드 표시 → 탭 시 게임 진입(하단바 미표시) → Intro "시작" → Playing HUD + 4×4 보드 → swipe 1회 크래시 없음 → 시스템 back → Pause overlay → "나가기" → 시각화 탭 복귀 → 기존 5탭 정상 이동.

## Phase 2 로 미룬 항목

원소 상세 → `startElement` 소비/보장(라우트 인자만 준비됨), 계산기 프리필 딥링크, 사전 딥링크, 도감/최고점수 persistence, 난이도/모드 확장, 시각 피드백(MergeSuccess/Rejected), 튜토리얼 단계별 하이라이트.
