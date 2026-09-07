package com.chemtable.interactive.feature.minigame.reactor.economy

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64

/** Internal v1 local format. Unknown/corrupt data must never become an empty wallet. */
internal object EconomySnapshotCodec {
    private const val VERSION = 1
    private const val MAX_RECORDS = 256
    private const val MAX_ENCODED_LENGTH = 1_048_576

    fun encode(snapshot: EconomySnapshot): String {
        require(snapshot.records.size <= MAX_RECORDS)
        LocalEntitlementRepository(snapshot) // Validate duplicate keys and balances before writing.
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(VERSION)
            output.writeInt(snapshot.records.size)
            snapshot.records.forEach { record ->
                output.writeUTF(record.request.key.source.name)
                output.writeUTF(record.request.key.eventId)
                when (val reward = record.request.reward) {
                    is FixedReward.Item -> {
                        output.writeByte(0)
                        output.writeUTF(reward.itemId)
                        output.writeInt(reward.quantity)
                    }
                    is FixedReward.Entitlement -> {
                        output.writeByte(1)
                        output.writeUTF(reward.contentId)
                    }
                }
                output.writeByte(if (record.revoked) 1 else 0)
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray()).also {
            require(it.length <= MAX_ENCODED_LENGTH)
        }
    }

    fun decode(encoded: String): EconomySnapshot {
        require(encoded.isNotEmpty() && encoded.length <= MAX_ENCODED_LENGTH)
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(encoded))).use { input ->
            require(input.readInt() == VERSION) { "Unsupported economy format" }
            val count = input.readInt()
            require(count in 0..MAX_RECORDS)
            val records = List(count) {
                val key = GrantKey(GrantSource.valueOf(input.readUTF()), input.readUTF())
                val reward = when (input.readUnsignedByte()) {
                    0 -> FixedReward.Item(input.readUTF(), input.readInt())
                    1 -> FixedReward.Entitlement(input.readUTF())
                    else -> error("Unknown reward type")
                }
                val revoked = input.readUnsignedByte()
                require(revoked in 0..1)
                GrantRecord(GrantRequest(key, reward), revoked == 1)
            }
            require(input.read() == -1) { "Trailing economy data" }
            return EconomySnapshot(records).also { LocalEntitlementRepository(it) }
        }
    }
}
