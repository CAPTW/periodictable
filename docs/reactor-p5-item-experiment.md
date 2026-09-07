# P5 item effect experiment

W-P5-001 implements the first playable P5 slice under OD-20260907-P5-09.
Open Visualization -> Reactor foundation -> Item effect experiment (P5).

## Rules and scope

This is a separate 5x5 experiment board. A and B are abstract game substrates,
not real biochemical families. S is a synthetic substrate for the explicit
negative enzyme case. No biological mechanism or reaction equation is claimed.
Real family selection, teaching content, and final balance remain open in Source06.

Select two cells in order. Catalyst Link combines only those two compatible
pieces, preserving the first cell and freeing the second. Both must be in the
same row or column at Manhattan distance 1 or 2. No line-wide/all-board matching
occurs; intervening pieces are not affected. The resulting single-cell
PolymerBundle holds at most four units. A one-unit piece is reusable feedstock.

Enzyme A acts only on A; enzyme B only on B. Select a bundle of at least two units,
then an empty nearby output cell. Exactly one unit is released, the remainder
stays in the source cell. No enzyme acts on S, including a synthetic-enzyme enum
value supplied directly to the resolver. Synthetic treatment is not implemented.
All fragment counts are conserved. These are reversible gameplay arrangements,
not balanced chemical reactions or a claim that generic catalysts synthesize proteins.

Every successful operation costs one of six free experiment actions. Invalid
geometry, occupied output, incompatible substrate, oversize bundle, exhausted
budget, and invalid indices return the original state and consume nothing.
Free reset restores the whole sample and budget together. Reset does not grant
any durable inventory. Closing/reopening the dialog retains the experiment while
the parent composition exists; leaving the screen/process resets it. No saved
board, persisted spending, score, achievements, or economic reward is claimed.

## Integration boundaries

The entry is wired into the real Reactor screen. The item resolver is pure and
immutable and does not write Classic/Room/settings or the P4 supply ledger. This
first slice does not yet insert bundles into P3 feed/pressure/settling/replay.
Existing P3 board and P4 practice fragment retain their prior behavior. Live integration is now provided by the separate Current Reactor Items entry;
see [P5 live item turns](reactor-p5-live-items.md). This older isolated experiment
retains its original behavior. Scientific-family/final balance choices remain open.
P5 is therefore in progress, not declared fully accepted. No P6/P7, real SDK,
network, schema migration, commit, or publication authority is inferred.

## Verification

JVM coverage checks geometry, compatibility, size and resource boundaries,
fragment conservation, repeat commands, deterministic replay, partial cleavage,
synthetic rejection and defensive state copying. Compose tests perform actual
selection/link/rejection/cleavage/relink/reset and close/reopen at 130% font.
Exact results and runtime/data provenance live only in canonical PROJECT_CONTROL
REVIEW/HANDOFF; this document does not duplicate that authority ledger.
