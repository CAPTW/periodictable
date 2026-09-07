package com.chemtable.interactive.feature.minigame.reactor.economy

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeEconomyTest {
    private val item = FixedReward.Item("test-fragment", 3)
    private fun request(
        id: String = "tutorial-1",
        source: GrantSource = GrantSource.FREE_PLAY,
        reward: FixedReward = item,
    ) = GrantRequest(GrantKey(source, id), reward)

    @Test fun freeClaimWorksWithoutAnyProvidersAndDuplicateDoesNotGrantTwice() {
        val ledger = LocalEntitlementRepository()
        val free = FreeAcquisition(ledger)
        assertEquals(GrantResult.APPLIED, free.claim(request()))
        repeat(10) { assertEquals(GrantResult.ALREADY_APPLIED, free.claim(request())) }
        assertEquals(3, ledger.quantity(item.itemId))
        assertEquals(1, ledger.snapshot().records.size)
    }

    @Test fun freePathCannotPretendToCompleteAPurchase() {
        val ledger = LocalEntitlementRepository()
        assertEquals(GrantResult.MISMATCH, FreeAcquisition(ledger).claim(request(source = GrantSource.FAKE_PURCHASE)))
        assertTrue(ledger.snapshot().records.isEmpty())
    }

    @Test fun rewardedCompletionGrantsExactlyRequestedItem() {
        val ledger = LocalEntitlementRepository()
        val request = request(source = GrantSource.FAKE_REWARDED)
        val result = FakeRewardedItemProvider(FakeOutcome.COMPLETED).request(request)
        assertEquals(ProviderResult.Completed(request), result)
        assertEquals(GrantResult.APPLIED, ledger.apply(request, result))
        assertEquals(3, ledger.quantity(item.itemId))
    }

    @Test fun everyNonCompletionLeavesLedgerUnchangedAndAllowsLaterRetry() {
        val request = request(source = GrantSource.FAKE_REWARDED)
        for (outcome in FakeOutcome.entries.filter { it != FakeOutcome.COMPLETED }) {
            val ledger = LocalEntitlementRepository()
            val before = ledger.snapshot()
            val result = FakeRewardedItemProvider(outcome).request(request)
            assertEquals(GrantResult.NOT_COMPLETED, ledger.apply(request, result))
            assertEquals(before, ledger.snapshot())
            assertEquals(GrantResult.APPLIED, ledger.apply(request, ProviderResult.Completed(request)))
        }
    }

    @Test fun providersDefaultToUnavailableAndNeverAutogrant() {
        val rewardRequest = request(source = GrantSource.FAKE_REWARDED)
        val purchaseRequest = request(source = GrantSource.FAKE_PURCHASE)
        assertEquals(ProviderResult.Unavailable, FakeRewardedItemProvider().request(rewardRequest))
        assertEquals(ProviderResult.Unavailable, FakePurchaseProvider(store()).request("sample", purchaseRequest))
    }

    @Test fun rewardedRejectsWrongSourceAndNonItemRewards() {
        val provider = FakeRewardedItemProvider(FakeOutcome.COMPLETED)
        assertEquals(ProviderResult.Rejected, provider.request(request()))
        assertEquals(ProviderResult.Rejected, provider.request(request(
            source = GrantSource.FAKE_REWARDED, reward = FixedReward.Entitlement("test-content"),
        )))
    }

    @Test fun purchaseRequiresKnownOfferAndExactRewardAndSource() {
        val provider = FakePurchaseProvider(store(), FakeOutcome.COMPLETED)
        val request = request(source = GrantSource.FAKE_PURCHASE)
        assertEquals(ProviderResult.Completed(request), provider.request("sample", request))
        assertEquals(ProviderResult.Rejected, provider.request("missing", request))
        assertEquals(ProviderResult.Rejected, provider.request("sample", request.copy(reward = item.copy(quantity = 4))))
        assertEquals(ProviderResult.Rejected, provider.request("sample", request()))
    }

    @Test fun purchaseFailureMatrixNeverChangesBalancesOrBlocksFreeClaim() {
        for (outcome in FakeOutcome.entries.filter { it != FakeOutcome.COMPLETED }) {
            val ledger = LocalEntitlementRepository()
            val request = request(source = GrantSource.FAKE_PURCHASE)
            val result = FakePurchaseProvider(store(), outcome).request("sample", request)
            assertEquals(GrantResult.NOT_COMPLETED, ledger.apply(request, result))
            assertTrue(ledger.snapshot().records.isEmpty())
            assertEquals(GrantResult.APPLIED, FreeAcquisition(ledger).claim(request()))
            assertEquals(3, ledger.quantity(item.itemId))
        }
    }

    @Test fun mismatchedCallbackIdSourceItemAndQuantityAreRejectedAtomically() {
        val ledger = LocalEntitlementRepository()
        val expected = request()
        val mismatches = listOf(
            request(id = "other"), request(source = GrantSource.FAKE_PURCHASE),
            request(reward = item.copy(itemId = "other")), request(reward = item.copy(quantity = 4)),
            request(reward = FixedReward.Entitlement("test-content")),
        )
        mismatches.forEach {
            assertEquals(GrantResult.MISMATCH, ledger.apply(expected, ProviderResult.Completed(it)))
        }
        assertTrue(ledger.snapshot().records.isEmpty())
    }

    @Test fun reusedKeyWithDifferentPayloadConflictsRatherThanSilentlyAcknowledging() {
        val ledger = LocalEntitlementRepository()
        val free = FreeAcquisition(ledger)
        free.claim(request())
        val before = ledger.snapshot()
        assertEquals(GrantResult.CONFLICT, free.claim(request(reward = item.copy(quantity = 9))))
        assertEquals(GrantResult.CONFLICT, free.claim(request(reward = FixedReward.Entitlement("test-content"))))
        assertEquals(before, ledger.snapshot())
        assertEquals(3, ledger.quantity(item.itemId))
    }

    @Test fun sourceNamespacingSeparatesIndependentEventIds() {
        val ledger = LocalEntitlementRepository()
        GrantSource.entries.forEach { source ->
            val request = request(source = source)
            assertEquals(GrantResult.APPLIED, ledger.apply(request, ProviderResult.Completed(request)))
        }
        assertEquals(9, ledger.quantity(item.itemId))
    }

    @Test fun overflowRejectsWholeGrantWithoutConsumingItsKey() {
        val ledger = LocalEntitlementRepository()
        val free = FreeAcquisition(ledger)
        val full = request(reward = item.copy(quantity = Int.MAX_VALUE))
        free.claim(full)
        val before = ledger.snapshot()
        val next = request(id = "next", reward = item.copy(quantity = 1))
        assertEquals(GrantResult.CAPACITY_EXCEEDED, free.claim(next))
        assertEquals(before, ledger.snapshot())
        assertEquals(Int.MAX_VALUE, ledger.quantity(item.itemId))
        ledger.revoke(full.key)
        assertEquals(GrantResult.APPLIED, free.claim(next))
        assertEquals(1, ledger.quantity(item.itemId))
    }

    @Test fun duplicateConcurrentCallbacksHaveExactlyOneGrant() {
        val ledger = LocalEntitlementRepository()
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = executor.invokeAll(List(64) { Callable { FreeAcquisition(ledger).claim(request()) } })
                .map { it.get() }
            assertEquals(1, results.count { it == GrantResult.APPLIED })
            assertEquals(63, results.count { it == GrantResult.ALREADY_APPLIED })
            assertEquals(3, ledger.quantity(item.itemId))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test fun snapshotRecoveryKeepsBalancesIdempotencyAndRevocationTombstones() {
        val ledger = LocalEntitlementRepository()
        val free = FreeAcquisition(ledger)
        free.claim(request())
        free.claim(request(id = "revoked"))
        ledger.revoke(request(id = "revoked").key)
        val restored = LocalEntitlementRepository(ledger.snapshot())
        assertEquals(ledger.snapshot(), restored.snapshot())
        assertEquals(3, restored.quantity(item.itemId))
        assertEquals(GrantResult.ALREADY_APPLIED, FreeAcquisition(restored).claim(request()))
        assertEquals(GrantResult.REVOKED, FreeAcquisition(restored).claim(request(id = "revoked")))
    }

    @Test fun entitlementRevocationKeepsOtherValidGrantsAndIsAuditable() {
        val ledger = LocalEntitlementRepository()
        val reward = FixedReward.Entitlement("test-content")
        val first = request(reward = reward)
        val second = request(id = "second", reward = reward)
        FreeAcquisition(ledger).claim(first)
        FreeAcquisition(ledger).claim(second)
        assertTrue(ledger.hasEntitlement(reward.contentId))
        assertEquals(RevocationResult.REVOKED, ledger.revoke(first.key))
        assertTrue(ledger.hasEntitlement(reward.contentId))
        assertEquals(RevocationResult.ALREADY_REVOKED, ledger.revoke(first.key))
        assertEquals(RevocationResult.UNKNOWN, ledger.revoke(request(id = "missing").key))
        ledger.revoke(second.key)
        assertFalse(ledger.hasEntitlement(reward.contentId))
        assertTrue(ledger.snapshot().records.all { it.revoked })
        val restored = LocalEntitlementRepository(ledger.snapshot())
        assertFalse(restored.hasEntitlement(reward.contentId))
    }

    @Test fun snapshotRejectsDuplicateKeysAndImpossibleBalance() {
        val record = GrantRecord(request())
        assertThrows(IllegalArgumentException::class.java) {
            LocalEntitlementRepository(EconomySnapshot(listOf(record, record)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocalEntitlementRepository(EconomySnapshot(listOf(
                GrantRecord(request(reward = item.copy(quantity = Int.MAX_VALUE))),
                GrantRecord(request(id = "too-many")),
            )))
        }
    }

    @Test fun callerOwnedCollectionsCannotMutateCatalogOrLedger() {
        val offers = mutableListOf(FakeOffer("a", item), FakeOffer("b", item))
        val store = FakeStorefront(offers)
        offers.clear()
        (store.offers() as MutableList).clear()
        assertEquals(2, store.offers().size)
        val records = mutableListOf(GrantRecord(request()), GrantRecord(request(id = "second")))
        val ledger = LocalEntitlementRepository(EconomySnapshot(records))
        records.clear()
        (ledger.snapshot().records as MutableList).clear()
        assertEquals(2, ledger.snapshot().records.size)
        assertEquals(6, ledger.quantity(item.itemId))
    }

    @Test fun identifiersAndQuantitiesRejectMalformedInputs() {
        assertThrows(IllegalArgumentException::class.java) { GrantKey(GrantSource.FREE_PLAY, " ") }
        assertThrows(IllegalArgumentException::class.java) { FixedReward.Item("", 1) }
        assertThrows(IllegalArgumentException::class.java) { FixedReward.Item("test", 0) }
        assertThrows(IllegalArgumentException::class.java) { FixedReward.Item("test", -1) }
        assertThrows(IllegalArgumentException::class.java) { FixedReward.Entitlement("") }
        assertThrows(IllegalArgumentException::class.java) { FakeOffer("", item) }
        assertThrows(IllegalArgumentException::class.java) { FakeStorefront(listOf(FakeOffer("same", item), FakeOffer("same", item))) }
        assertTrue(FakeStorefront().offers().isEmpty())
    }

    private fun store() = FakeStorefront(listOf(FakeOffer("sample", item)))
}
