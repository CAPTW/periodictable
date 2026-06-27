# Implement.md — Autopilot Runbook

Source of truth: `Plan.md` (milestones) + `Prompt.md` (Done-When).
This runbook tells each autonomous run HOW to make safe, durable progress.

## Durable state model (read this first every run)
This automation may start in a fresh worktree/environment, so state lives in the repo:
- Memory files at repo root: `Prompt.md`, `Plan.md`, `Implement.md`, `Documentation.md`.
- Git pointer `refs/codex/autopilot` = the last completed autopilot commit.
- A review branch `codex/autopilot` points at the same commit (created, not auto-checked-out).

### Start of run
1. Confirm this is a git repo. Determine `MAIN_HEAD` (branch `main`; this repo has no `origin`).
2. If `refs/codex/autopilot` is missing → create it at `MAIN_HEAD`.
   If it exists and `MAIN_HEAD` has advanced past it (autopilot is an ancestor) →
   fast-forward `refs/codex/autopilot` to `MAIN_HEAD`.
3. Base your work on `refs/codex/autopilot` (a clean checkout of that commit).
   **Important:** the live working tree may sit on a *different* feature branch with
   uncommitted work (e.g. `codex/perf-profileable-measurement`). Do **not** disturb it.
   Prefer an isolated `git worktree` off `refs/codex/autopilot`/`main` for build+commit,
   then point the refs at the new commit and remove the worktree. (Copy the git-ignored
   `local.properties` into the worktree so the Android SDK path resolves.)

### During the run — pick exactly ONE milestone
- From `Plan.md`, take the smallest still-unchecked `[ ]` milestone, priority order:
  (1) build/test/run pipeline, (2) doc/run-flow mismatches, (3) feature gaps/bugs,
  (4) refactors only in service of 1–3.
- Make the **minimal** change. Don't touch many files. Keep the diff reviewable.
- Then run that milestone's **validation commands** (see below). If they fail:
  diagnose → minimal fix → re-run. Never "ignore and move on".

### Validation commands (this project)
- Toolchain sanity (no device): `./gradlew --version`, `./gradlew help`
- Unit tests (no device, JVM): `./gradlew testReleaseUnitTest`  (or a single class via `--tests`)
- Release assembly (exercises proguard/release path): `./gradlew assembleRelease`
- Canonical full gate: `.\scripts\verify.ps1` (Windows) / `bash scripts/verify.sh`
  = `validate_assets.py` + `gradlew --no-daemon lintRelease testReleaseUnitTest assembleRelease`
- Build env: set `JAVA_HOME` to the Android Studio JBR
  (`C:\Program Files\Android\Android Studio\jbr`, JDK 21) as `verify.ps1` does, or rely on
  `gradle.properties` `org.gradle.java.home`. Do **not** depend on the system JDK (25).
- Instrumented `androidTest` tests do **not** exist and require a device/emulator — out of
  scope for headless validation; note this when a milestone can only be fully checked on-device.

### End of run — save durably
1. Update `Documentation.md`: what changed, commands run + results, next milestone.
2. In `Plan.md`: check off the completed milestone `[x]`; split the next one smaller if needed.
3. Commit (logical 1–3 commits, `feat`/`test`/`docs`; or 1 if small).
   Message format: `codex: <milestone title>`.
4. Move `refs/codex/autopilot` to the new HEAD; ensure branch `codex/autopilot` matches.

## Scope guardrails
- One milestone per run. No opportunistic refactors. No new deps without justification.
- Offline-first is non-negotiable — never add a network dependency to a core path.
- Unknown data → "N/A" (consistent null-safety policy).
- Leave the working tree clean (no stray build artifacts committed; respect `.gitignore`).

## Termination (COMPLETE)
- When every `Prompt.md` Done-When box is checked and every `Plan.md` milestone is `[x]`
  and the canonical verify is green:
  - If `Documentation.md` already has a "COMPLETE" section → make **no changes**, just report.
  - Else → append a one-time "COMPLETE" section (with final run/verify commands) and commit.
- After COMPLETE, every subsequent run is a **no-op** (report "already COMPLETE", change nothing).
