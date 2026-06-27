# Documentation.md — Autopilot Progress Log

## Current status
**Build/verify pipeline: GREEN baseline established.** The canonical verify gate passes on
a clean `main` checkout plus the M2 fix. The app's core features are implemented (periodic
table, search, element detail, calculator, notes, glossary, visualization, molecule mini-game);
`Settings` is a UI-only stub. M1–M4 are done; remaining work toward "ships" is tracked in
`Plan.md` (M5–M8): resolving the in-flight perf/startup branch (M5) and feature gaps (M6–M8).

Not yet COMPLETE — `Prompt.md` Done-When still has open items (M5 perf resolution;
M6–M8 feature work; on-device flow/accessibility confirmation, which needs an emulator).

## How to build / test / verify (verified 2026-06-27)
Environment (no network required — all cached):
- Gradle **9.0.0** (wrapper); AGP **8.13.0**; Kotlin **2.1.10** (KSP 2.1.10-1.0.29).
- JDK: the Gradle daemon JVM is pinned to the **Android Studio JBR (JDK 21.0.9)** via
  `gradle.properties` (`org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr`).
  The system JDK (Temurin 25) is NOT used. `verify.ps1` also force-sets `JAVA_HOME` to the JBR.
- Android SDK: installed; path supplied by the git-ignored `local.properties` (`sdk.dir`).

Commands (from repo root):
```
# Unit tests (JVM, no device):
./gradlew testReleaseUnitTest          # or .\gradlew.bat on Windows
# Release assembly (exercises the proguard/release path):
./gradlew assembleRelease
# Full canonical gate:
.\scripts\verify.ps1                   # Windows
bash scripts/verify.sh                 # Unix
# Debug APK for install on a device/emulator:
./gradlew assembleDebug
```
Running the app interactively requires an Android device/emulator (no headless path).

## Run log

### Run 2 — 2026-06-27 — M4: truthful AGENTS.md env + top-level README
- Replaced the stale `AGENTS.md` "환경 요구사항" section with the verified toolchain
  (Gradle 9.0 / AGP 8.13.0 / Kotlin 2.1.10 / `jvmToolchain(21)`+JVM_17; JBR-21 daemon JVM).
- Added top-level `README.md` (features, requirements, build/test/verify, project layout).
- Validation: `rg '8.3.2|Kotlin 2.0|Gradle 8.7|JDK 17' AGENTS.md` → no matches. Doc-only; no build/device.
- Next: **M5** — resolve the perf/startup branch to a definite state.

### Run 1 — 2026-06-27 — M1+M2+M3: secure the build/verify pipeline + autopilot scaffolding
**What I did**
- Mapped the repo (6-way parallel read + synthesis) to ground accurate durable docs.
- Found the priority-1 blocker: `app/build.gradle.kts` (release block) references
  `proguard-rules.pro`, but the file was **missing** (untracked, not on disk) → the canonical
  `verify` (`assembleRelease`) would fail. Created `app/proguard-rules.pro` (standard
  Android-Studio default; inert since `isMinifyEnabled=false`, but required to resolve the reference).
- Created the durable autopilot docs: `Prompt.md`, `Plan.md`, `Implement.md`, `Documentation.md`.
- Established `refs/codex/autopilot` and review branch `codex/autopilot`.

**How I worked safely**
- The live working tree sits on `codex/perf-profileable-measurement` with substantial
  **uncommitted** startup-perf WIP. I did NOT touch it. All build/commit work happened in an
  isolated `git worktree` off clean `main` (`d7e22f1`), with `local.properties` copied in.

**Commands run + results**
- `./gradlew --version` → Gradle 9.0.0; launcher/daemon JVM = JBR 21.0.9. ✅
- `python scripts/validate_assets.py` → `[asset-validate] PASS` (exit 0). ✅
- `./gradlew testReleaseUnitTest assembleRelease` → **BUILD SUCCESSFUL in 1m28s** (exit 0);
  `extractProguardFiles`, `testReleaseUnitTest`, `lintVitalRelease`, `packageRelease`,
  `assembleRelease` all green. ✅ (test inventory ≈ 18 unit-test classes; all passed, no failures)
- `./gradlew lintRelease` → **BUILD SUCCESSFUL** (exit 0); lint report written, no errors. ✅
- Net: every task in `scripts/verify.ps1` (validate_assets + lintRelease + testReleaseUnitTest
  + assembleRelease) passes ⇒ canonical gate is green.

**Result:** M1, M2, M3 complete. Build/verify baseline secured.

**Next milestone:** **M4** — fix the stale `AGENTS.md` env section (it claims JDK17 / AGP 8.3.2 /
Kotlin 2.0 / Gradle 8.7 — all wrong) and add a top-level `README.md`. Doc-only, no build/device
needed; smallest next verifiable unit.

## Decisions (why)
- **Created `proguard-rules.pro` instead of removing the build.gradle reference.** Matches the
  Android-Studio default and keeps a home for keep-rules if minification is enabled later;
  removing the reference would diverge from the standard release template.
- **Worked in an isolated worktree off `main`.** The current branch has valuable uncommitted
  perf WIP; detaching/stashing the live tree risked it. A worktree gives a clean, true baseline
  with zero risk to that work, matching `Implement.md`'s durable-state model.
- **Did not edit `AGENTS.md` this run.** It's an existing durable file; per the autopilot rules
  it gets minimal, intentional updates — scheduled as its own milestone (M4) to keep run 1 focused.
- **Baseline against `main`, not the perf tree.** The perf WIP is ~90% but not mergeable as-is;
  the autopilot baseline must be the clean reference. Perf resolution is its own milestone (M5).

## Known issues / risks (see `Plan.md` for the actionable backlog)
- Stale `AGENTS.md` environment section (JDK17/AGP8.3.2/Kotlin2.0/Gradle8.7 vs actual 21/8.13.0/2.1.10/9.0). → M4
- No top-level `README.md`; run/verify flow only in the Korean `AGENTS.md`. → M4
- Uncommitted perf/startup branch is dangling/not-mergeable: hardcoded `delay(500)` in
  PeriodicTableRoute, no try/catch around Hilt `EntryPointAccessors` in `AppStartupSeeder`,
  unverified `stateIn`→`MutableStateFlow` change in `PeriodicTableViewModel`. → M5
- `Settings` is a UI-only stub (no DataStore; "data sync" item is non-functional). → M8
- Release build is unsigned and `isMinifyEnabled=false` → not Play-ready (out of scope unless asked).
- No CI (`.github/workflows` absent) and no instrumented `androidTest` (migration/e2e safety only
  covered by JVM SQL-level tests). → deferred backlog.
- Working tree on the perf branch is littered with `artifacts/` outputs and a `.patch` file
  (mostly `.gitignore`d; stray `artifacts/*.txt|*.patch` are not). Cosmetic; not addressed.
