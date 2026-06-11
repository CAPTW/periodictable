# Mini-game Placement Audit & Plan

> **Status:** Approved planning document (no code changes). Audit + placement plan for adding a chemistry mini-game to the existing Android periodic table app.
> **Date:** 2026-06-09

## Summary (Conclusion)

**기존 시각화 탭을 탐구(Lab) 허브로 확장하고, 미니게임은 하단바 없는 별도 풀스크린 라우트로 배치한다.**

새 하단 탭을 추가하지 않는다. 현재 하단바는 이미 5탭(주기율표·검색·시각화·계산기·사전)으로 가득 차 있어 6번째 탭은 터치 타깃·가독성을 해친다. 대신 가장 단순·확장 여지가 큰 "시각화" 탭을 "탐구(Lab)" 허브로 승격해 그 안에 미니게임 진입 카드를 두고, 실제 게임 플레이는 제스처·하단바 충돌을 피하기 위해 하단바를 숨긴 별도 풀스크린 라우트(`game/molecule`)로 띄운다. 원소 상세·계산기는 보조 진입로로 함께 두고, 사전 Interactive Lesson·독립 탭 승격은 사용지표 검증 후 장기 확장으로 미룬다.

---

## 1. Repo Audit Summary

**아키텍처:** 단일 모듈(`app`) 기반의 Clean Architecture + Jetpack Compose + Hilt + Room. 패키지 `com.chemtable.interactive` 아래 레이어가 명확히 분리되어 있다.

- `core/model` — 도메인 모델 (`Element`, `ElementCategory`, `GlossaryTerm`, `MolarMassModels`, `Isotope` 등). `Element.molarMass`가 **non-null Double**로 항상 존재 → 게임의 gravity/massScore에 바로 사용 가능.
- `core/util` — `FormulaParser`, `MolarMassCalculator`. **이미 화학식 파싱과 몰질량 계산 로직이 존재** → 분자 블록 질량 계산·검증에 재사용 가능.
- `core/designsystem` — `theme/`(Color, Typography, Spacing, Shape, Motion), `component/`(`ElementCell`, `ChemSearchBar`). `ElementCell`은 카테고리별 색상 매핑 + compact 모드 + `combinedClickable`(click/long-click) 지원 → 보드 블록 UI 베이스로 거의 그대로 재활용 가능.
- `core/database` — Room (DAO 5종: Element/Glossary/Isotope/CalcHistory/Note, entity, converter, Migrations).
- `data` — `mapper`, `prepopulate`(JSON seeder: `assets/elements.json`, `glossary.json`), `repository`(Impl 5종).
- `domain` — `repository`(인터페이스 5종), `usecase`(15개, 예: `GetElementsUseCase`, `CalculateMolarMassUseCase`, `GetGlossaryUseCase`).
- `feature` — 화면 단위 패키지: `periodictable`, `search`, `visualization`, `calculator`, `glossary`, `elementdetail`, `notes`, `settings`. 각 패키지 = Screen + ViewModel(`@HiltViewModel`) 패턴.
- `navigation` — `Screen.kt`(sealed class 라우트 정의), `ChemTableNavHost.kt`(NavHost + 하단 NavigationBar).
- `app/di` — `DatabaseModule`, `RepositoryModule`(Hilt `@Binds`).

**미니게임 삽입에 영향을 주는 핵심 파일/모듈:**
`navigation/Screen.kt`, `navigation/ChemTableNavHost.kt`(라우트·탭 정의), `core/util/MolarMassCalculator.kt` + `FormulaParser.kt`(재사용), `core/designsystem/component/ElementCell.kt`(블록 UI), `domain/usecase/GetElementsUseCase.kt`(원소 데이터 공급), `app/di/RepositoryModule.kt`(새 레시피/스코어 repository 바인딩 시).

## 2. Current UX/IA Assessment

**현재 앱 흐름:** 하단 5탭 구조 — **주기율표 · 검색 · 시각화 · 계산기 · 사전**. 시작 화면은 주기율표.

- **주기율표:** 18열 LazyVerticalGrid, pinch-zoom/pan(`detectTransformGestures`), 셀 탭 → 상세, long-press → `ElementQuickPreview`.
- **원소 상세(ElementDetail):** 노트 열기·노트 추가·연관 용어(glossary) 이동 콜백 보유 → 이미 다른 기능으로의 허브 역할.
- **검색:** 모드/정렬/필터 패널.
- **시각화:** Canvas 기반 속성 비교(heat map gradient 등). **가장 단순하고 확장 여지가 큰 탭.**
- **계산기:** 몰 질량 계산기. quickKeys에 `H, C, N, O, S, P, Cl, Na, Ca, Fe, Cu, (, ), ·, →, +` 이미 존재 → "분자 만들기"와 개념적으로 직결.
- **사전(Glossary):** `GlossaryTerm`에 **`interactiveType`(ELECTRON_ANIMATION, BOND_VISUALIZATION, DECAY_SIMULATION…)** 및 `relatedElements`/`relatedTerms` 필드 보유 → "인터랙티브 레슨" 개념이 이미 데이터 모델에 내장.

**학습 흐름상 강점:** 탐색(주기율표/검색) → 심화(상세) → 도구(계산기) → 개념(사전) → 개인화(노트)로 이어지는 선형·발견형 구조. 모델 레이어가 풍부(밀도, 전기음성도, 원자량 등 다수 속성)해 교육 콘텐츠 확장에 유리.

**삽입 시 주의점:**

1. **하단 탭이 이미 5개** — Material 가이드 권장 상한(3~5). 6번째 탭 추가는 터치 타깃 폭을 좁히고 라벨이 한글이라 더 빡빡해짐.
2. 게임은 drag/swipe를 쓰는데, 주기율표가 이미 pan/zoom 제스처를 점유 → **같은 화면에 게임을 인라인으로 넣으면 제스처 충돌** 위험. 게임은 별도 전체화면 라우트여야 함.
3. 기존 화면들이 모두 `innerPadding`(공유 Scaffold) 위에서 동작 → 게임 보드는 풀스크린/몰입형이 자연스러운데, 공유 하단바와 충돌. 게임 in-play 화면은 하단바를 숨기는 별도 라우트 권장.

## 3. Placement Options

| 후보 | 장점 | 단점 | 구현 영향도 | UX 적합도 | 추천도 |
|---|---|---|---|---|---|
| **A. 독립 하단탭 "Lab/게임"** | 발견성 최상, 핵심기능 신호 | 탭 6개→과밀, 한글 라벨 압박, 나머지 탭 폭 축소 | 높음(탭/IA 재설계) | 중 | △ |
| **B. 시각화 탭을 "탐구(Lab)" 허브로 확장 → 그 안에 게임 카드** | 탭 수 유지(5), 발견성 양호, 약한 탭 강화, 제스처 충돌 없음(게임은 별도 라우트) | 시각화 탭 정체성 재정의 필요 | 중 | 상 | ◎ |
| **C. 주기율표 화면 상단 "실험/게임" CTA** | 메인에서 즉시 노출 | 주기율표 제스처/공간과 경쟁, 학습보다 게임 인상 | 중 | 중 | ○ |
| **D. 원소 상세에서 "이 원소로 시작" 진입** | 맥락 학습 연결 강력 | 단독 발견성 거의 0(보조 진입로로만) | 낮음 | 상(보조) | ○(보조) |
| **E. 계산기 "분자 만들기" 모드 연결** | 계산기·레시피 개념 직결, 재사용 큼 | 계산기 사용자만 발견, 게임이 도구 하위로 묻힘 | 낮음 | 중 | ○(보조) |
| **F. 사전 Interactive Lesson으로 배치** | 교육 정합성 최상(`interactiveType`/`relatedElements` 기존 모델 활용) | 발견성 최저, 사전 안에 게임이 있다고 기대 안 함 | 낮음 | 중(교육) | ○(보조) |

## 4. Recommended Placement

**1순위 — B: "시각화" 탭을 "탐구(Lab)" 허브로 확장하고, 그 안에 미니게임을 카드로 배치. 게임 플레이는 하단바 없는 별도 풀스크린 라우트.**
탭 수를 5로 유지해 IA를 깨지 않으면서, 현재 가장 약한 시각화 탭을 "시각화 + 분자 만들기 게임 + (향후) 인터랙티브 레슨"을 모은 탐구 허브로 승격한다. 게임 보드는 제스처 충돌과 하단바 충돌을 피하기 위해 별도 라우트(`game/molecule`)로 띄운다. **발견성·구현비용·제스처 안전성의 균형이 가장 좋다.**

**2순위 — D+E 보조 진입로 병행:** 원소 상세의 "이 원소로 미니게임 시작" CTA와 계산기의 "분자 만들기 모드" 링크를 **추가 진입점**으로 둔다(허브를 메인 입구로, 이 둘은 맥락 진입로로). 단독 배치로는 부족하지만 1순위와 합치면 학습 흐름 연결이 강해진다.

**추천하지 않음 — A(독립 6번째 탭):** 한글 라벨 5개로도 빡빡한 하단바에 탭을 추가하면 터치 타깃·가독성이 나빠지고, 검증 전 기능에 영구 IA 비용을 지불하게 된다. (단, Phase 3에서 사용지표가 좋으면 그때 승격 검토.)

**최소 변경 경로:** ① `Screen.kt`에 `MiniGame`/`Lab` 라우트 추가 → ② `ChemTableNavHost`에 `composable` 등록(게임 in-play는 하단바 숨김 처리) → ③ 시각화 화면 상단에 "분자 만들기" 진입 카드 1개 추가. 기존 탭 정의·데이터·DB는 건드리지 않음.

**장기 확장 경로:** 시각화 탭을 정식 "탐구" 탭으로 리브랜딩 → 게임을 난이도/도감/미션 포함 정식 모드로 → 지표 검증 후 필요 시 독립 탭 승격, 합성한 분자를 사전/계산기/도감과 양방향 연결.

## 5. Mini-game MVP UX

**화면 구성 (라우트 단위):**

- **진입(허브 카드):** 시각화/탐구 탭 상단 "분자 만들기" 카드 — 한 줄 설명 + "플레이" 버튼.
- **튜토리얼(최초 1회/스킵 가능):** 3스텝 코치마크 — ①"무거운 원소는 아래로, 가벼운 원소는 위로" ②"스와이프로 블록을 모으세요" ③"올바른 조합이 만나면 분자가 됩니다(예: H + H → H₂)". 첫 진입 시만 자동, 이후 ?버튼으로 재호출.
- **보드 화면(풀스크린, 하단바 숨김):** 상단 HUD(점수·목표·남은 이동/시간), 중앙 4×4 grid, 하단 보조 영역(다음 등장 원소·일시정지).
- **일시정지/결과 화면:** 재개·재시작·나가기 / 결과는 점수·만든 분자 목록(탭하면 상세로).

**원소 블록 UI:** 기존 `ElementCell`(카테고리 색 + 기호 + 원자번호) 재사용, 하단에 작은 원자량 표기 추가. 무게감을 색 명도/그림자로 살짝 암시.

**분자/화합물 블록 UI:** 화학식(H₂O, CO₂) + 작은 분자량 라벨 + 구성 원소 색의 합성 톤. 원소 블록과 시각적으로 구분(테두리/배지).

**보드 interaction:** 상하좌우 swipe → 전체 블록이 한 방향으로 슬라이드(2048류). 단, "중력"은 **원자량/분자량 기반**: swipe 후 각 블록이 자기 mass에 따라 무거우면 아래쪽, 가벼우면 위쪽 행으로 정렬되려는 보정 1틱 적용. drag는 MVP에서 단일 블록 미세 이동(선택). 애니메이션은 기존 `theme/Motion.kt` 토큰 사용.

**합성 성공 feedback:** 두 블록이 한 칸에서 만나 레시피 충족 시 → 합치는 애니메이션 + 색 플래시 + "H₂O 생성!" 토스트 + 점수 팝 + (옵션) 짧은 진동.

**합성 실패 feedback:** 잘못된 조합이 인접해도 합쳐지지 않고 살짝 흔들림(shake) + 회색 X — **벌점 없이** "이 조합은 결합하지 않아요" 힌트만(오개념 방지·학습 친화).

**점수/목표/미션 표시:** HUD에 현재 점수, 목표 분자 칩(예: "H₂O ×2 만들기"), 콤보 카운터. MVP는 미션 1개 + 점수.

**기존 기능 연결:** 결과/플레이 중 만든 분자 블록 탭 → 선택지: "계산기에서 보기"(식 프리필) / "사전에서 보기"(관련 용어) / "구성 원소 상세 보기"(ElementDetail). 진입을 원소 상세에서 했다면 그 원소를 보드 시작 원소로 보장.

## 6. Game Rule Proposal

**Block model:**

```
GameBlock = Element 블록(symbol, atomicNumber, molarMass, category색)
          | Molecule 블록(formula, molarMass=Σ, 구성 elements)
massScore = molarMass (그대로 사용; Element.molarMass는 non-null)
```

**Recipe model (MVP는 자유결합 금지, 화이트리스트 레시피만):**
다중원소 동시 조합은 구현·교육 난이도가 높으므로 **MVP는 "2-입력 단계 합성"**으로 단순화 권장:

- H + H → H₂, O + O → O₂, N + N → N₂ (동핵 이원자분자)
- O + H₂ → H₂O (안전한 단계 경로; OH 중간체류는 오개념 위험이라 피함)
- C + O₂ → CO₂ (또는 CO + O → CO₂)
- NH₃, NaCl 등은 **사전 정의된 최종 레시피 매칭**: NH₃, H₂O, CO₂, NaCl, O₂, H₂를 "정답 조합 집합"으로 데이터화하고, 보드에서는 **2개씩 단계 병합**으로만 도달.
- Na + Cl → NaCl

레시피는 정적 JSON(또는 코드 상수)로 선언 → 화학적으로 검증된 항목만 등장. **임의 원소를 붙여 가짜 분자를 만들 수 없게** 화이트리스트로 잠근다.

**Gravity / massScore model:** swipe 방향으로 슬라이드 후, 열(column) 단위로 `molarMass` 내림차순이 되도록 "정렬 중력" 1틱 적용 → "무거우면 가라앉는다"를 체감. 단순·결정적이라 성능/예측성 양호.

**밀도(density):** MVP 제외 → **고급 모드로 연기.** 이유: `liquidDensity`가 nullable이고 기체/고체 혼재라 일관 비교가 어렵고, "원자량=무게"보다 직관적이지 않아 초급 오개념 유발. 고급 모드에서 "같은 분자량이면 밀도로 2차 정렬" 식 도입.

**Scoring:** 합성 성공 = 생성 분자의 분자량 비례 점수 + 콤보 배수. discovery bonus = 처음 만든 분자 1회 보너스. mistake penalty = **없음(MVP)**, 흔들림 피드백만.

**Difficulty:** 등장 원소 풀과 목표 분자로 조절(아래 8절).

## 7. Data/Architecture Impact

**필요한 data model (신규, 게임 패키지 내부):**
`GameBlock`(sealed: ElementBlock/MoleculeBlock), `BoardState`(4×4), `Recipe`(inputs→productFormula), `GameResult`. 모두 `core/model`을 **읽기만** 하고 새 모델은 `feature/minigame/model`에 격리.

**기존 레이어 연결:**

- 원소 데이터: `GetElementsUseCase`(기존) 그대로 주입 → 원자량·색·기호 확보.
- 분자량 계산/검증: `MolarMassCalculator` + `FormulaParser`(기존) 재사용 → 별도 화학 로직 중복 없음.
- 레시피: `assets/recipes.json`(신규 정적 자산) 또는 코드 상수. DB 불필요.

**새 feature module/package 위치:** `feature/minigame/`(또는 `feature/lab/molecule/`) — 기존 feature 패턴과 동일하게 `MiniGameScreen.kt` + `MiniGameViewModel.kt`(`@HiltViewModel`) + 내부 `model/`, `engine/`(보드 로직·중력·레시피 매칭 순수 Kotlin).

**상태관리:** 기존 패턴대로 ViewModel + `StateFlow<GameUiState>`. 보드 로직은 순수 함수(`engine`)로 분리해 단위테스트 용이하게.

**persistence 필요 여부:** MVP는 **불필요**(세션 내 메모리). 최고점수/도감만 남기려면 후속에 작은 Room 테이블(`GameRecordEntity`) 또는 DataStore 1개 추가 — 기존 `RepositoryModule`에 `@Binds` 한 줄 패턴으로 확장 가능.

## 8. Risks & Safeguards

- **화학적 정확성:** ①임의 결합 허용 시 가짜 분자 오개념 → **화이트리스트 레시피로 잠금.** ②"원자량 큰 게 가라앉는다"를 밀도로 오해 → 게임 내 문구를 "원자량(질량) 기준"으로 명시, 밀도는 고급 모드 분리. ③화학량론(H₂O는 H 2개 필요) 왜곡 방지 → 레시피에 정확한 계수 명시, 결과 분자 탭 시 계산기/사전으로 보내 정식 정보 확인.
- **UX 복잡도:** 다중원소 동시합성·중력·레시피를 한 번에 넣으면 과부하 → MVP는 2-입력 단계 합성 + 단일 미션으로 축소.
- **성능:** 4×4(16셀)는 작아 Compose recomposition 부담 적음. Canvas/애니메이션은 기존 시각화 탭 수준. 주의점은 게임 화면에서 주기율표의 pan/zoom 제스처와 섞이지 않게 **별도 라우트로 격리**.
- **기존 구조 변경 리스크:** 6번째 탭 추가를 피하면 IA 변경 최소. 변경은 `Screen.kt`/`NavHost`의 **추가**와 시각화 화면의 카드 1개로 국한 → 기존 화면 회귀 위험 낮음. 게임 in-play에서 하단바 숨김 처리만 검증 필요.

## 9. Implementation Roadmap

**Phase 0 — 설계/프로토타입 (코드 외)**
Task: 보드/중력/레시피 규칙 페이퍼 프로토타입, 레시피 JSON 스키마 확정, 게임 라우트·하단바 숨김 방식 결정.
Acceptance: 레시피 6종·점수·중력 규칙이 문서로 확정; 제스처/하단바 충돌 회피안 합의.

**Phase 1 — MVP**
Task: `feature/minigame` 패키지(Screen+VM+engine), 4×4 보드, swipe 이동, massScore 중력, 화이트리스트 2-입력 합성, 성공/실패 피드백, 단일 미션+점수, 시각화 탭 진입 카드, 게임 라우트 등록. engine 단위테스트.
Acceptance: H+H→H₂, Na+Cl→NaCl 등 6 레시피 정상 합성; 무거운 블록이 아래로 정렬; 잘못된 조합은 벌점 없이 흔들림; 게임 화면에서 하단바 숨김; 기존 5탭 회귀 없음.

**Phase 2 — 학습 연동**
Task: 결과/블록 → 계산기(식 프리필)·사전(관련 용어)·원소 상세 딥링크; 원소 상세·계산기에서 게임 진입 CTA; 튜토리얼 코치마크.
Acceptance: 생성 분자 탭 시 3개 연결 동작; 상세에서 "이 원소로 시작" 시 해당 원소가 보드에 보장 등장.

**Phase 3 — 고급 규칙/도감/난이도**
Task: 초/중/고급(등장 원소 풀·목표 분자), 콤보·discovery bonus, time-attack/mission/endless 모드, 분자 도감(persistence: 작은 Room 또는 DataStore), 밀도 2차 정렬 고급 옵션. 지표 좋으면 독립 탭 승격 검토.
Acceptance: 난이도별 원소 풀 적용; 도감에 발견 분자 저장/재방문; 모드 전환 동작.

**난이도/모드 권장(8절 점수 구조):** 초급=H,O,Na,Cl·목표 H₂O/NaCl / 중급=+C,N·목표 CO₂/NH₃ / 고급=+콤보·시간. 모드는 **MVP는 mission(목표 분자 달성), 이후 endless 추가, time-attack은 마지막.** mistake penalty는 도입하지 않음(학습 친화).

## 10. Files Likely to Touch Later (구현 시점에만; 현재 미수정)

- `navigation/Screen.kt` — 게임/허브 라우트 sealed object **추가**.
- `navigation/ChemTableNavHost.kt` — `composable` 등록, 게임 in-play 하단바 숨김 분기.
- `feature/visualization/VisualizationScreen.kt` — "분자 만들기" 진입 카드 추가(허브화).
- `feature/elementdetail/ElementDetailScreen.kt` — "이 원소로 미니게임 시작" CTA(Phase 2).
- `feature/calculator/CalculatorScreen.kt` / `CalculatorViewModel.kt` — "분자 만들기" 모드 링크·식 프리필 수신(Phase 2).
- `feature/glossary/*` — 분자→관련 용어 딥링크(Phase 2, 선택).
- `app/di/RepositoryModule.kt` (+ `DatabaseModule`, `core/database/*`) — 도감/최고점수 persistence 도입 시에만(Phase 3).
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — 추가 라이브러리 불필요(기존 Compose 제스처·애니메이션으로 충분). touch 가능성 낮음.
- **신규 생성 예정(수정 아님):** `feature/minigame/`(Screen/VM/engine/model), `assets/recipes.json`.

---

## Notes

- **코드 수정 없음:** 이 작업에서 기존 Kotlin/Compose/Room/Hilt 소스, Gradle 파일, 리소스 파일, AndroidManifest, DB/데이터 파일을 일절 수정하지 않았다. 본 문서(`docs/minigame-placement-audit-plan.md`)만 신규 생성했으며, `docs/` 디렉토리가 없어 생성했다.
- **실행한 명령(read-only + 디렉토리 생성):** 이전 audit 단계 — `git status`, `ls`, `find`, `cat`, `sed -n`. 본 저장 단계 — `docs` 디렉토리 존재 확인 후 `mkdir docs`(없어서 생성).
