# ChemTable Interactive

An **offline-first, on-device Android periodic-table learning app** (package
`com.chemtable.interactive`). Kotlin + Jetpack Compose + Material 3 + Hilt + Room,
organized in Clean Architecture layers (`core` / `data` / `domain` / `feature`).
All element, isotope, glossary, and recipe data is bundled in the APK, seeded into
Room on first launch, and available fully **without a network**.

## Features
- **Periodic table**: interactive grid with zoom/pan; tap an element for detail.
- **Search**: by name, symbol, atomic number, property ranges, isotope stability,
  isotope decay filters, and sorting.
- **Element detail**: overview, atomic/electron, thermal, crystal/3D, NFPA,
  abundance, and isotope tabs, with per-element notes and glossary cross-links.
  Unknown values show **N/A**.
- **Molar-mass calculator**: formula parsing for parentheses, charges, hydrates,
  and history.
- **Notes**: per-element CRUD, editable from element detail.
- **Glossary**: search, detail pages, and cross-references to/from elements and
  properties.
- **Visualization**: property heatmaps, bars, comparison, and a lab hub.
- **Molecule Making mini-game**: build molecules from elemental blocks via
  whitelist recipes; discovery Dex; Beginner, Intermediate, and Advanced
  difficulties; Mission, Endless, and Time-Attack modes; persistent high scores.

## Screenshots

| Periodic table | Element detail | Molar-mass calculator |
| :---: | :---: | :---: |
| <img src="docs/screenshots/periodic-table.png" width="230"> | <img src="docs/screenshots/element-detail.png" width="230"> | <img src="docs/screenshots/calculator.png" width="230"> |
| **Glossary** | **Dark theme** | **Solarized theme** |
| <img src="docs/screenshots/glossary.png" width="230"> | <img src="docs/screenshots/theme-dark.png" width="230"> | <img src="docs/screenshots/theme-solarized.png" width="230"> |

## Requirements
- **JDK 21** for the build. Set `JAVA_HOME` to a JDK 21, or set the Gradle JDK
  in Android Studio. The project does not hard-code a machine-specific JDK path.
- **Android SDK** with compileSdk 35. Provide its path in `local.properties`
  (`sdk.dir`); this file is git-ignored and must not be committed.
- Toolchain: Gradle **9.0**, AGP **8.13.0**, Kotlin **2.1.10**. `minSdk 26` /
  `targetSdk 35`.

## Build / Test / Verify
```bash
# Debug APK (install on a device/emulator):
./gradlew assembleDebug            # Windows: .\gradlew.bat assembleDebug

# JVM unit tests (no device required):
./gradlew testReleaseUnitTest

# Release APK (unsigned; minification disabled):
./gradlew assembleRelease

# Full verification gate:
.\scripts\verify.ps1               # Windows (PowerShell)
bash scripts/verify.sh             # Unix
```

Running the app interactively requires an Android device or emulator. There are
currently no instrumented (`androidTest`) tests; correctness is covered by JVM
unit tests under `app/src/test`.

## Project Layout
```
app/src/main/java/com/chemtable/interactive/
  app/            # Application + Hilt DI modules + nav host wiring
  core/           # design system, model, database, utilities
  data/           # repository implementations, mappers, asset-to-Room seeding
  domain/         # repository interfaces and use cases
  feature/        # calculator, element detail, glossary, mini-game, notes,
                  # periodic table, search, settings, visualization
app/src/main/assets/   # elements.json, glossary.json, isotopes.json
scripts/               # verification, prerequisite, and environment helpers
docs/                  # screenshots, phase notes, and runtime checklists
```

## Documentation
- Screenshots: `docs/screenshots/`.
- Mini-game planning notes and runtime checklists: `docs/`.
- Build verification: `scripts/verify.ps1` and `scripts/verify.sh`.

## Offline-First & Data Policy
No network is used on any core path. Unknown or missing data is rendered
consistently as **N/A**. All learning content ships inside the APK.
