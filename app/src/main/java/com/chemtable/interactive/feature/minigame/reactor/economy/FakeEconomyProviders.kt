package com.chemtable.interactive.feature.minigame.reactor.economy

/** Completion must be selected explicitly by a test/local host. Nothing plays or purchases. */
internal enum class FakeOutcome { COMPLETED, PENDING, CANCELLED, UNAVAILABLE, REJECTED }

internal class FakeRewardedItemProvider(
    private val outcome: FakeOutcome = FakeOutcome.UNAVAILABLE,
) : RewardedItemProvider {
    override fun request(request: GrantRequest): ProviderResult =
        if (request.key.source != GrantSource.FAKE_REWARDED || request.reward !is FixedReward.Item) {
            ProviderResult.Rejected
        } else {
            outcome.result(request)
        }
}

/** No prices, network, clock, SDK, or built-in merchandise. An empty catalog is valid. */
internal class FakeStorefront(offers: List<FakeOffer> = emptyList()) : Storefront {
    private val catalog = offers.toList()

    init { require(catalog.map { it.id }.distinct().size == catalog.size) }

    override fun offers(): List<FakeOffer> = catalog.toList()
}

internal class FakePurchaseProvider(
    private val storefront: Storefront,
    private val outcome: FakeOutcome = FakeOutcome.UNAVAILABLE,
) : PurchaseProvider {
    override fun request(offerId: String, request: GrantRequest): ProviderResult {
        val offer = storefront.offers().singleOrNull { it.id == offerId }
        if (request.key.source != GrantSource.FAKE_PURCHASE || offer?.reward != request.reward) {
            return ProviderResult.Rejected
        }
        return outcome.result(request)
    }
}

/** The free path never depends on storefront availability or a rewarded/purchase result. */
internal class FreeAcquisition(private val ledger: InventoryLedger) {
    fun claim(request: GrantRequest): GrantResult =
        if (request.key.source != GrantSource.FREE_PLAY) GrantResult.MISMATCH
        else ledger.apply(request, ProviderResult.Completed(request))
}

private fun FakeOutcome.result(request: GrantRequest): ProviderResult = when (this) {
    FakeOutcome.COMPLETED -> ProviderResult.Completed(request)
    FakeOutcome.PENDING -> ProviderResult.Pending
    FakeOutcome.CANCELLED -> ProviderResult.Cancelled
    FakeOutcome.UNAVAILABLE -> ProviderResult.Unavailable
    FakeOutcome.REJECTED -> ProviderResult.Rejected
}
