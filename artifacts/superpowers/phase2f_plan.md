# Phase 2F Implementation Plan - In-game Molecule Block Action Sheet

## 1. Goal
- 게임 플레이 중 보드 위에 있는 `MoleculeBlock`을 탭하면 Bottom Sheet(Action Sheet)를 띄워, 기존에 구현된 계산기/원소/용어 이동 연동 기능을 플레이 도중에도 자연스럽게 사용할 수 있도록 합니다.

## 2. Constraints & Risks
- **제스처 충돌 방지**: 보드의 스와이프(드래그) 제스처와 탭 제스처가 꼬이지 않도록 명확한 임계값(Tap Threshold vs Swipe Threshold) 필터링이 필요합니다.
- **회귀 차단**: 기존 Result Overlay의 CTA 연동과 계산기 프리필 기능이 오동작하거나 망가지지 않아야 합니다.
- **백핸들러 조율**: 바텀 시트가 열린 상태에서 시스템 백 버튼을 누르면 게임 일시정지가 되는 대신 바텀 시트만 안전하게 닫혀야 합니다.

## 3. Proposed Changes

### [T1. model 패키지 및 GameUiState 확장]
- [GameState.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/model/GameState.kt)에 `SelectedMoleculeSheet` 모델 추가 및 `GameUiState`에 `selectedMoleculeSheet` 바인딩 추가.
- [GameEvent.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/model/GameEvent.kt)에 `BlockTapped(blockId: Long)`와 `CloseMoleculeSheet` 이벤트 추가.

### [T2. ViewModel 비즈니스 로직 보완]
- [MoleculeGameViewModel.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/MoleculeGameViewModel.kt)에서 `BlockTapped` 이벤트 접수 시 보드 상태에서 `MoleculeBlock`을 룩업.
- `MoleculeElementLinkResolver`와 `MoleculeGlossaryLinkResolver`를 호출해 칩 리스트를 계산한 뒤 `selectedMoleculeSheet` 상태 업데이트.
- `OpenCalculator`, `OpenElement`, `OpenGlossary` 등의 액션 클릭 시 화면 이동 전 바텀시트 상태 클리어 처리 추가.

### [T3. GameBoardView 제스처 처리 보완]
- [MoleculeGameScreen.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/MoleculeGameScreen.kt)의 `GameBoardView` 내에 `awaitEachGesture` 및 `pointerInput` 기반의 커스텀 터치 감지 로직 적용.
- 터치 이동량이 tap threshold 미만이면 탭된 픽셀 좌표 `(x, y)`로부터 보드의 `(row, col)`을 역산하여 해당 셀에 올라온 블록의 탭 이벤트를 VM으로 전파.

### [T4. Bottom Sheet UI 렌더링 및 BackHandler 적용]
- [MoleculeGameScreen.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/MoleculeGameScreen.kt) 내에 `selectedMoleculeSheet` 상태가 `non-null` 일 때 `ModalBottomSheet` Composable을 출력하도록 함.
- 칩 렌더링 시 접근성 속성(`contentDescription`) 추가 제공.
- `BackHandler` 조건문을 조정하여 `selectedMoleculeSheet != null` 일 경우 뒤로가기가 시트를 닫도록 설정.

## 4. Verification Plan

### Automated Tests
- `MoleculeGameViewModelTest`에 `BlockTapped` 및 `CloseMoleculeSheet` 수신 시 상태 갱신 테스트 코드 추가.
- 기존 단위 테스트(`BoardEngineTest`, `MoleculeGlossaryLinkResolverTest` 등) 정상 패스 검증.
- `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest`

### Manual Verification
- 게임 보드 위의 MoleculeBlock을 탭하여 Bottom Sheet가 열리는지 확인.
- 계산기에서 보기 클릭 시 식 프리필 이동 및 back 시 정상 복귀 확인.
- 구성 원소 및 용어 칩 클릭 시 각 상세화면 이동 및 back 시 정상 복귀 확인.
- 바텀시트 뜬 채로 시스템 백 버튼 클릭 시 바텀시트만 닫히는지 확인.
- 일반 스와이프 조작(상하좌우)이 여전히 정상 작동하는지 확인.
