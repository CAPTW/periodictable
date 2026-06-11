# Phase 2B Fix Gate

We will perform the Phase 2B Fix Gate to address the formula query parameter decode asymmetry, introduce a process death recovery flag (`prefillConsumed`), add vertical scroll constraints to the made molecules list overlay, and document URL percent-encoding details.

## User Review Required

> [!IMPORTANT]
> The URL decode logic has been designed to treat chemistry `+` signs (e.g., in `Na+` or multi-formula reactions like `A+B`) correctly without converting them to spaces.
> To achieve this, we avoid direct use of `URLDecoder.decode(value, "UTF-8")` because it translates `+` to spaces (application/x-www-form-urlencoded format). Instead, we replace `+` with `%2B` before calling `URLDecoder.decode`, ensuring both raw and decoded strings round-trip successfully.

## Proposed Changes

### Navigation Layer

#### [MODIFY] [Screen.kt](file:///D:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/navigation/Screen.kt)
- Add `decodeArg(value: String): String` helper to `Screen.Calculator`.
- It replaces `+` with `%2B` before decoding via `java.net.URLDecoder.decode(..., "UTF-8")` to keep chemical formulas with plus signs (`+`) from being parsed into spaces.
- Add detailed comments to `encodeArg` explaining why `.replace("+", "%20")` is used (since standard `URLEncoder` maps spaces to `+`, but we want spaces as `%20` and literal plus signs as `%2B`).

#### [MODIFY] [ChemTableNavHost.kt](file:///D:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/navigation/ChemTableNavHost.kt)
- Add/enhance explanatory comments for the distinction between `route` (the pattern matching route used for selection matching) and `navRoute` (the concrete route navigated to).

---

### Calculator Feature

#### [MODIFY] [CalculatorViewModel.kt](file:///D:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/calculator/CalculatorViewModel.kt)
- Update `init` block to decode the formula query argument retrieved from `SavedStateHandle` using `Screen.Calculator.decodeArg`.
- Introduce a `"prefill_consumed"` boolean flag inside `SavedStateHandle` to mark the prefill as processed once handled, preventing repeated prefill and automatic calculations when recovering from a process death.

---

### Minigame Feature

#### [MODIFY] [MoleculeGameScreen.kt](file:///D:/dev/repos/PeriodicTable/app/src/main/java/com/chemtable/interactive/feature/minigame/MoleculeGameScreen.kt)
- Wrap the "만든 분자" (made molecules) list inside `ResultOverlay` in a `Column` with a max height constraint (`.heightIn(max = 160.dp)`) and vertical scroll (`.verticalScroll(rememberScrollState())`) to prevent screen layout overflow on smaller devices when the list is long.

---

### Unit Tests

#### [MODIFY] [CalculatorRouteTest.kt](file:///D:/dev/repos/PeriodicTable/app/src/test/java/com/chemtable/interactive/navigation/CalculatorRouteTest.kt)
- Add test cases to assert round-trip encoding and decoding for:
  - `H2O`
  - `Ca(OH)2`
  - `CuSO4·5H2O` (special dot character)
  - `H2O+NaCl` (special plus character)

#### [MODIFY] [CalculatorPrefillTest.kt](file:///D:/dev/repos/PeriodicTable/app/src/test/java/com/chemtable/interactive/feature/calculator/CalculatorPrefillTest.kt)
- Add verification that the decode logic successfully handles raw, partially decoded, and fully encoded inputs without corrupting `+` characters.

---

## Verification Plan

### Automated Tests
We will verify the changes using:
1. Unit tests:
   `cmd /c ".\gradlew.bat --no-daemon -Dorg.gradle.vfs.watch=false -Pkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest"`
2. Compilation gate:
   `cmd /c ".\gradlew.bat --no-daemon -Dorg.gradle.vfs.watch=false -Pkotlin.compiler.execution.strategy=in-process :app:compileDebugKotlin"`
3. APK assembly:
   `cmd /c ".\gradlew.bat --no-daemon -Dorg.gradle.vfs.watch=false -Pkotlin.compiler.execution.strategy=in-process :app:assembleDebug"`
4. Verification script:
   `.\scripts\verify.ps1`

### Manual Verification
1. Play the game, synthesize `H2` or `H2O`, and tap **"계산기에서 보기"** (View in Calculator) from the results screen.
2. Confirm the formula is correctly filled and auto-calculated in the calculator screen.
3. Tap back to confirm returning to the game result overlay.
4. Verify the made molecules list scrolls correctly if populated with multiple items.
