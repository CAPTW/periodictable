package com.chemtable.interactive.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.chemtable.interactive.core.model.ClassicBoardSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicBoardSizePreferencesTest {

    @Test
    fun missingPreferenceDefaultsToFourByFour() = runBlocking {
        val repository = SettingsRepositoryImpl(FakePreferencesDataStore())

        assertEquals(ClassicBoardSize.FOUR_BY_FOUR, repository.settings.first().preferredClassicBoardSize)
    }

    @Test
    fun everySupportedSizeRoundTripsThroughTheRepository() = runBlocking {
        val repository = SettingsRepositoryImpl(FakePreferencesDataStore())

        ClassicBoardSize.entries.forEach { expected ->
            repository.setPreferredClassicBoardSize(expected)
            assertEquals(expected, repository.settings.first().preferredClassicBoardSize)
        }
    }

    @Test
    fun corruptStoredValueFallsBackToFourByFour() = runBlocking {
        val corruptPreferences = preferencesOf(intPreferencesKey("preferred_classic_board_size") to 99)
        val repository = SettingsRepositoryImpl(FakePreferencesDataStore(corruptPreferences))

        assertEquals(ClassicBoardSize.FOUR_BY_FOUR, repository.settings.first().preferredClassicBoardSize)
    }

    private class FakePreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
