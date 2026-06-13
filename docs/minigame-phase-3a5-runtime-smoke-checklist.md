# Mini-game Phase 3A.5 Runtime Smoke Checklist

## Scope
- Verify that molecule game persistence survives app relaunch.
- Verify that Discovery Dex remains usable on small screens.
- Verify that calculator, element detail, and glossary actions still work.

## Setup
- Install the current debug build.
- Start the app with a fresh or known test database when possible.
- Use the Lab tab as the entry point for both the mini-game and Discovery Dex.

## Empty Dex
1. Open the Lab tab.
2. Open "분자 도감".
3. Confirm the empty state appears when no molecule has been recorded.
4. Tap "분자 만들기 시작".
5. Confirm the mini-game opens and the bottom bar is hidden on the game route.

## Persistence
1. Play the mini-game until at least one molecule is made and the result screen is reached.
2. Open the Lab tab, then open Discovery Dex.
3. Confirm the molecule formula, discovery count, high score, and recent session are visible.
4. Force-stop the app.
5. Relaunch the app and open Discovery Dex again.
6. Confirm the same molecule and high score are still visible.
7. If feasible, make the same molecule again and confirm discovery count increments.

## Action Regression
1. From Discovery Dex, tap "계산기" for a discovered molecule.
2. Confirm the calculator opens with the molecule formula prefilled.
3. From Discovery Dex, tap an element chip.
4. Confirm Element Detail opens and Back returns to Dex.
5. From Discovery Dex, tap a glossary chip.
6. Confirm Glossary Detail opens and Back returns to Dex.

## Navigation Regression
1. Confirm Lab -> Dex -> Back returns to Lab.
2. Confirm the bottom bar is visible on Dex.
3. Confirm the bottom bar is hidden on game routes.
4. Confirm the existing five bottom tabs still switch normally.
