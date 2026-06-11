# Mini-game Phase 3A Persistence Plan

## 1. Scope

Phase 3A adds local persistence for mini-game progress and result history.

Included:
- Discovered molecule dex: locally persisted formula list for molecules/compounds the player has made.
- High score: highest score across completed game sessions, with room for per-difficulty expansion.
- Discovery count: how many times each molecule formula has been created.
- Recent game results: completed game sessions with score, success state, mission context, and made molecules.

Excluded:
- Cloud sync.
- User account.
- Sharing.
- Multiplayer.
- Discovery Dex artwork, badges, achievements, or external profile features.
- Phase 3 persistence beyond mini-game progress.

## 2. Storage Decision

Decision: use Room only for Phase 3A.

Rationale:
- The app already uses Room for bundled/offline data and user-owned local data.
- The requested data is relational and queryable: discovery rows, counts, recent sessions, high score, and future per-difficulty stats.
- DataStore is not currently wired into the app and would require a new dependency plus DI/test setup.
- A hybrid Room/DataStore approach would split mini-game progress across two persistence systems without enough benefit at this stage.

Tutorial seen state can remain out of Phase 3A or be stored later in a small Room-backed profile/stats table if persistence becomes required.

## 3. Data Model Draft

### GameMoleculeDiscoveryEntity

Purpose: one row per discovered molecule formula.

Fields:
- `formula: String`
  - Primary key.
  - Canonical formula from the mini-game recipe/product model.
- `discoveryCount: Int`
  - Total number of times the formula has been created.
- `firstDiscoveredAt: Long`
  - Epoch millis when the formula was first recorded.
- `lastDiscoveredAt: Long`
  - Epoch millis when the formula was most recently recorded.

Indexes:
- Primary key on `formula`.
- Optional index on `lastDiscoveredAt` for recent discoveries.
- Optional index on `discoveryCount` for most-discovered sorting.

Notes:
- Do not persist localized display names as source of truth.
- UI can derive names/details from RecipeBook, formula helpers, glossary links, or element links.

### GameSessionEntity

Purpose: one row per completed mini-game session/result.

Fields:
- `id: Long`
  - Auto-generated primary key.
- `score: Int`
  - Final score.
- `success: Boolean`
  - Whether the mission was completed.
- `difficulty: String`
  - Store enum name as string for future compatibility.
- `missionFormula: String?`
  - Mission target formula if present.
- `missionTarget: Int`
  - Target count for the mission.
- `missionProgress: Int`
  - Progress at session end.
- `playedAt: Long`
  - Epoch millis when the result was recorded.
- `moleculesMade: List<String>`
  - Formula list from the completed session.
  - Existing Room type converter patterns can be reused if suitable.

Indexes:
- Index on `playedAt` for recent results.
- Index on `score` for high score queries.
- Optional composite index on `difficulty, score` for per-difficulty high score.

Alternative deferred model:
- A normalized `GameSessionMoleculeEntity(sessionId, formula, count)` can be added later if per-session molecule breakdown queries become more important than a compact session summary.

## 4. DAO Draft

Discovery DAO responsibilities:
- `observeDiscoveredMolecules`
  - Return discovered formulas sorted by `lastDiscoveredAt` or formula.
- `observeDiscovery(formula)`
  - Return one discovered formula row.
- `upsertDiscovery`
  - Insert a first discovery or increment count and update `lastDiscoveredAt`.
- `observeDiscoveryCount`
  - Return total unique discovered molecule count.
- `clearDiscoveries`
  - Debug/reset path only if product scope approves.

Session/stat DAO responsibilities:
- `insertGameSession`
  - Persist one completed result.
- `observeRecentGameSessions`
  - Return recent sessions by `playedAt DESC` with a limit.
- `observeHighScore`
  - Return max score across all sessions.
- `observeHighScoreByDifficulty`
  - Return max score for a difficulty string.
- `clearStats`
  - Debug/reset path only if product scope approves.

Transaction requirement:
- Recording a completed session should insert the session and update discovery rows in one transaction so session history and dex progress stay consistent.

## 5. Repository/UseCase Draft

Repository:
- `GameStatsRepository`
  - `observeDiscoveredMolecules()`
  - `observeRecentGameResults(limit)`
  - `observeHighScore()`
  - `observeHighScore(difficulty)`
  - `recordGameResult(record)`
  - Optional `clearStats()` only if a reset UX is approved.

Domain models:
- `DiscoveredMoleculeProgress`
  - Formula, discovery count, first discovered timestamp, last discovered timestamp.
- `GameResultRecord`
  - Score, success, difficulty, mission fields, played timestamp, made molecule formulas.
- `GameResultSummary`
  - UI-facing recent session summary.

Use cases:
- `RecordGameResultUseCase`
  - Called once when a mini-game session reaches the result phase.
- `GetDiscoveredMoleculesUseCase`
  - Powers Discovery Dex and intro summary.
- `GetHighScoreUseCase`
  - Powers game intro/result high score display.
- `GetRecentGameResultsUseCase`
  - Powers recent results UI.
- Optional `ClearGameStatsUseCase`
  - Only if reset/debug UX is explicitly approved.

## 6. Migration Plan

Current DB version: `4`.

Next DB version: `5`.

Migration scope:
- Add mini-game persistence tables only.
- Do not modify existing element, isotope, note, glossary, or calculator history tables.
- Do not involve asset prepopulation or seed data for game progress.

Migration statements draft:
- Create `game_molecule_discoveries`.
- Create index on `game_molecule_discoveries(last_discovered_at)`.
- Create index on `game_molecule_discoveries(discovery_count)`.
- Create `game_sessions`.
- Create index on `game_sessions(played_at)`.
- Create index on `game_sessions(score)`.
- Create optional index on `game_sessions(difficulty, score)`.

Test plan:
- Verify a fresh DB creates both new tables.
- Verify migration from version 4 to 5 preserves existing data.
- Verify DAO queries return empty state on upgraded users with no game history.
- Verify recording a session after migration inserts session and discovery rows.

## 7. UI Integration Plan

Lab card:
- Add a Discovery Dex card in the Lab/Visualization hub near the existing mini-game entry.
- Show a compact discovered count when available.

Result overlay:
- Add a secondary "Dex" entry point after a game ends.
- Keep calculator CTA, element chips, and glossary chips unchanged.
- Avoid adding another per-molecule row action that would overcrowd result rows.

Dex screen:
- Show discovered molecules sorted by recent discovery by default.
- Include formula, discovery count, and optional last discovered date.
- Reuse existing molecule formula rendering and existing calculator/element/glossary navigation patterns where appropriate.

Game intro summary:
- Show compact high score and discovered count.
- Keep it secondary to the start-game action.

## 8. Implementation Phases

### 3A.1 data layer
- Add Room entities for molecule discoveries and game sessions.
- Add DAO methods and transactional record operation.
- Add DB version 5 migration.
- Wire DAO provider in the database DI module.

### 3A.2 domain/usecase
- Add domain repository interface and data implementation.
- Add mapper functions between Room entities and domain models.
- Add use cases for record result, discovered molecules, high score, and recent results.
- Bind repository implementation in DI.

### 3A.3 ViewModel record result
- Inject `RecordGameResultUseCase` into the mini-game ViewModel.
- Record only when transitioning into the result phase.
- Add a guard so the same completed session is not saved more than once.
- Preserve existing calculator, element detail, glossary, action sheet, and result overlay behavior.

### 3A.4 Dex UI
- Add Discovery Dex route/screen after data layer is stable.
- Add Lab card entry point.
- Optionally add result overlay and game intro summary entry points.

### 3A.5 tests/smoke
- Add DAO, repository, migration, and ViewModel tests where supported.
- Run existing mini-game, calculator route/prefill, element link, and glossary link tests.
- Run runtime smoke for persistence across app restart.

## 9. Test Plan

DAO tests:
- Empty DB returns empty discoveries and no high score.
- Inserting a session persists score, success state, difficulty, mission fields, and made formulas.
- Upserting a first discovery sets count to 1 and both timestamps.
- Re-discovering a formula increments count and updates `lastDiscoveredAt` while preserving `firstDiscoveredAt`.
- Recent sessions are ordered by newest first.
- High score returns the max score.

Migration tests:
- Version 4 to 5 creates new tables and indexes.
- Existing rows in pre-existing tables remain readable.
- New tables are empty after migration for existing users.

Repository tests:
- `recordGameResult` inserts one session and updates all discovery counts.
- Duplicate formulas in one result update counts according to the intended rule.
- Multiple sessions accumulate discovery counts and high score correctly.

ViewModel result recording tests:
- Result transition calls recording once.
- Recomposition/state collection does not duplicate records.
- Restarting a game clears session-local guard state for the next result.
- Failed and successful results are both recorded if they reach result phase.

Runtime smoke:
- Complete a mini-game and see high score/discovery count update.
- Restart app and verify persisted values remain.
- Open Discovery Dex and verify created formulas appear.
- Confirm calculator CTA, element chips, glossary chips, action sheet, and 5-tab navigation still work.

## 10. Risks

Migration:
- DB version bump affects release users.
- Migration tests are important because existing migration coverage is limited.

Duplicate records:
- Result phase can remain visible for a while.
- Recording must happen once per completed session, not on every state emission.

Formula identity:
- Formula strings must remain canonical.
- If future recipes introduce aliases, a normalization strategy may be needed.

Display names/localization:
- Persisting display names can drift from current recipe/glossary data.
- Prefer deriving labels at display time.

Future recipes:
- New recipes should automatically appear in the dex after discovery.
- Dex completeness totals should come from RecipeBook or a dedicated catalog, not hardcoded persistence rows.
