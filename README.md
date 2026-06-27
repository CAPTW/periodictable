# ChemTable Interactive

An **offline-first, on-device Android periodic-table learning app** (package
`com.chemtable.interactive`). Kotlin + Jetpack Compose + Material 3 + Hilt + Room,
organized in Clean Architecture layers (`core` / `data` / `domain` / `feature`).
All element / isotope / glossary / recipe data is bundled in the APK and seeded into
Room on first launch — the app works fully **without a network**.

## Features
- **Periodic table** — interactive grid with zoom/pan; tap an element for detail.
- **Search** — by name / symbol / atomic number, plus property-range filters, isotope
  stability/decay filters, and sorting.
- **Element detail** — 7 tabs (overview, atomic/electron, thermal, crystal/3D, NFPA,
  abundance, isotopes), with per-element notes and glossary cross-links. Unknown values
  show **N/A**.
- **Molar-mass calculator** — formula parsing (parentheses, charges, hydrates), history.
- **Notes** — per-element CRUD, editable from element detail.
- **Glossary** — search, detail, and cross-references to/from elements and properties.
- **Visualization** — property heatmaps / bars / comparison; a "Lab" hub.
- **Molecule Making mini-game** — build molecules from elemental blocks via whitelist
  recipes; discovery Dex; 3 difficulties (Beginner/Intermediate/Advanced) × 3 modes
  (Mission/Endless/Time-Attack); persistent high scores.

## Requirements
- **JDK 21** for the build. The Gradle daemon JVM is pinned to the Android Studio bundled
  JBR (JDK 21) via `gradle.properties` (`org.gradle.java.home`), so the system JDK is not
  used. Set `JAVA_HOME` to that JBR if you invoke gradle outside the helper scripts.
- **Android SDK** (compileSdk 35). Provide its path in `local.properties` (`sdk.dir`);
  this file is git-ignored and must not be committed.
- Toolchain: Gradle **9.0**, AGP **8.13.0**, Kotlin **2.1.10**. `minSdk 26` / `targetSdk 35`.

## Build / test / verify
```bash
# Debug APK (install on a device/emulator):
./gradlew assembleDebug            # Windows: .\gradlew.bat assembleDebug

# JVM unit tests (no device required):
./gradlew testReleaseUnitTest

# Release APK (unsigned; minification disabled):
./gradlew assembleRelease

# Full canonical verification gate:
.\scripts\verify.ps1               # Windows (PowerShell)
bash scripts/verify.sh             # Unix
#   = validate_assets.py + gradlew --no-daemon lintRelease testReleaseUnitTest assembleRelease
#   prints "✅ verify PASS" on success.
```
Running the app interactively requires an Android device or emulator. There are currently
no instrumented (`androidTest`) tests; correctness is covered by JVM unit tests under
`app/src/test`.

## Project layout
```
app/src/main/java/com/chemtable/interactive/
  app/            # Application + Hilt DI modules + nav host wiring
  core/           # designsystem (theme/components), model, database (Room), util
  data/           # repository impls, mappers, prepopulate (asset → Room seeding)
  domain/         # repository interfaces, use cases
  feature/        # calculator, elementdetail, glossary, minigame, notes,
                  # periodictable, search, settings, visualization
app/src/main/assets/   # elements.json, glossary.json, isotopes.json (bundled data)
scripts/               # verify, prereqs, env, automation (ralph-loop, swarm)
agent/                 # PRD / ACCEPTANCE / BACKLOG (product spec, single source of truth)
docs/                  # mini-game phase specs & plans
```

## Documentation
- Product spec / acceptance: `agent/PRD.md`, `agent/ACCEPTANCE.md`.
- Developer/agent guide: `AGENTS.md`.
- Autopilot state (on branch `codex/autopilot` / `refs/codex/autopilot`): `Prompt.md`,
  `Plan.md`, `Implement.md`, `Documentation.md`.

## Offline-first & data policy
No network is used on any core path. Unknown/missing data is rendered consistently as
**N/A**. All learning content ships inside the APK.
