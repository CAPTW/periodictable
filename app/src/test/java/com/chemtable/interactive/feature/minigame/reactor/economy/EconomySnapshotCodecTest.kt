package com.chemtable.interactive.feature.minigame.reactor.economy

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EconomySnapshotCodecTest {
    private val snapshot = EconomySnapshot(listOf(
        GrantRecord(GrantRequest(GrantKey(GrantSource.FREE_PLAY, "튜토리얼\n1"), FixedReward.Item("조각", 2))),
        GrantRecord(GrantRequest(GrantKey(GrantSource.FAKE_PURCHASE, "content"), FixedReward.Entitlement("학습")), true),
    ))

    @Test fun roundTripPreservesExactGrantsAndTombstones() {
        val encoded = EconomySnapshotCodec.encode(snapshot)
        assertEquals(snapshot, EconomySnapshotCodec.decode(encoded))
        assertEquals(encoded, EconomySnapshotCodec.encode(EconomySnapshotCodec.decode(encoded)))
        val empty = EconomySnapshot(emptyList())
        assertEquals(empty, EconomySnapshotCodec.decode(EconomySnapshotCodec.encode(empty)))
    }

    @Test fun malformedTruncatedAndTrailingDataFailClosed() {
        val bytes = Base64.getDecoder().decode(EconomySnapshotCodec.encode(snapshot))
        listOf("", "not base64!", encode(bytes.copyOf(bytes.size - 1)), encode(bytes + 0)).forEach {
            assertThrows(Exception::class.java) { EconomySnapshotCodec.decode(it) }
        }
    }

    @Test fun unknownVersionAndOversizedCountAreRejected() {
        val bytes = Base64.getDecoder().decode(EconomySnapshotCodec.encode(snapshot))
        val version = bytes.copyOf().apply { this[3] = 2 }
        val count = bytes.copyOf().apply { this[4] = 127 }
        listOf(version, count).forEach {
            assertThrows(IllegalArgumentException::class.java) { EconomySnapshotCodec.decode(encode(it)) }
        }
    }

    @Test fun duplicateSnapshotCannotBePersisted() {
        assertThrows(IllegalArgumentException::class.java) {
            EconomySnapshotCodec.encode(EconomySnapshot(listOf(snapshot.records.first(), snapshot.records.first())))
        }
    }

    @Test fun invalidRevocationFlagIsNotSilentlyAcceptedAsTrue() {
        val bytes = Base64.getDecoder().decode(EconomySnapshotCodec.encode(snapshot))
        bytes[bytes.lastIndex] = 2
        assertThrows(IllegalArgumentException::class.java) { EconomySnapshotCodec.decode(encode(bytes)) }
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
}
