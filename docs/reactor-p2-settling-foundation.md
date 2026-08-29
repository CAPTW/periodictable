# Reactor P2 settling foundation

`분자 반응조 · 기초 실험`은 완성된 Reactor 게임이 아니라, 결정적 한 턴과 한 단계 침강을 검증하기 위한 실험적 기초 기능이다. 기존 Classic `분자 만들기`와 별도 route, ViewModel, board model, entity store, turn engine을 사용한다. Classic의 4×4/5×5/6×6 선택, 점수, 세션 기록, Molecule Dex 및 Room v7 데이터는 Reactor 상태를 저장하거나 변경하지 않는다.

## P2 범위

- 화면은 고정된 5×5 샘플 보드로 시작한다.
- 한 번의 스와이프는 압축, 허용된 Classic 레시피 조합, 정확히 한 번의 odd/even 침강 tick 순으로 처리된다.
- Classic `RecipeBook`이 유일한 여섯 레시피 권한이며 Reactor는 adapter를 통해 multiset 결과만 조회한다.
- 모든 이동·결합·침강 이벤트는 순서대로 기록되고, 독립 event replayer가 최종 보드를 재구성한 경우에만 화면 상태로 게시된다.
- Reset은 동일한 위치와 결정적 ID, turn 0, phase 0을 복원한다.
- Reactor 상태는 ViewModel 수명 동안의 메모리에만 존재한다. 프로세스 종료 뒤 복원하지 않는 것이 P2의 의도된 제한이다.

P2에는 feed, pressure, polymer, enzyme, score, item, Reactor Dex, 네트워크 또는 production monetization이 없다. 광고·결제·Billing·Backend도 추가하지 않는다.

## 침강 게임 모델

기본 기준 몰질량은 `32.0 g/mol`, 반올림 허용 오차는 `0.01 g/mol`이다. 침강 지수는 `몰질량 - 기준 몰질량`이며 결과를 `RISE`, `NEUTRAL`, `SINK`로 나눈다. 프로필은 주입 가능하므로 이후 단계가 turn resolver를 다시 작성하지 않고 값을 교체할 수 있다.

침강 지수는 몰질량을 바탕으로 한 게임용 단순화이다. 실제 물질의 밀도·부력·침강은 물질 상태, 매질, 온도, 구조 등 여러 조건에 따라 달라진다. 이 기능은 실제 밀도나 침강의 과학 시뮬레이션이 아니다.

phase 0은 각 열의 `(0,1)`, `(2,3)` 쌍을, phase 1은 `(1,2)`, `(3,4)` 쌍을 pre-tick snapshot에서 동시에 판정한다. 각 entity는 한 tick에 최대 한 칸만 이동하며 한 번만 참여한다.

## 검증

JVM 검증은 Reactor model, entity-store invariant, 4/5/6 board validity, profile, odd/even resolver, turn pipeline, canonical recipe adapter, deterministic IDs, event replay 및 5×5 sample authority를 포함한다.

Android instrumentation은 Gradle `connected*`, UTP 또는 Test Orchestrator 없이 직접 ADB로만 실행한다. 설치된 앱 데이터는 지우지 않으며 uninstall 또는 `pm clear`를 사용하지 않는다.

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -r `
  -e class com.chemtable.interactive.feature.minigame.reactor.ReactorFoundationComposeTest,com.chemtable.interactive.feature.minigame.reactor.ReactorFoundationNavigationTest `
  com.chemtable.interactive.test/androidx.test.runner.AndroidJUnitRunner
```

직접 계측 검증은 Reactor 카드와 별도 route, 정확한 25칸, 면책·범례, 한 스와이프 한 턴, 이벤트 출력, Reset, 의미론을 확인한다. 기존 Classic 계측 테스트도 함께 실행해 4×4/5×5/6×6 선택과 동작의 회귀가 없음을 확인한다.
