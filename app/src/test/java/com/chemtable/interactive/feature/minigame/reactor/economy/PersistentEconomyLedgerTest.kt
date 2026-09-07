package com.chemtable.interactive.feature.minigame.reactor.economy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersistentEconomyLedgerTest {
    @get:Rule val temp = TemporaryFolder()
    private val item = GrantRequest(GrantKey(GrantSource.FAKE_REWARDED, "reward-1"), FixedReward.Item("fixture", 2))
    private val pack = GrantRequest(GrantKey(GrantSource.FAKE_PURCHASE, "pack-1"), FixedReward.Entitlement("fixture-pack"))

    @Test fun fakeRewardAndEntitlementRestoreAndRevocationRemainsFinalAcrossReopen() = runBlocking {
        val file = temp.newFolder().resolve("ledger.preferences_pb")
        withStore(file) { store ->
            val ledger = PersistentEconomyLedger(store)
            assertEquals(GrantResult.APPLIED, ledger.apply(item, FakeRewardedItemProvider(FakeOutcome.COMPLETED).request(item)).result)
            val purchase = FakePurchaseProvider(FakeStorefront(listOf(FakeOffer("pack", pack.reward))), FakeOutcome.COMPLETED)
            assertEquals(GrantResult.APPLIED, ledger.apply(pack, purchase.request("pack", pack)).result)
        }
        withStore(file) { store ->
            val ledger = PersistentEconomyLedger(store)
            val local = LocalEntitlementRepository(ledger.read())
            assertEquals(2, local.quantity("fixture"))
            assertTrue(local.hasEntitlement("fixture-pack"))
            assertEquals(GrantResult.ALREADY_APPLIED, ledger.apply(pack, ProviderResult.Completed(pack)).result)
            assertEquals(RevocationResult.REVOKED, ledger.revoke(pack.key).result)
        }
        withStore(file) { store ->
            val ledger = PersistentEconomyLedger(store)
            assertFalse(LocalEntitlementRepository(ledger.read()).hasEntitlement("fixture-pack"))
            assertEquals(GrantResult.REVOKED, ledger.apply(pack, ProviderResult.Completed(pack)).result)
            assertEquals(RevocationResult.ALREADY_REVOKED, ledger.revoke(pack.key).result)
            assertEquals(2, LocalEntitlementRepository(ledger.read()).quantity("fixture"))
        }
    }

    @Test fun cancelledPendingUnavailableAndMismatchedCallbacksDoNotCreateAReceipt() = runBlocking {
        val file = temp.newFolder().resolve("ledger.preferences_pb")
        withStore(file) { store ->
            val ledger = PersistentEconomyLedger(store)
            for (outcome in FakeOutcome.entries.filter { it != FakeOutcome.COMPLETED }) {
                assertEquals(GrantResult.NOT_COMPLETED, ledger.apply(item, FakeRewardedItemProvider(outcome).request(item)).result)
            }
            assertEquals(GrantResult.MISMATCH, ledger.apply(item, ProviderResult.Completed(pack)).result)
            assertEquals(RevocationResult.UNKNOWN, ledger.revoke(pack.key).result)
            assertTrue(ledger.read().records.isEmpty())
            assertFalse(file.exists())
            // Free progression remains possible when all optional providers fail.
            val free = StarterSupply.request
            assertEquals(GrantResult.APPLIED, ledger.apply(free, ProviderResult.Completed(free)).result)
        }
    }

    @Test fun conflictCannotRewriteADurableGrant() = runBlocking {
        val file = temp.newFolder().resolve("ledger.preferences_pb")
        withStore(file) { store ->
            val ledger = PersistentEconomyLedger(store)
            ledger.apply(item, ProviderResult.Completed(item))
            val bytes = file.readBytes()
            val changed = item.copy(reward = FixedReward.Item("fixture", 99))
            assertEquals(GrantResult.CONFLICT, ledger.apply(changed, ProviderResult.Completed(changed)).result)
            assertTrue(bytes.contentEquals(file.readBytes()))
        }
    }

    @Test fun failedRevocationDoesNotRemoveThePersistedEntitlement() = runBlocking {
        val file = temp.newFolder().resolve("ledger.preferences_pb")
        withStore(file) { real ->
            PersistentEconomyLedger(real).apply(pack, ProviderResult.Completed(pack))
            val failing = object : DataStore<Preferences> {
                override val data = real.data
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    real.updateData { current ->
                        transform(current)
                        throw IOException("Synthetic failure before durable write")
                    }
            }
            var failed = false
            try { PersistentEconomyLedger(failing).revoke(pack.key) } catch (_: IOException) { failed = true }
            assertTrue(failed)
        }
        withStore(file) { store ->
            assertTrue(LocalEntitlementRepository(PersistentEconomyLedger(store).read()).hasEntitlement("fixture-pack"))
        }
    }

    private suspend fun withStore(file: File, block: suspend (DataStore<Preferences>) -> Unit) {
        val job = SupervisorJob()
        try {
            block(PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file })
        } finally {
            job.cancelAndJoin()
        }
    }
}
