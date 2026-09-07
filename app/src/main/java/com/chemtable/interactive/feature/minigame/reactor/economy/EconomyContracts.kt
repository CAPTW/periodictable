package com.chemtable.interactive.feature.minigame.reactor.economy

/** P4 local prototypes only. IDs and quantities are supplied by a caller, not a final catalog. */
internal enum class GrantSource { FREE_PLAY, FAKE_REWARDED, FAKE_PURCHASE }

internal data class GrantKey(val source: GrantSource, val eventId: String) {
    init { require(eventId.isNotBlank()) }
}

internal sealed interface FixedReward {
    data class Item(val itemId: String, val quantity: Int) : FixedReward {
        init {
            require(itemId.isNotBlank())
            require(quantity > 0)
        }
    }

    data class Entitlement(val contentId: String) : FixedReward {
        init { require(contentId.isNotBlank()) }
    }
}

internal data class GrantRequest(val key: GrantKey, val reward: FixedReward)

internal sealed interface ProviderResult {
    data class Completed(val request: GrantRequest) : ProviderResult
    data object Pending : ProviderResult
    data object Cancelled : ProviderResult
    data object Unavailable : ProviderResult
    data object Rejected : ProviderResult
}

internal fun interface RewardedItemProvider {
    fun request(request: GrantRequest): ProviderResult
}

internal data class FakeOffer(val id: String, val reward: FixedReward) {
    init { require(id.isNotBlank()) }
}

internal fun interface Storefront {
    fun offers(): List<FakeOffer>
}

internal fun interface PurchaseProvider {
    fun request(offerId: String, request: GrantRequest): ProviderResult
}

internal enum class GrantResult {
    APPLIED, ALREADY_APPLIED, REVOKED, CONFLICT, MISMATCH, NOT_COMPLETED, CAPACITY_EXCEEDED,
}

internal enum class RevocationResult { REVOKED, ALREADY_REVOKED, UNKNOWN }

internal data class GrantRecord(val request: GrantRequest, val revoked: Boolean = false)

/** An in-process value snapshot, NOT a versioned disk format or durable purchase receipt. */
internal data class EconomySnapshot(val records: List<GrantRecord>)

internal interface EntitlementRepository {
    fun hasEntitlement(contentId: String): Boolean
}

internal interface InventoryLedger {
    fun quantity(itemId: String): Int
    fun apply(expected: GrantRequest, result: ProviderResult): GrantResult
    fun snapshot(): EconomySnapshot
}
