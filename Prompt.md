# Prompt.md — ChemTable Interactive (Single Source of Truth for the Autopilot)

> This file defines WHAT we are building and WHEN it is "done". It is durable
> autopilot memory. The canonical product spec lives in `agent/PRD.md` and
> `agent/ACCEPTANCE.md`; this file consolidates them with the autopilot's
> ship-readiness goal. Keep edits minimal and intentional.

## 1. Goals
- Ship **ChemTable Interactive**: an offline-first, on-device Android periodic-table
  learning app (Kotlin + Jetpack Compose + Material 3 + Hilt + Room, Clean Architecture).
- All element / isotope / glossary / recipe data is bundled in the APK
  (`app/src/main/assets/{elements,glossary,isotopes}.json`) and seeded into Room on
  first launch — the app must work fully **without a network**.
- Core learning features (all six are "필수" / required per `agent/PRD.md`):
  1. Interactive periodic table (tap → detail, pinch-zoom / pan).
  2. Fast search — by name / symbol / atomic number **and** property filters (range + sort).
  3. Element detail with physical **and** nuclear/isotope properties (N/A policy for unknowns).
  4. Per-element notes (CRUD, editable from detail).
  5. Property visualization (list chips/bars and/or table color-map).
  6. Molar-mass calculator (formula parsing: parentheses, hydrates, charges).
  7. Chemistry glossary (search + detail + cross-links to/from elements & properties).
- A "Molecule Making" mini-game (build molecules from elemental blocks via whitelist
  recipes; discovery Dex; 3 difficulties × 3 modes) extends the learning experience and
  is already implemented — keep it functional.

## 2. Non-goals (for the autopilot; do not scope-creep)
- No large rewrites / architecture changes. One small, verifiable milestone per run.
- No Play Store release engineering (signing keys, minification tuning) unless asked.
- No new network/online features (offline-first is a hard requirement).
- No new third-party dependencies without a clear, justified need.
- Do not destabilize the in-flight perf branch's uncommitted work (see Plan M5).

## 3. Deliverables
- A green canonical verify on a clean checkout: `scripts/verify.ps1` (Windows) /
  `scripts/verify.sh` → `validate_assets.py` + `gradlew lintRelease testReleaseUnitTest assembleRelease`.
- Durable autopilot docs kept current: `Prompt.md`, `Plan.md`, `Implement.md`, `Documentation.md`.
- Accurate developer onboarding docs (`AGENTS.md` env section truthful; a top-level `README.md`).
- The uncommitted startup-perf work resolved to a definite, non-dangling state.

## 4. Constraints
- Work only inside this repo; no edits outside the workspace.
- No network calls by default. (The build needs none: Gradle 9.0 dist + AndroidX/Compose
  deps are already cached under the user Gradle home; Android SDK is installed and
  referenced via `local.properties`.)
- Never create or commit secrets/keys. `local.properties` is git-ignored — never commit it.
- Toolchain (verified): Gradle **9.0**, AGP **8.13.0**, Kotlin **2.1.10**, KSP 2.1.10-1.0.29,
  `compileSdk` 35 / `minSdk` 26 / `targetSdk` 35, `kotlin.jvmToolchain(21)` + `jvmTarget=17`.
  The Gradle daemon JVM is pinned via `gradle.properties` (`org.gradle.java.home` →
  Android Studio JBR, JDK 21), so the system JDK (25) is not used for the build.

## 5. Done When (ship criteria)
Functional (from `agent/ACCEPTANCE.md`):
- [x] App renders the periodic table offline on first launch.
- [x] Element detail shows the required properties (N/A when unknown).
- [x] Quick search works for name / symbol / atomic number.
- [x] Property search: at least one property filter (range) + sort.
- [x] Property visualization: list and/or table color-map (≥1).
- [x] Notes: per-element CRUD, editable from element detail.
- [x] Calculator: molar mass from a typed chemical formula.
- [x] Glossary: search + detail + related-link navigation.

Quality / pipeline:
- [x] `scripts/verify.ps1` (or `.sh`) prints **"✅ verify PASS"** on a clean checkout
      (lintRelease + testReleaseUnitTest + assembleRelease all green; ~18 unit-test classes pass).
- [x] No crash on the basic flow: Table → Detail → Search → Calculator → Glossary.
- [x] Accessibility: element cells / buttons carry contentDescription.
- [x] Build environment is documented and reproducible (JDK/Gradle/JBR settled).
- [x] `AGENTS.md` env section is truthful and a top-level `README.md` documents build/test/verify.
- [x] The uncommitted perf/startup work is committed-as-is, finished, or reverted — not dangling.

> **Status (after M1–M8 + on-device verification, 2026-06-27): ALL Done-When boxes verified.**
> The full canonical gate (validate_assets + lintRelease + testReleaseUnitTest + assembleRelease)
> is green on the autopilot lineage, AND the app was run on an Android emulator
> (Medium_Phone_API_36.1, API 36): the basic flow Table → Detail → Search → Calculator → Glossary
> (+ Visualization, Notes) all render correctly with **no crash/ANR**; the calculator computed
> H₂O = 18.015 g/mol; element cells carry `contentDescription`. The app is **COMPLETE / ship-ready**
> — see the COMPLETE section in `Documentation.md`. Minor follow-up (NOT a Done-When blocker): the
> `Settings` screen is registered but has no UI entry point, so it is currently unreachable.

When every box above is checked, append a one-time "COMPLETE" section to
`Documentation.md` and stop making changes (see `Implement.md` §Termination).
