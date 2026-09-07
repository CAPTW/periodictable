package com.chemtable.interactive.feature.minigame.reactor.economy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReactorSupplyRepositoryTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun realFileReopenRestoresOneGrantAfterConcurrentClaims() = runBlocking {
        val file = temp.newFolder().resolve("supply.preferences_pb")
        val firstJob = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file }
        try {
            val first = ReactorSupplyRepository(store)
            assertEquals(ReactorSupplyAccount(0, false), first.read())
            assertFalse(file.exists()) // Merely visiting the screen must not create a grant.
            val results = List(24) { async { ReactorSupplyRepository(store).claim() } }.awaitAll()
            assertTrue(results.all { it == ReactorSupplyAccount(1, true) })
            assertTrue(file.length() > 0)
        } finally {
            firstJob.cancelAndJoin()
        }
        val secondJob = SupervisorJob()
        val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(secondJob + Dispatchers.IO)) { file }
        try {
            val second = ReactorSupplyRepository(reopened)
            assertEquals(ReactorSupplyAccount(1, true), second.read())
            assertEquals(ReactorSupplyAccount(1, true), second.claim())
        } finally {
            secondJob.cancelAndJoin()
        }
    }

    @Test fun writeFailureDoesNotAcknowledgeOrConsumeTheGrant() = runBlocking {
        val store = FaultStore(failBeforeWrite = true)
        val repo = ReactorSupplyRepository(store)
        expectFailure { repo.claim() }
        assertEquals(ReactorSupplyAccount(0, false), repo.read())
        store.failBeforeWrite = false
        assertEquals(ReactorSupplyAccount(1, true), repo.claim())
    }

    @Test fun lostAcknowledgementAfterCommitRetriesWithoutDuplicate() = runBlocking {
        val store = FaultStore(failAfterWrite = true)
        expectFailure { ReactorSupplyRepository(store).claim() }
        store.failAfterWrite = false
        val recreated = ReactorSupplyRepository(store)
        assertEquals(ReactorSupplyAccount(1, true), recreated.read())
        assertEquals(ReactorSupplyAccount(1, true), recreated.claim())
    }

    @Test fun corruptOrUnknownDataIsPreservedRatherThanReplacedWithZero() = runBlocking {
        for (prefs in listOf(
            mutablePreferencesOf(ReactorSupplyRepository.SNAPSHOT to "corrupt"),
            mutablePreferencesOf(stringPreferencesKey("future-schema") to "keep"),
        )) {
            val store = FaultStore(initial = prefs)
            val repo = ReactorSupplyRepository(store)
            expectFailure { repo.read() }
            expectFailure { repo.claim() }
            assertEquals(prefs, store.value.value)
        }
    }

    @Test fun revokedStarterCannotBeClaimedAgainAfterReopen() = runBlocking {
        val ledger = LocalEntitlementRepository()
        FreeAcquisition(ledger).claim(StarterSupply.request)
        ledger.revoke(StarterSupply.request.key)
        val prefs = mutablePreferencesOf(ReactorSupplyRepository.SNAPSHOT to EconomySnapshotCodec.encode(ledger.snapshot()))
        val repo = ReactorSupplyRepository(FaultStore(initial = prefs))
        assertEquals(ReactorSupplyAccount(0, true), repo.read())
        assertEquals(ReactorSupplyAccount(0, true), repo.claim())
    }

    @Test fun grantContentConflictPreservesExistingReceipt() = runBlocking {
        val changed = StarterSupply.request.copy(reward = FixedReward.Item("different", 9))
        val encoded = EconomySnapshotCodec.encode(EconomySnapshot(listOf(GrantRecord(changed))))
        val prefs = mutablePreferencesOf(ReactorSupplyRepository.SNAPSHOT to encoded)
        val store = FaultStore(initial = prefs)
        expectFailure { ReactorSupplyRepository(store).read() }
        expectFailure { ReactorSupplyRepository(store).claim() }
        assertEquals(prefs, store.value.value)
    }

    private suspend fun expectFailure(block: suspend () -> Any) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        assertTrue("Expected read/write to fail", failed)
    }

    private class FaultStore(
        initial: Preferences = emptyPreferences(),
        var failBeforeWrite: Boolean = false,
        var failAfterWrite: Boolean = false,
    ) : DataStore<Preferences> {
        val value = MutableStateFlow(initial)
        override val data: Flow<Preferences> = value
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val next = transform(value.value)
            if (failBeforeWrite) throw IOException("Synthetic write failure")
            value.value = next
            if (failAfterWrite) throw IOException("Synthetic lost acknowledgement")
            return next
        }
    }
}
