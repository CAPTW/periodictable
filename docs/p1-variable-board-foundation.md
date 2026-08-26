# P1 variable-board foundation

The Classic molecule game supports exactly three independently selectable board sizes: 4×4, 5×5, and 6×6. Mode and difficulty do not choose or override the board size. A new installation and an unknown or corrupt stored value both fall back to 4×4.

## Behavior and persistence

- The Intro screen restores the locally stored preferred size and saves explicit changes immediately.
- Starting or restarting a game uses the selected size. Active board state is not persisted.
- The six Classic recipes, merge rules, mass sorting, score calculation, missions, and difficulty tuning are unchanged.
- Rendering uses the smaller of the available width and height so every board remains square. The 6×6 presentation compacts visible cell text while retaining full occupied-cell semantics.

## Stored game data

Room schema version 7 adds a non-null `boardSize` column to `game_sessions` with a default of `4` and an index for score queries. Migration 6→7 is additive: existing sessions remain in place and are classified as 4×4. New sessions store their actual board dimension.

High scores are queried within the selected board size. Molecule discovery and the recent-session feed remain global; recent sessions expose their own board-size label. The Molecule Dex board-size selector changes the score scope without hiding discoveries.

## Verification surface

JVM tests cover the typed size model, square board validation, dynamic engine behavior, preference fallback and selection ordering, board-scoped scores, Dex filtering, and migration SQL. Focused Android tests cover 16/25/36-cell Compose boards, selector and compact-cell semantics, one-gesture/one-move dispatch, and a real SQLite 6→7 migration with preserved rows.

This foundation does not implement Reactor, single-step settling, new items or recipes, board-size rebalancing, ads, billing, backend services, or networking.
