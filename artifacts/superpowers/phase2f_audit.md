# Phase 2F Action Sheet Audit - In-game Molecule Block Action Sheet

## 현재 보드/블록 구조
1. **GameBoardView 및 셀 렌더링**:
   - `GameBoardView` ([MoleculeGameScreen.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/MoleculeGameScreen.kt))는 4x4 Grid 구조로 되어 있으며, 각 셀은 `ElementBlockView` 또는 `MoleculeBlockView`로 표현됩니다.
   - 현재 셀 단위에는 탭/클릭 이벤트 바인딩이 정의되어 있지 않습니다.
2. **제스처 제어**:
   - `GameBoardView`는 최외각 `Box`에 `Modifier.pointerInput`을 부착하여 `detectDragGestures`를 통해 스와이프(상하좌우)를 감지하고 있습니다.
   - 드래그 임계치(`threshold = 40f`)가 초과되었을 때만 스와이프 이벤트를 트리거합니다.

## 구현 전략
1. **제스처 충돌 회피 (핵심)**:
   - `detectDragGestures`는 터치를 강하게 가로채기 때문에 셀에 직접 `clickable`을 바인딩하면 탭이 씹히거나 스와이프 제스처가 씹히는 충돌이 발생할 수 있습니다.
   - **해결 방안 A (좌표 기반 역산)**: `detectTapGestures`와 `detectDragGestures`를 동일 `pointerInput` 스코프 내에서 순차 적용하거나, `detectDragGestures` 내부에서 누적 드래그 거리가 임계값 미만으로 `onDragEnd`에 도달할 경우 클릭 이벤트로 간주하여 터치 좌표 `(x, y)`로부터 행/열 `(row, col)`을 역산해 탭된 셀을 식별합니다.
   - **해결 방안 B (제스처 분기)**: 각 블록 뷰 내부에서 `detectTapGestures`를 쓰고 부모 뷰의 드래그를 조율하는 Compose의 nested scroll/gesture delegation 메커니즘을 조정합니다. 안전성을 고려할 때 **해결 방안 A(좌표 기반 역산)**가 런타임 제스처 씹힘을 방지하기에 가장 확실합니다.
2. **ViewModel 및 UI 상태 확장**:
   - `GameUiState` ([GameState.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/model/GameState.kt))에 현재 선택된 분자 블록 정보를 보관할 nullable 상태 `selectedMoleculeBlock: MoleculeBlock?`을 추가합니다.
   - View(Screen) 레퍼런스에서는 이 상태가 `non-null`이 될 때 Material 3 `ModalBottomSheet`를 표출합니다.
3. **분자 상세 룩업 메커니즘**:
   - `MoleculeBlock` 자체는 원소/용어 딥링크 정보를 직접 들고 있지 않지만, `GameUiState`가 가지고 있는 `discoveredMolecules: List<DiscoveredMolecule>` 리스트에 이미 2E 단계에서 완성한 `DiscoveredMolecule` 정보가 들어있습니다.
   - 따라서 `discoveredMolecules.find { it.formula == block.formula }`를 통하여 바텀 시트에 노출할 `elementLinks`와 `glossaryLinks` 데이터를 안전하게 바인딩할 수 있습니다.

## 수정 예상 파일
- [GameEvent.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/model/GameEvent.kt):
  - `data class BlockTapped(val blockId: Long) : GameEvent`
  - `data object CloseMoleculeSheet : GameEvent` 추가.
- [GameState.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/model/GameState.kt):
  - `GameUiState` 데이터 클래스 내에 `val selectedMoleculeBlock: MoleculeBlock? = null` 추가.
- [MoleculeGameViewModel.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/MoleculeGameViewModel.kt):
  - `BlockTapped` 이벤트 접수 시 보드 상태에서 ID로 블록을 검색하고, `MoleculeBlock`일 경우 `selectedMoleculeBlock` 상태에 업데이트하는 로직 구현.
  - `CloseMoleculeSheet` 이벤트 접수 시 `selectedMoleculeBlock = null` 처리.
- [MoleculeGameScreen.kt](file:///d:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/MoleculeGameScreen.kt):
  - `GameBoardView` 내에 탭 제스처 감지 로직 보완 및 탭 이벤트 위임.
  - `selectedMoleculeBlock` 상태 관찰에 따라 `ModalBottomSheet`를 띄우는 Composable 연동.

## UX 배치 (Action Sheet / Bottom Sheet Layout)
- **ModalBottomSheet (Material 3)**:
  - **헤더**: 화학식 문자열(숫자 아래첨자 처리) 및 한글 이름, 몰 질량 정보(`massScore` 이용)를 깔끔하게 시인.
  - **바디**:
    - **구성 원소**: `elementLinks` 리스트를 수평 스크롤 가능한 Row에 `ElementLinkChip`들로 렌더링. 클릭 시 `OpenElement(link.atomicNumber)` 트리거.
    - **관련 용어**: `glossaryLinks` 리스트를 수평 스크롤 가능한 Row에 `GlossaryLinkChip`들로 렌더링. 클릭 시 `OpenGlossary(link.termId)` 트리거.
  - **푸터**:
    - "계산기에서 보기" CTA 버튼 배치. 클릭 시 `OpenCalculator(formula)` 트리거.
    - "닫기" OutlinedButton 배치.

## 테스트 계획
1. **제스처 결합 테스트 (Unit/UI Test)**:
   - 보드 스와이프 조작 시 드래그 동작이 정상 작동하며 바텀 시트가 트리거되지 않음을 검증.
   - 분자 블록 영역의 단순 탭 동작 시 `BlockTapped` 이벤트가 정상 방출되는지 검증.
2. **ViewModel 상태 테스트**:
   - `BlockTapped` 수신 시 `selectedMoleculeBlock`이 지정되고, `CloseMoleculeSheet` 수신 시 `null`로 클리어되는지 단위 테스트 검증.
3. **딥링크 네비게이션 효과 테스트**:
   - 바텀 시트 내의 원소 칩, 용어 칩, 계산기 버튼 클릭 시 각각 `NavigateToElement`, `NavigateToGlossary`, `NavigateToCalculator` 일회성 이펙트가 정상 송출되는지 검증.

## 리스크
- **제스처 상호 간섭**: 스와이프 터치 이벤트와 개별 셀 탭 이벤트가 꼬여서 게임 조작감이 나빠지는 현상이 가장 큽니다. 좌표 역산 방식을 철저히 설계하고 테스트로 조율해야 합니다.
- **바텀 시트 높이 제한**: 4인치대 소형 기기에서 바텀 시트가 화면의 너무 많은 부분을 덮어 보드 상태를 가리거나 잘리지 않도록 `heightIn` 제한과 내부에 스크롤을 배치하여 가드해야 합니다.
- **접근성 결함**: 탭 이벤트에 대해 `contentDescription` 스피치 미흡 우려가 있으므로, "XX 분자 상세 정보 보기" 등의 명확한 접근성 라벨을 제공해야 합니다.
