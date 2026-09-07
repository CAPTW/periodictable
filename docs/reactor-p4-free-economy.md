# P4 free acquisition and local restoration

The Reactor screen opens **무료 보급 · 보관함**. Its starter supply grants one
practice fragment once per retained app data. The dialog shows the exact quantity,
claim status, read/save errors and retry. Practice fragments have no spending or
board effect yet; this is not a final item catalog or earning-rate balance.
The game and free emergency vent remain usable without a claim or functioning store.

## Durable free path

`ReactorSupplyViewModel` loads the account and calls `ReactorSupplyRepository`.
The repository shares `PersistentEconomyLedger` with the fake provider adapters.
It uses the existing Preferences DataStore dependency (1.1.1), in the
separate `files/datastore/reactor_economy.preferences_pb` file. It does not use the
settings DataStore or Classic Room database. There is no existing-data migration.

The stable key is `FREE_PLAY/starter-supply-v1`, for `practice-fragment`, quantity 1.
Navigation, sample reset and process restart never generate a fresh key. Within one
serialized `updateData` operation, the repository reconstructs the ledger, applies
the grant and persists its full snapshot. Quantities are derived from receipts,
not stored independently. The UI receives success only after `updateData` returns.
An interrupted acknowledgement can therefore retry the same key without a second grant.

This relies on the [DataStore atomic update contract](https://developer.android.com/reference/androidx/datastore/core/DataStore).
The application uses a single delegate per file, in one Android process. Tests use
isolated files and close each DataStore scope before reopening the same file.

`EconomySnapshotCodec` version 1 contains source/event IDs, exact item or entitlement,
quantity and revocation status, in a Base64-encoded binary record stream. It rejects
unknown versions/types, duplicate keys, invalid quantities, trailing/truncated data,
and balances above Int.MAX_VALUE. This small prototype store is capped at 256 records
and 1 MiB encoded; exceeding a cap fails before committing. Broader progression may
need a separately reviewed storage evolution. No silent truncation or reset occurs.

Missing file/empty preferences means a new account. Existing undecodable data, or
nonempty preferences missing the snapshot, raises an error. No corruption replacement
handler erases records. A retry reads the durable account again; a failed write must
not be interpreted as a successful award. Data deletion/uninstall is not recovered
by this feature; no cloud/account restore is implemented. The Reactor board itself
still resets on a new process, independently of the restored supply account.

## Fake provider and ledger boundary

`FakeRewardedItemProvider` and `FakePurchaseProvider` default to unavailable.
Completion requires an explicit fake outcome. Purchases require a matching unique
fake offer and exact reward. `FakeStorefront` defaults to an empty catalog, with no
prices or SDK. These optional fakes are not exposed as payment/ad buttons in the UI.
The live starter path submits its fixed FREE_PLAY request to the durable ledger and
never consults a provider. `FreeAcquisition` remains the pure in-memory free-path helper.

`LocalEntitlementRepository` remains the in-memory validator. Completed requests
are idempotent; conflicting payloads or mismatched callbacks cannot grant. Pending,
cancelled, unavailable and rejected outcomes consume no key. Revocation retains a
tombstone and subtracts the grant; other active grants preserve an entitlement.
`PersistentEconomyLedger` persists both fake completed grants and revocations using
the same atomic snapshot transaction as the starter path. It returns the outcome
and committed snapshot only after durable acknowledgement. Rejected/pending/mismatched
results and unknown revocations do not write a receipt. Domain snapshots alone are
not proof of durable storage. The app UI exposes only the free starter path.

## Threat and policy review

- Local fake completion and imported snapshots are trusted inputs, not proof of an
  ad view/payment. No authentication, purchase verifier or tamper resistance is claimed.
- No network/SDK/backend, new permission, secret or personal-data access is added.
- Grant validation and durable updates are one serialized transaction. No optimistic
  balance or mutable ledger is cached by the repository.
- Unknown-key revocation remains `UNKNOWN` without a tombstone. Out-of-order refunds,
  spending, refund-after-spend, multi-process writers and cloud restore are unimplemented.
- Existing Classic engine, scoring, Room schema and settings remain separate.

## Validation and remaining scope

JVM tests exercise actual DataStore file reopen/concurrent claims, failed writes,
lost acknowledgements, corrupt records and codec boundaries. Compose tests cover
130% font, one claim, busy/error/retry states and leaving the dialog. Runtime acceptance
must include offline claim, force-stop/relaunch, unchanged balance/claim status,
sample-reset isolation and before/after Classic logical data.

Game-effect earning/spending balance and richer content-specific free triggers are
reviewed with the later content phases. P4 closes only the foundation scope below;
P5 and real providers retain their separate execution gates.


## P4 closure boundary

P4 foundation covers the offline free starter, exact fake reward/store/purchase
contracts, local inventory/entitlements, durable grants/revocations, duplicate and
failure handling, and independence from the board/Classic/real providers. The
prototype earning rate is explicit: one fragment once per retained app data, zero
paid advantage, no forced waiting and no currency cost for core play or emergency
vent. This is a bounded foundation balance, not a final content economy.

The source roadmap puts catalyst/item effects in P5 and richer collection/catalog
in P6. Their prices, recipes, spending balance and further earning triggers are not
silently established by closing P4. Neither local integration nor these tests
approves Ads, Billing, backend, publication, or execution of the next phase.

The durable fake-provider matrix includes real-file reopen after reward/purchase,
revocation across another reopen, duplicate refusal, exact-payload conflict,
non-completion without receipts, and rollback when revocation persistence fails.
