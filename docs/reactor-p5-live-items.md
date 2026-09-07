# P5 live Reactor item turns

W-P5-002 follows direct OD-20260907-P5-10. The current Reactor session now accepts
item commands as well as swipes. Open the Reactor, scroll past its core controls,
and select Current Reactor Items. The grid in that dialog is the live session
board, not a copy. P5 sample reset explicitly replaces the active board and resets
turn/feed/pressure/free resources; the normal sample reset still restores exact P3.

A/B/S remain abstract game substrates. A neutral single-cell ReactorPolymerEntity
holds 1..4 units; @A/@B/@SYNTHETIC compositions cannot reach ordinary chemistry
recipes. Its mass field is a nonphysical sentinel, presented as N/A in details and
semantics. Neutral index0 is a gameplay rule, not a physical settling assertion.
Existing real element/molecule rules and default feed sequence are unchanged.

A valid item turn applies the bounded W-P5-001 two-target rule, performs exactly
one odd/even settling tick, advances turn/phase once, then calls the same P3 feed
and pressure tail used by swipes. It consumes one of six free session actions.
Linking preserves the first entity ID and releases the second cell; cleavage
preserves the remaining bundle ID and creates a deterministic fragment ID at the
chosen empty destination. Selected output positions may subsequently move during
settling. Existing feed may occupy the newly freed space. No automatic chemical
merge/compression occurs on an item turn; ordinary swipes still compress/merge.

Invalid geometry/substrate/capacity/output, insufficient actions, unsupported
board/turn, bad prior counters, and overflow reject before session publication.
The board, cursor, pressure and resources stay unchanged; the UI reports the error
and does not claim replay success. Overflow locks item buttons as well as swipes.
Existing free vent recovers the board without replenishing item resources.
Free sample reset restores resources only with a reset of the entire sample.
Neither action spends or grants the persistent P4 practice fragment.

ItemApplied records the external command, prior turn/resource count, resulting
resource count and effect-stage board. The P5 validator takes prior board,
command, feed/operational context and budget from the caller. It deterministically
recomputes the effect/tick and compares the complete result, then uses the P3
replayer to reconstruct the settling/feed/pressure continuation. Forged receipts,
costs, effect boards, reordered/omitted events, cursors and phase are rejected.
P2-only replay explicitly rejects item receipts. This is layered deterministic
self-verification, not an independent implementation or security proof.

The session publishes only after validation. A following swipe uses the same live
board/counters. Reset keeps P3 behavior and clears the transient event log; app
restart does not restore the board. No Room/settings writes, ledger format change,
SDK/network, P6/P7 feature, actual biochemical family or final catalog/balance is
introduced. The earlier isolated P5 experiment remains accessible for comparison.

Verification and exact source/APK identities are in canonical PROJECT_CONTROL
REVIEW/HANDOFF for P5LIVE-20260907T201041. Commands use the earlier isolated build
init script; no cleanup of main build output or empty legacy resource folder.


## Learning and session recharge (W-P5-005)
Enzyme guidance now explains substrate matching, one-unit separation, conservation
and relinking; it explicitly warns that cleavage costs space and is not pressure
relief. Successful item use reports the observed learning step, not a mission,
achievement, learning certification or gameplay reward.

While fewer than six actions remain, three validated ordinary swipe turns make
one free action claimable. Progress caps at three; claim resets progress, adds one
(up to six), and leaves board, turn, feed, pressure, event receipt and P4 inventory
unchanged. Item turns, free vents, rejected swipes and swipes at full capacity do
not earn progress. Claim is repeat-safe. Reset clears progress with the whole
sample; this session-only state is not restored after leaving/restarting. The
three-turn rate is a reversible prototype choice, not final accepted balance.
