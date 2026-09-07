package com.chemtable.interactive.feature.minigame.reactor.economy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreeEconomyAndroidTest {
    @Test fun durableFakeGrantAndRevocationSurviveAndroidStoreReopen() = runBlocking {
        val file = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
            .resolve("p4-test-${UUID.randomUUID()}.preferences_pb")
        val reward = FixedReward.Entitlement("test-only-durable-content")
        val request = GrantRequest(GrantKey(GrantSource.FAKE_PURCHASE, "receipt-1"), reward)
        val completion = FakePurchaseProvider(
            FakeStorefront(listOf(FakeOffer("fixture", reward))), FakeOutcome.COMPLETED,
        ).request("fixture", request)
        for (phase in 0..2) {
            val job = SupervisorJob()
            try {
                val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
                val ledger = PersistentEconomyLedger(store)
                when (phase) {
                    0 -> assertEquals(GrantResult.APPLIED, ledger.apply(request, completion).result)
                    1 -> {
                        assertTrue(LocalEntitlementRepository(ledger.read()).hasEntitlement(reward.contentId))
                        assertEquals(GrantResult.ALREADY_APPLIED, ledger.apply(request, completion).result)
                        assertEquals(RevocationResult.REVOKED, ledger.revoke(request.key).result)
                    }
                    else -> {
                        assertFalse(LocalEntitlementRepository(ledger.read()).hasEntitlement(reward.contentId))
                        assertEquals(GrantResult.REVOKED, ledger.apply(request, completion).result)
                    }
                }
            } finally {
                job.cancelAndJoin()
            }
        }
    }

    @Test fun unavailableProvidersLeaveFreeAcquisitionUsableOnAndroid() {
        val ledger = LocalEntitlementRepository()
        val reward = FixedReward.Item("test-only-fragment", 2)
        val request = GrantRequest(GrantKey(GrantSource.FAKE_REWARDED, "reward-1"), reward)
        assertEquals(GrantResult.NOT_COMPLETED, ledger.apply(request, FakeRewardedItemProvider().request(request)))
        val purchase = request.copy(key = GrantKey(GrantSource.FAKE_PURCHASE, "purchase-1"))
        val store = FakeStorefront(listOf(FakeOffer("test-only-offer", reward)))
        assertEquals(GrantResult.NOT_COMPLETED, ledger.apply(purchase, FakePurchaseProvider(store).request("test-only-offer", purchase)))
        val free = request.copy(key = GrantKey(GrantSource.FREE_PLAY, "tutorial-1"))
        assertEquals(GrantResult.APPLIED, FreeAcquisition(ledger).claim(free))
        assertEquals(2, ledger.quantity(reward.itemId))
        assertEquals(1, ledger.snapshot().records.size)
    }

    @Test fun fakeEntitlementRestoresInMemoryAndRevocationSurvivesDuplicateCompletion() {
        val reward = FixedReward.Entitlement("test-only-content")
        val request = GrantRequest(GrantKey(GrantSource.FAKE_PURCHASE, "purchase-1"), reward)
        val provider = FakePurchaseProvider(FakeStorefront(listOf(FakeOffer("test-only-offer", reward))), FakeOutcome.COMPLETED)
        val completion = provider.request("test-only-offer", request)
        val ledger = LocalEntitlementRepository()
        assertEquals(GrantResult.APPLIED, ledger.apply(request, completion))
        val recovered = LocalEntitlementRepository(ledger.snapshot())
        assertTrue(recovered.hasEntitlement(reward.contentId))
        assertEquals(GrantResult.ALREADY_APPLIED, recovered.apply(request, completion))
        assertEquals(RevocationResult.REVOKED, recovered.revoke(request.key))
        assertEquals(GrantResult.REVOKED, recovered.apply(request, completion))
        assertFalse(recovered.hasEntitlement(reward.contentId))
    }
}
