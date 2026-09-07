package com.chemtable.interactive.feature.minigame.reactor.economy

/**
 * Session-local, synchronized grant ledger. It writes no files, Room, or preferences.
 * Completed is a trusted local fake result, NOT proof of an ad view or real payment.
 * A future durable adapter must atomically persist verification + grant + revocation.
 */
internal class LocalEntitlementRepository(
    initial: EconomySnapshot = EconomySnapshot(emptyList()),
) : EntitlementRepository, InventoryLedger {
    private val records = linkedMapOf<GrantKey, GrantRecord>()
    private val quantities = mutableMapOf<String, Int>()

    init {
        // Rebuild from audited grants, never trust caller-provided balance totals.
        // Reject duplicate keys even if identical: a malformed snapshot fails closed.
        initial.records.toList().forEach { record ->
            require(record.request.key !in records) { "Duplicate grant key" }
            if (!record.revoked) {
                val result = apply(record.request, ProviderResult.Completed(record.request))
                require(result == GrantResult.APPLIED) { "Invalid snapshot: $result" }
            } else {
                records[record.request.key] = record
            }
        }
    }

    @Synchronized
    override fun apply(expected: GrantRequest, result: ProviderResult): GrantResult {
        if (result !is ProviderResult.Completed) return GrantResult.NOT_COMPLETED
        if (result.request != expected) return GrantResult.MISMATCH
        records[expected.key]?.let { previous ->
            return when {
                previous.request != expected -> GrantResult.CONFLICT
                previous.revoked -> GrantResult.REVOKED
                else -> GrantResult.ALREADY_APPLIED
            }
        }
        val item = expected.reward as? FixedReward.Item
        val nextQuantity = if (item != null) {
            val current = quantities[item.itemId] ?: 0
            if (current > Int.MAX_VALUE - item.quantity) return GrantResult.CAPACITY_EXCEEDED
            current + item.quantity
        } else null

        // Both changes happen under the same monitor, after all validation/calculation.
        if (item != null) quantities[item.itemId] = requireNotNull(nextQuantity)
        records[expected.key] = GrantRecord(expected)
        return GrantResult.APPLIED
    }

    @Synchronized
    fun revoke(key: GrantKey): RevocationResult {
        val record = records[key] ?: return RevocationResult.UNKNOWN
        if (record.revoked) return RevocationResult.ALREADY_REVOKED
        val item = record.request.reward as? FixedReward.Item
        if (item != null) {
            quantities[item.itemId] = quantities.getValue(item.itemId) - item.quantity
        }
        records[key] = record.copy(revoked = true)
        return RevocationResult.REVOKED
    }

    @Synchronized
    override fun quantity(itemId: String): Int = quantities[itemId] ?: 0

    @Synchronized
    override fun hasEntitlement(contentId: String): Boolean = records.values.any {
        !it.revoked && it.request.reward == FixedReward.Entitlement(contentId)
    }

    @Synchronized
    override fun snapshot(): EconomySnapshot = EconomySnapshot(records.values.toList())
}
