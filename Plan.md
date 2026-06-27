# Plan.md — Milestone Checklist

Rules: one milestone per run; smallest verifiable unit first. Priority order —
(1) build/test pipeline, (2) doc/run-flow mismatch, (3) feature gap/bug, (4) refactor.
`[ ]` todo · `[x]` done · `[~]` in progress. Each milestone lists Acceptance +
Validation commands + likely files. Validation that needs a device/emulator is marked.

---

## Priority 1 — Secure the build / verify pipeline

### [x] M1 — Confirm the build environment
- **Acceptance:** a documented JDK/Gradle combo runs gradle without toolchain errors;
  `JAVA_HOME`/`gradle.properties` reference a path that exists.
- **Validation:** `./gradlew --version`, `./gradlew help`
- **Result:** PASS. Gradle 9.0.0; launcher + daemon JVM = Android Studio JBR (JDK 21.0.9),
  pinned via `gradle.properties` `org.gradle.java.home`. System JDK (25) is not used.
  Gradle 9.0 dist + AndroidX/Compose deps already cached (no network needed).
- **Files:** gradle.properties, gradle/wrapper/gradle-wrapper.properties (read-only)

### [x] M2 — Create missing `app/proguard-rules.pro`
- **Acceptance:** `app/proguard-rules.pro` exists (empty/commented is fine, `isMinifyEnabled=false`);
  `assembleRelease` no longer fails resolving the proguard reference.
- **Validation:** `./gradlew assembleRelease`
- **Result:** PASS (verified in isolated worktree off `main`; `extractProguardFiles` +
  `assembleRelease` green). This was the priority-1 blocker for the canonical verify.
- **Files:** app/proguard-rules.pro (new), app/build.gradle.kts (reference, unchanged)

### [x] M3 — Make the canonical verify pipeline pass green
- **Acceptance:** `validate_assets.py` + `lintRelease` + `testReleaseUnitTest` + `assembleRelease`
  all succeed (~18 unit-test classes pass); `verify.ps1` prints "✅ verify PASS".
- **Validation:** `.\scripts\verify.ps1`  (or the four tasks individually)
- **Result:** PASS on clean `main` + M2 fix (worktree). See `Documentation.md` run log for
  the exact task results. The autopilot baseline is now green.
- **Files:** scripts/verify.ps1, scripts/verify.sh (read-only)

---

## Priority 2 — Documentation / run-flow accuracy

### [x] M4 — Fix stale `AGENTS.md` env section + add top-level `README.md`
- **Acceptance:** `AGENTS.md` "환경 요구사항" states the real toolchain (Gradle 9.0, AGP 8.13.0,
  Kotlin 2.1.10, `jvmToolchain(21)`/JVM_17 target) instead of the stale JDK17/AGP8.3.2/Kotlin2.0/Gradle8.7;
  a root `README.md` documents build (`gradlew assembleDebug`), test (`testReleaseUnitTest`),
  and verify (`verify.ps1`/`verify.sh`) flows.
- **Validation:** `rg -n '8\.3\.2|Kotlin 2\.0|Gradle 8\.7|JDK 17' AGENTS.md` returns nothing stale;
  manual review (no build/device).
- **Files:** AGENTS.md, README.md (new)
- **Result:** DONE (run 2, 2026-06-27). `AGENTS.md` "환경 요구사항" now lists Gradle 9.0 /
  AGP 8.13.0 / Kotlin 2.1.10 / jvmToolchain(21)+JVM_17 (JBR-21 daemon JVM); top-level
  `README.md` added. `rg '8.3.2|Kotlin 2.0|Gradle 8.7|JDK 17' AGENTS.md` → no matches.

---

## Priority 3 — Feature gaps, bugs, and the in-flight perf work

### [ ] M5 — Resolve the uncommitted perf/startup work to a definite state
- **Context:** branch `codex/perf-profileable-measurement` carries ~90%-complete startup
  instrumentation (StartupTrace, deferred seeding via AppStartupSeeder, profileable release,
  PeriodicTableRoute loading shell). It must NOT merge to main as-is.
- **Acceptance:** (a) the hardcoded `delay(500)` in PeriodicTableRoute is removed or guarded
  behind a debug/BuildConfig check; (b) `AppStartupSeeder.start()` wraps the Hilt
  `EntryPointAccessors` access in try/catch; (c) the `PeriodicTableViewModel`
  `stateIn(WhileSubscribed)` → `MutableStateFlow` change is verified safe for late/empty
  subscription (loading shell covers it) with a note; (d) the branch is committed/finished/reverted
  — no dangling perf changes mixed with feature work.
- **Validation:** `./gradlew testReleaseUnitTest`; full UX confirmation needs a device/emulator.
- **Files:** feature/periodictable/{PeriodicTableScreen,PeriodicTableViewModel}.kt,
  navigation/ChemTableNavHost.kt, data/prepopulate/AppStartupSeeder.kt, core/util/StartupTrace.kt,
  MainActivity.kt, ChemTableApp.kt, app/src/main/AndroidManifest.xml

### [ ] M6 — Add/extend Room migration safety test (v4→v5→v6)
- **Acceptance:** a JVM test asserts the schema reaches version 6 via migrations without
  dropping element/glossary/note data and creates the two game tables (builds on
  `GameStatsMigrationSqlTest`).
- **Validation:** `./gradlew testReleaseUnitTest --tests com.chemtable.interactive.core.database.GameStatsMigrationSqlTest`
- **Files:** app/src/test/.../core/database/GameStatsMigrationSqlTest.kt, core/database/(DbMigrations, ChemTableDatabase)

### [ ] M7 — Wire mini-game Result-overlay molecule actions (Phase 2)
- **Acceptance:** tapping a made molecule offers Calculator (formula prefill), Glossary
  (term lookup), and Element Detail (per-atom) navigation; each navigates without crash
  (formula prefill via existing CalculatorRoute encoding).
- **Validation:** `./gradlew testReleaseUnitTest` (route encoding covered by CalculatorRouteTest/
  CalculatorPrefillTest; full flow needs device/emulator).
- **Files:** feature/minigame/{MoleculeGameScreen,MoleculeGameViewModel}.kt, navigation/ChemTableNavHost.kt

### [ ] M8 — Implement or explicitly disable the Settings stub
- **Acceptance:** Settings gains ≥1 working DataStore-backed option (e.g. theme toggle), or
  its non-functional items (data sync) are removed / marked "coming soon" so the UI does not
  imply unimplemented behavior.
- **Validation:** `./gradlew testReleaseUnitTest` (UI confirmation needs device/emulator).
- **Files:** feature/settings/SettingsScreen.kt

---

## Deferred backlog (not yet milestones; promote when reached)
- Mini-game Phase 2 polish: TutorialCoachMarks step highlights; merge-success animation +
  score popup; invalid-combo shake (component/GameBoardView, MoleculeBlockView).
- Mini-game Phase 3C: density as 2nd-order gravity tiebreaker (Advanced) — needs density-data audit;
  Dex completeness % / achievement badges.
- Instrumented `androidTest`: migration + game→calculator→glossary→element e2e smoke (needs device).
- Release hardening (signing config, decide on minification) — only if a Play-ready build is requested.
