package com.chemtable.interactive.feature.minigame.reactor.economy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reactorEconomyStore by preferencesDataStore(name = "reactor_economy")

internal data class ReactorSupplyAccount(val quantity: Int, val claimed: Boolean)

/** The starter reward is a prototype, not a spending currency or gameplay advantage. */
internal object StarterSupply {
    val reward = FixedReward.Item("practice-fragment", 1)
    val request = GrantRequest(GrantKey(GrantSource.FREE_PLAY, "starter-supply-v1"), reward)
}

@Singleton
class ReactorSupplyRepository internal constructor(private val store: DataStore<Preferences>) {
    @Inject constructor(@ApplicationContext context: Context) : this(context.reactorEconomyStore)
    private val ledger = PersistentEconomyLedger(store)

    internal suspend fun read(): ReactorSupplyAccount = account(ledger.read())

    internal suspend fun claim(): ReactorSupplyAccount {
        val committed = ledger.apply(StarterSupply.request, ProviderResult.Completed(StarterSupply.request))
        when (committed.result) {
            GrantResult.APPLIED, GrantResult.ALREADY_APPLIED, GrantResult.REVOKED -> Unit
            else -> error("Starter grant conflict")
        }
        return account(committed.snapshot)
    }

    private fun account(snapshot: EconomySnapshot): ReactorSupplyAccount {
        val starter = snapshot.records.singleOrNull { it.request.key == StarterSupply.request.key }
        require(starter == null || starter.request == StarterSupply.request) { "Starter receipt mismatch" }
        val ledger = LocalEntitlementRepository(snapshot)
        return ReactorSupplyAccount(
            quantity = ledger.quantity(StarterSupply.reward.itemId),
            claimed = starter != null,
        )
    }

    internal companion object {
        val SNAPSHOT = PersistentEconomyLedger.SNAPSHOT
    }
}
