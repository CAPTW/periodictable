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

## Screenshots

| Periodic table | Element detail | Molar-mass calculator |
| :---: | :---: | :---: |
| <img src="docs/screenshots/periodic-table.png" width="230"> | <img src="docs/screenshots/element-detail.png" width="230"> | <img src="docs/screenshots/calculator.png" width="230"> |
| **Glossary** | **Dark theme** | **Solarized theme** |
| <img src="docs/screenshots/glossary.png" width="230"> | <img src="docs/screenshots/theme-dark.png" width="230"> | <img src="docs/screenshots/theme-solarized.png" width="230"> |

## Requirements
- **JDK 21** for the build. Set `JAVA_HOME` to a JDK 21 (e.g. Android Studio's bundled JBR at
  `.../Android Studio/jbr`), or set the Gradle JDK in Android Studio. The project no longer
  hard-codes a machine-specific JDK path, so it builds on any OS / install location.
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
scripts/               # verify, prereqs, env, and local automation helpers
docs/product/          # PRD / ACCEPTANCE / BACKLOG product planning docs
docs/                  # screenshots, mini-game phase specs, plans, and guides
```

## Documentation
- Product spec / acceptance: `docs/product/PRD.md`, `docs/product/ACCEPTANCE.md`,
  `docs/product/BACKLOG.md`.
- Developer guide: `docs/developer-guide.md`.

## Offline-first & data policy
No network is used on any core path. Unknown/missing data is rendered consistently as
**N/A**. All learning content ships inside the APK.
