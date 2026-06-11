# Mini-game Phase 0 Spec

> **Status:** Pre-implementation design spec (no code). Defines everything that must be locked before Phase 1 coding begins.
> **Date:** 2026-06-09
> **Parent doc:** `docs/minigame-placement-audit-plan.md`
> **Read before implementing:** `navigation/Screen.kt`, `navigation/ChemTableNavHost.kt`, `feature/visualization/VisualizationScreen.kt`, `core/model/*`, `core/util/FormulaParser.kt`, `core/util/MolarMassCalculator.kt`, `core/designsystem/component/ElementCell.kt`

---

## 1. Product Decision

**최종 배치 결정:** 미니게임은 **"시각화" 탭을 "탐구(Lab)" 허브로 확장**하여 그 안의 진입 카드로 노출하고, **실제 플레이는 하단바 없는 별도 풀스크린 라우트**로 띄운다.

**왜 Lab/Visualization 허브가 1순위인가**

- 하단 탭은 이미 5개(주기율표·검색·시각화·계산기·사전)로 Material 권장 상한이며, 한글 라벨이라 폭 여유가 없다. 탭 수를 5로 유지해야 IA가 깨지지 않는다.
- `VisualizationScreen`은 단일 `Column` + 상단 `FilterChip` 행 + `when(mode)` 패널 구조로, 콘텐츠 카드를 상단에 추가하기 쉽고 현재 가장 단순·확장 여지가 큰 탭이다.
- 시각화·게임·(향후) 인터랙티브 레슨은 "탐구/실험" 개념으로 묶이므로 사용자 멘탈 모델이 자연스럽다.
- 게임의 swipe 제스처는 주기율표의 pan/zoom(`detectTransformGestures`)과 충돌하므로 메인 탭 인라인은 불가. 허브 카드(발견) + 별도 라우트(플레이) 분리가 정답이다.

**왜 독립 하단 탭은 지금 피하는가**

- 6번째 탭은 터치 타깃 폭을 좁히고 한글 라벨 가독성을 떨어뜨린다.
- 검증되지 않은 기능에 영구적 IA 비용을 선지불하게 된다.
- 승격은 되돌리기 어렵다. MVP 지표가 좋으면 Phase 3에서 "탐구" 정식 탭으로 승격하는 편이 안전하다.

## 2. Navigation Design

**예상 route 목록 (`Screen.kt`에 추가 예정 — 본 문서에서는 정의만, 코드 미수정)**

| Screen object | route | 설명 |
|---|---|---|
| `Lab` (또는 기존 `Visualization` 재사용) | `visualization` | Lab 허브. 시각화 + 게임 진입 카드. 하단바 표시. |
| `MoleculeGame` | `game/molecule?startElement={atomicNumber}` | 게임 플레이(풀스크린). 하단바 숨김. `startElement` optional(원소 상세에서 진입 시). |

**Lab 허브 route:** 기존 `Screen.Visualization`("visualization")을 그대로 사용하고 화면 내용만 허브화(카드 추가). 새 라우트 문자열 불필요 → 변경 최소.

**Mini-game play route:** `game/molecule` 신규. optional query `startElement`(IntType, defaultValue=-1). 진입 출처 무관하게 동일 라우트 사용.

**결과 화면: 별도 route인가 내부 state인가 → 내부 state(overlay)로 결정.**

- 이유: 결과는 게임 세션 종료 직후의 모달 성격이며, 백스택에 별도 엔트리를 쌓으면 back 처리가 복잡해진다.
- 구현: `GameUiState.phase`(`Intro`/`Playing`/`Paused`/`Result`)로 표현. Result는 `MoleculeGameScreen` 위의 overlay composable. Pause도 동일하게 overlay.
- 결과에서 "다시하기"는 state 리셋, "나가기"는 `popBackStack()`.

**하단바 숨김 조건**

- `ChemTableNavHost`의 `Scaffold(bottomBar = ...)`는 현재 무조건 NavigationBar를 그린다. 게임 라우트에서만 숨기려면 `currentRoute`가 `game/...`로 시작하는지로 분기:
  - `val showBottomBar = currentRoute?.startsWith("game/") != true` → `bottomBar = { if (showBottomBar) NavigationBar { ... } }`.
- 게임 화면 composable은 `innerPadding`을 받되 풀스크린으로 채우고, 시스템 인셋만 고려한다.

**back navigation 정책**

- `Intro` 단계에서 back → `popBackStack()`(허브로 복귀).
- `Playing` 단계에서 back(시스템 제스처/버튼) → 즉시 종료가 아니라 **`Paused` overlay 표시**(실수 종료 방지). `BackHandler(enabled = phase == Playing)`로 가로채기.
- `Paused`에서 back → `Playing`으로 복귀(resume).
- `Result`에서 back → `popBackStack()`(허브로 복귀).
- 딥링크(원소 상세→게임)로 진입한 경우에도 back은 동일하게 허브/이전 화면으로 자연 복귀(단일 `game/molecule` 엔트리만 쌓임).

## 3. UX Flow

**전체 경로:** Lab 허브 카드 → (최초 1회) 튜토리얼 → 게임 시작 → 플레이 → 결과 → 계산기/사전/원소 상세 연결.

**최초 사용자 flow**

1. 시각화(탐구) 탭 진입 → 상단 "분자 만들기" 카드 발견.
2. 카드의 "플레이" 탭 → `game/molecule` 진입, `phase = Intro`.
3. 최초 진입이므로 Intro 위에 **튜토리얼 코치마크 3스텝** 자동 표시(중력/스와이프/합성). "건너뛰기" 가능.
4. 튜토리얼 종료 → "시작" → `phase = Playing`.
5. 스와이프로 블록 이동·합성 → 미션 달성 또는 game over → `phase = Result`.
6. 결과 overlay에서 점수·만든 분자 목록 확인 → 분자 탭 시 연결 메뉴(계산기/사전/원소 상세).

**재방문 사용자 flow**

1. 허브 카드 → "플레이" → `phase = Intro`(또는 설정에 따라 곧장 Playing).
2. 튜토리얼은 자동 표시하지 않음. Intro 화면의 "?" 버튼으로만 재호출.
3. 이후 동일.

> 참고: "튜토리얼을 봤는가" 플래그는 MVP에서 in-memory(세션) 또는 추후 DataStore. MVP는 ViewModel 보유 플래그로 충분(앱 재시작 시 다시 보일 수 있음 — 수용 가능).

**연결(딥링크) 동작**

- 결과/플레이 중 분자 블록 탭 → bottom sheet 또는 메뉴: ① "계산기에서 보기" → `Calculator` 라우트로 이동하며 식 프리필(Phase 2), ② "사전에서 보기" → 관련 `GlossaryDetail`(있으면), ③ "구성 원소 보기" → `ElementDetail`.
- 원소 상세에서 "이 원소로 시작" 진입 시 `startElement` 전달 → 보드 초기 스폰에 해당 원소 1개 보장.

## 4. MVP Screen Specification

**(a) Lab entry card** — `VisualizationScreen` 상단 `Text("속성 시각화")` 위 또는 아래에 1개 카드 추가.

- 구성: 제목("분자 만들기"), 한 줄 설명("원소를 모아 분자를 합성하는 미니게임"), "플레이" 버튼.
- 동작: 클릭 → `onPlayMiniGame()` 콜백 → NavHost가 `game/molecule`로 이동.
- 기존 시각화 UI(FilterChip/패널)는 그대로 아래 유지.

**(b) MiniGameIntro (phase = Intro)**

- 게임 제목, 규칙 요약 3줄, "시작" primary 버튼, "?"(튜토리얼 재생) 버튼, 난이도 선택(MVP는 단일/초급 고정 가능).
- 최초 진입 시 위에 튜토리얼 코치마크 자동 표시.

**(c) MiniGamePlayScreen (phase = Playing)**

- 상단 HUD: 점수, 목표 분자 칩(예: "H₂O ×2"), 콤보 카운터, 남은 이동/시간(모드에 따라), 일시정지 버튼.
- 중앙: 4×4 보드(`LazyVerticalGrid` 또는 커스텀 `Layout`; 셀 간 1~4dp gap, 기존 그리드 톤과 일치).
- 블록: `ElementBlock`은 `ElementCell` 재사용(`isCompact=false`, click/long-click은 게임용으로 매핑), `MoleculeBlock`은 전용 composable.
- 하단 보조: 다음 등장 원소 미리보기(옵션).
- 입력: 보드 영역 `pointerInput`으로 swipe(상/하/좌/우) 감지.

**(d) Pause overlay (phase = Paused)**

- 반투명 배경 + 카드: "재개", "다시 시작", "나가기", (옵션) "튜토리얼 다시 보기".

**(e) Result overlay (phase = Result)**

- 결과 요약: 최종 점수, 달성 여부(미션 성공/실패), 만든 분자 목록(각 항목 탭 → 연결 메뉴).
- 버튼: "다시 하기"(state 리셋), "나가기"(popBackStack).

**(f) Tutorial coach marks**

- 3스텝 순차: ① "무거운 원소는 아래로, 가벼운 원소는 위로 정렬됩니다(원자량 기준)." ② "상하좌우로 스와이프해 블록을 모으세요." ③ "올바른 조합이 만나면 분자가 됩니다. 예: H + H → H₂."
- 각 스텝: 짧은 문구 + 하이라이트 + "다음"/"건너뛰기". 마지막 스텝 "시작".

## 5. Board Rule Specification

**4×4 board**

- 16칸 고정. 행 인덱스 0(top)~3(bottom), 열 인덱스 0(left)~3(right).
- 좌표계: `row`가 클수록 화면 아래쪽(중력 방향). massScore가 큰 블록일수록 큰 `row`(아래)로 가려는 성향.

**block position model**

- 각 블록은 `(row, col)` 정수 좌표 + 안정 id를 가진다. 빈 칸은 `null`.
- 보드는 `List<List<BoardCell>>`(4×4) 또는 `Map<Position, GameBlock>`. 애니메이션을 위해 블록 id 유지가 중요.

**swipe input 처리**

- `pointerInput` + drag 누적 → 임계값(예: 24dp) 초과 시 주축(가로/세로) 판정 → `Direction(UP/DOWN/LEFT/RIGHT)` 1회 emit.
- 한 swipe = 1 move. move 처리 중에는 추가 입력 무시(애니메이션 완료까지 lock).

**block movement rule (2048류 슬라이드)**

1. swipe 방향으로 각 라인(행 또는 열)의 블록을 해당 방향 끝으로 압착(빈칸 제거).
2. 압착 과정에서 인접하게 만난 두 블록이 **합성 가능(레시피 충족)**하면 1개 분자 블록으로 병합(merge). 한 move에서 같은 블록은 1회만 병합.
3. 합성 불가한 두 블록은 병합되지 않고 인접 정지.

**gravity/massScore rule**

- `massScore = molarMass`(ElementBlock은 원소 원자량, MoleculeBlock은 구성 원소 합).
- swipe 후처리로 **열(column) 단위 "정렬 중력" 1틱** 적용: 각 열에서 블록을 `massScore` 내림차순으로 아래(row 큰 쪽)부터 재배치. 결정적이고 단순.
- 좌우 swipe도 이동 후 동일 중력 1틱을 적용해 "무거운 게 가라앉는" 일관성 유지(설계 확정 필요: MVP는 매 move 후 항상 중력 1틱 적용).

**merge/composition rule**

- 두 블록이 만나 합성되려면 레시피 화이트리스트에 정확히 매칭되어야 함(§6). 매칭 단위는 **원소/분자 multiset**(파서 문자열이 아님).
- 병합 결과는 단일 MoleculeBlock. 그 자리에 생성, massScore 재계산.
- 한 move 내 다중 병합 허용하되, 각 블록은 1회만.

**spawn rule**

- 게임 시작 시 보드에 N개(예: 2~4) 랜덤 원소 스폰. `startElement`가 있으면 그중 1칸 보장.
- 매 유효 move(블록이 실제로 이동/병합된 경우) 후 빈칸 1개에 새 원소 1개 스폰.
- 등장 원소 풀은 난이도로 제한(§Chemistry/§8). 가중치는 레시피 달성 가능하도록 조정(예: H, O 빈도↑).

**game over rule**

- 보드가 가득 차고 어떤 방향으로도 이동/병합이 불가능하면 game over → `Result(실패)`.
- mission 모드: 목표 분자 달성 시 즉시 `Result(성공)`.
- (옵션) move/time 제한 모드: 한도 소진 시 `Result`.

## 6. Chemistry Rule Specification

**자유 결합 금지** — 임의 원소를 붙여 임의 분자를 만들 수 없다. 합성은 사전 정의 레시피에만 허용한다. (오개념·가짜 분자 방지.)

**whitelist recipe 기반** — 레시피는 입력 multiset → 결과 분자(formula)로 선언. 매칭은 multiset 동등성으로 판정.

**MVP recipe 목록 (2-입력 단계 합성 우선)**

| 입력(multiset) | 결과 formula | 비고 |
|---|---|---|
| {H, H} | H₂ | 동핵 이원자분자 |
| {O, O} | O₂ | |
| {N, N} | N₂ | |
| {H₂, O} | H₂O | 안전한 단계 경로(중간체 OH 회피) |
| {O, O} → O₂, {C, O₂} | CO₂ | 2단계 |
| {Na, Cl} | NaCl | 이온결합 예시 |
| {N, H} … | NH₃ | **다단계 필요** → MVP는 NH₃를 "최종 레시피"로 두되 도달 경로가 복잡하면 Phase 1 제외 후 Phase 3 편입 검토 |

> 결정 사항: **MVP 확정 6종 = H₂, O₂, N₂, H₂O, CO₂, NaCl.** NH₃는 3개 H가 필요해 2-입력 단계로 만들기 번거로우므로 MVP 제외, 난이도 확장 시 재도입.

**recipe matching 방식**

- 각 블록은 "구성 원소 multiset"을 안다(ElementBlock = {symbol×1}, MoleculeBlock = 구성 원소 카운트).
- 두 블록이 만나면 두 multiset을 합쳐 레시피 테이블에서 정확 매칭 검색. 매칭되면 결과 분자 생성.
- **주의:** 기존 `FormulaParser`는 `+`/`→`를 구분자로 보고 모든 원소를 평면 합산하므로(예: "H + H" → H:2) **레시피 매칭에는 사용하지 않는다.** 파서/`MolarMassCalculator`는 오직 **결과 분자량 계산**용으로만 재사용(`calculate("H2O", elements)` → 18.015).
- 분자량은 `MolarMassCalculator.calculate(formula, elements)`로 산출하고, 그 값으로 massScore·점수 계산.

**오개념 방지 문구(게임 내 표기 고정)**

- 중력 설명은 항상 **"원자량(질량) 기준"**이라고 명시(밀도 아님).
- 잘못된 조합은 벌점 없이 "이 조합은 결합하지 않아요"만 표시(임의 결합이 가능하다는 인상 차단).
- 화학량론 정확성: H₂O는 H 2개 필요 등 레시피 계수를 정확히 반영. 결과 분자 탭 시 계산기/사전으로 보내 정식 정보로 확인 유도.

**밀도는 MVP 제외(고급 모드로 미루는 이유)**

- `Element`의 밀도 관련 필드(`liquidDensity`)는 nullable이고 고체/액체/기체가 혼재해 일관 비교가 어렵다(`StateOfMatter`로 상태가 갈림).
- 초급자에게 "밀도"는 "원자량=무게"보다 직관성이 낮아 오개념을 유발한다.
- 따라서 MVP 중력은 `molarMass` 단일 기준. 고급 모드에서 "분자량 동률 시 밀도 2차 정렬" 정도로 한정 도입.

## 7. Data Model Draft

> 아래는 **pseudo-code 설계 초안**이다. 실제 Kotlin 파일은 생성하지 않는다. 패키지 위치는 §8 참조.

```kotlin
// 보드에 올라가는 블록
sealed interface GameBlock {
    val id: Long                  // 안정 id(애니메이션용)
    val massScore: Double         // = molarMass
    val composition: Map<String, Int>  // 원소 multiset (레시피 매칭 키)
    val label: String             // 화면 표기(기호 또는 화학식)
}

data class ElementBlock(
    override val id: Long,
    val atomicNumber: Int,
    val symbol: String,
    val nameKo: String,
    val molarMass: Double,
    val category: ElementCategory   // 색상 매핑(ElementCell 재사용)
) : GameBlock {
    override val massScore get() = molarMass
    override val composition get() = mapOf(symbol to 1)
    override val label get() = symbol
}

data class MoleculeBlock(
    override val id: Long,
    val formula: String,            // 예: "H2O"
    val molarMass: Double,          // MolarMassCalculator 결과
    override val composition: Map<String, Int>
) : GameBlock {
    override val massScore get() = molarMass
    override val label get() = formula
}

// 보드 좌표/셀
data class Position(val row: Int, val col: Int)   // row 0=top..3=bottom
data class BoardCell(val position: Position, val block: GameBlock?)

data class BoardState(
    val size: Int = 4,
    val cells: List<BoardCell>      // 16개 (또는 Map<Position, GameBlock?>)
) {
    fun blockAt(row: Int, col: Int): GameBlock?
}

// 레시피
data class Recipe(
    val inputs: Map<String, Int>,   // 합쳐진 원소 multiset (예: {H:2, O:1})
    val productFormula: String,     // "H2O"
    val displayKo: String           // "물"
)

// UI 상태
enum class GamePhase { INTRO, PLAYING, PAUSED, RESULT }

data class GameUiState(
    val phase: GamePhase = GamePhase.INTRO,
    val board: BoardState,
    val score: Int = 0,
    val combo: Int = 0,
    val missionTarget: MissionTarget?,   // 예: H2O ×2
    val movesLeft: Int? = null,          // 모드에 따라 null
    val discoveredMolecules: List<String> = emptyList(),
    val showTutorial: Boolean = false,
    val difficulty: Difficulty = Difficulty.BEGINNER
)

data class MissionTarget(val formula: String, val count: Int, val progress: Int)
enum class Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

// 입력 이벤트(View → ViewModel)
sealed interface GameEvent {
    data class Swipe(val direction: Direction) : GameEvent
    data object Pause : GameEvent
    data object Resume : GameEvent
    data object Restart : GameEvent
    data object Exit : GameEvent
    data object StartGame : GameEvent
    data object SkipTutorial : GameEvent
    data class BlockTapped(val blockId: Long) : GameEvent
}
enum class Direction { UP, DOWN, LEFT, RIGHT }

// 일회성 효과(ViewModel → View)
sealed interface GameEffect {
    data class MergeSuccess(val formula: String, val gained: Int) : GameEffect
    data object MergeRejected : GameEffect          // 흔들림 피드백
    data class NavigateToCalculator(val formula: String) : GameEffect
    data class NavigateToGlossary(val termId: String) : GameEffect
    data class NavigateToElement(val atomicNumber: Int) : GameEffect
}

// 세션 결과
data class GameResult(
    val finalScore: Int,
    val success: Boolean,
    val moleculesMade: List<String>,
    val maxCombo: Int
)
```

## 8. Architecture Plan

**`feature/minigame` 패키지 구조 제안**

```
feature/minigame/
  MoleculeGameScreen.kt        // Compose: phase별 화면 분기, overlay
  MoleculeGameViewModel.kt     // @HiltViewModel, StateFlow<GameUiState>
  component/
    ElementBlockView.kt        // ElementCell 재사용 래퍼
    MoleculeBlockView.kt
    GameBoardView.kt           // 4×4 레이아웃 + swipe pointerInput
    HudBar.kt, PauseOverlay.kt, ResultOverlay.kt, TutorialCoachMarks.kt, LabEntryCard.kt(또는 visualization 내부)
  model/                       // §7 data models
  engine/
    BoardEngine.kt             // 순수 Kotlin: move/gravity/merge/spawn/gameover
    RecipeBook.kt              // 레시피 테이블 + multiset 매칭
```

**engine 순수 Kotlin 분리 계획**

- `engine/`은 Android·Compose·Hilt 의존성 0. 입력 `(BoardState, Direction)` → 출력 `(BoardState, mergedFormulas, gainedScore)`인 순수 함수 집합.
- gravity/merge/spawn/gameover 판정을 모두 순수 함수로 → 단위테스트 100% 가능(§9).
- 랜덤 스폰은 `Random` 시드를 주입받아 테스트에서 결정적으로.

**ViewModel StateFlow 구조**

- `private val _uiState = MutableStateFlow(GameUiState(...))`, `val uiState: StateFlow<GameUiState>`.
- `fun onEvent(e: GameEvent)` 단일 진입점에서 engine 호출 → 새 state 방출.
- 일회성 효과는 `Channel<GameEffect>` → `Flow`로 노출(토스트/흔들림/네비게이션).
- 원소 데이터는 init에서 `GetElementsUseCase()` 수집 후 스폰 풀 구성.

**기존 `GetElementsUseCase` 재사용**

- `getElementsUseCase()` (Flow<List<Element>>) 를 주입받아 원소 마스터(원자량·색·기호) 확보. 새 원소 데이터 소스 불필요.

**기존 `FormulaParser` / `MolarMassCalculator` 재사용**

- 분자 생성 시 결과 formula의 분자량을 `MolarMassCalculator.calculate(formula, elements)`로 계산(이미 Hilt 주입 가능, `@Inject` 생성자).
- 레시피 **매칭**에는 사용하지 않음(파서는 `+` 병합 특성상 부적합). 매칭은 `RecipeBook`의 multiset 비교로 독립 구현.

**신규 DB를 MVP에서 만들지 않는 이유**

- MVP 게임 상태는 세션 메모리(ViewModel)로 충분. 최고점수·도감 등 영속 데이터가 없다.
- Room 엔티티/마이그레이션 추가는 `ChemTableDatabase` 버전 변경·마이그레이션 리스크를 동반 → 검증 전 회피.
- 영속화가 필요해지는 Phase 3에서 작은 `GameRecordEntity` 또는 DataStore를 `RepositoryModule`의 `@Binds` 패턴으로 추가.

## 9. Test Plan

**engine unit test 목록 (순수 Kotlin, JUnit)**

- *recipe matching test:* {H,H}→H₂, {O,O}→O₂, {N,N}→N₂, {H₂,O}→H₂O, {C,O₂}→CO₂, {Na,Cl}→NaCl 각각 정확 매칭. 비레시피 multiset(예: {H,Na})은 null 반환.
- *gravity sorting test:* 한 열에 [가벼움(top), 무거움(bottom)] 무작위 배치 → 중력 1틱 후 massScore 내림차순(무거운 게 row 큰 쪽)인지.
- *invalid recipe test:* 합성 불가 두 블록이 인접해도 병합되지 않고 둘 다 보존되는지. (오개념 방지 핵심.)
- *board movement test:* 각 방향 swipe에 대해 압착 결과 좌표가 기대값과 일치. 한 move에서 동일 블록 1회만 병합. 이동이 없으면 spawn 미발생.
- *merge mass test:* 병합 후 MoleculeBlock.massScore = 구성 원소 분자량 합(`MolarMassCalculator` 결과)과 일치.
- *spawn test:* 유효 move 후 빈칸 1개에만 새 블록 생성, 시드 고정 시 결정적.
- *game over test:* 보드 풀 + 모든 방향 이동 불가 → game over true. 한 방향이라도 가능하면 false.
- *startElement test:* startElement 지정 시 초기 보드에 해당 원소 1개 이상 보장.

**UI smoke test 후보 (Compose, androidTest)**

- 허브 카드 "플레이" 클릭 → 게임 라우트 진입(하단바 미표시) 확인.
- Intro "시작" → Playing HUD 노출.
- 보드 swipe 1회 → recomposition 정상(크래시 없음).
- 결과 overlay 노출 시 "다시 하기"/"나가기" 동작.

## 10. Phase 1 Implementation Checklist

> 각 task는 작고 독립적으로. 코드 작성은 Phase 1에서 수행(본 문서는 작성하지 않음).

- [ ] **T1. model 패키지 생성** — §7 data class/sealed interface 작성.
  - *AC:* 컴파일 통과, 외부 의존성 없음.
- [ ] **T2. RecipeBook + multiset 매칭** — MVP 6종 레시피 테이블 + `match(a, b): Recipe?`.
  - *AC:* recipe matching test 전부 green, 비레시피는 null.
- [ ] **T3. BoardEngine 이동/압착** — 4방향 슬라이드 + 압착.
  - *AC:* board movement test green, 이동 없으면 변화 없음 보고.
- [ ] **T4. BoardEngine 중력** — 열 단위 massScore 내림차순 1틱.
  - *AC:* gravity sorting test green.
- [ ] **T5. BoardEngine 병합** — 인접 합성 + massScore 재계산(MolarMassCalculator 주입).
  - *AC:* invalid recipe test + merge mass test green, 한 move 1회 병합.
- [ ] **T6. spawn + game over** — 유효 move 후 스폰, 종료 판정.
  - *AC:* spawn/game over test green, 시드 고정 결정적.
- [ ] **T7. ViewModel** — `@HiltViewModel`, StateFlow + GameEvent + GameEffect Channel, GetElementsUseCase 주입.
  - *AC:* onEvent(Swipe)가 engine 호출→state 갱신, 효과 방출.
- [ ] **T8. GameBoardView + swipe** — 4×4 레이아웃 + pointerInput 방향 감지.
  - *AC:* 4방향 swipe가 정확히 1 move emit, 애니메이션 중 입력 lock.
- [ ] **T9. ElementBlockView / MoleculeBlockView** — ElementCell 재사용 + 분자 블록.
  - *AC:* 원소/분자 블록 시각 구분, massScore/라벨 표기.
- [ ] **T10. HUD + Pause/Result overlay + phase 분기** — GamePhase별 화면.
  - *AC:* Intro/Playing/Paused/Result 전환, BackHandler 정책(§2) 동작.
- [ ] **T11. Tutorial coach marks** — 최초 1회 자동 + "?" 재호출.
  - *AC:* 최초 진입 자동 표시, 재방문 미표시, 건너뛰기 동작.
- [ ] **T12. Lab entry card** — VisualizationScreen 상단 카드 + onPlay 콜백.
  - *AC:* 카드 클릭 → game 라우트, 기존 시각화 UI 무영향.
- [ ] **T13. Navigation 배선** — Screen.MoleculeGame 추가, NavHost composable 등록, 하단바 숨김 분기.
  - *AC:* 허브→게임 진입, 게임에서 하단바 숨김, back 정책 동작, 기존 5탭 회귀 없음.
- [ ] **T14. UI smoke test** — §9 후보 4종.
  - *AC:* 진입·시작·swipe·결과 흐름 크래시 없음.

---

## Notes

- **코드 수정 없음:** 이 작업에서 기존 Kotlin/Compose/Room/Hilt 소스, Gradle, Manifest, 리소스, DB/데이터 파일을 일절 수정하지 않았다. 신규 Kotlin 파일도 생성하지 않았다.
- **생성/수정한 문서 경로:** `docs/minigame-phase-0-spec.md` (신규 생성, 단일 문서).
- **이번 작업에서 읽은(read-only) 파일:** `docs/minigame-placement-audit-plan.md`, `navigation/Screen.kt`, `navigation/ChemTableNavHost.kt`, `feature/visualization/VisualizationScreen.kt`, `core/model/*`(Element, ElementCategory, MolarMassModels, GlossaryTerm, ElementProperty, StateOfMatter 등), `core/util/FormulaParser.kt`, `core/util/MolarMassCalculator.kt`, `core/designsystem/component/ElementCell.kt`. 실행 명령은 `cat`, `sed -n`(read-only)뿐이다.
