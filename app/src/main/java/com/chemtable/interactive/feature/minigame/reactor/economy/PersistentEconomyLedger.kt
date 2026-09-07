package com.chemtable.interactive.feature.minigame.reactor.economy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

internal data class LedgerCommit<T>(val result: T, val snapshot: EconomySnapshot)

/** Durable local/fake transactions only; a Completed value is not payment verification. */
internal class PersistentEconomyLedger(private val store: DataStore<Preferences>) {
    suspend fun read(): EconomySnapshot = decode(store.data.first())

    suspend fun apply(expected: GrantRequest, result: ProviderResult): LedgerCommit<GrantResult> =
        transact { it.apply(expected, result) }

    suspend fun revoke(key: GrantKey): LedgerCommit<RevocationResult> = transact { it.revoke(key) }

    private suspend fun <T : Any> transact(action: (LocalEntitlementRepository) -> T): LedgerCommit<T> {
        var result: T? = null
        val committed = store.updateData { prefs ->
            val before = decode(prefs)
            val ledger = LocalEntitlementRepository(before)
            result = action(ledger)
            val after = ledger.snapshot()
            if (after == before) prefs else prefs.toMutablePreferences().apply {
                this[SNAPSHOT] = EconomySnapshotCodec.encode(after)
            }
        }
        // Return only after durable acknowledgement, including for refund tombstones.
        return LedgerCommit(requireNotNull(result), decode(committed))
    }

    private fun decode(prefs: Preferences): EconomySnapshot {
        val encoded = prefs[SNAPSHOT]
        if (encoded != null) return EconomySnapshotCodec.decode(encoded)
        require(prefs.asMap().isEmpty()) { "Missing economy snapshot" }
        return EconomySnapshot(emptyList())
    }

    companion object {
        val SNAPSHOT = stringPreferencesKey("snapshot")
    }
}
