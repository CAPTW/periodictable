package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetHighScoreUseCaseTest {

    @Test
    fun invoke_withoutDifficultyReturnsGlobalHighScore() = runBlocking {
        val useCase = GetHighScoreUseCase(FakeGameStatsRepository())

        assertEquals(900, useCase().first())
        assertEquals(900, useCase.current())
    }

    @Test
    fun invoke_withDifficultyReturnsDifficultyHighScore() = runBlocking {
        val useCase = GetHighScoreUseCase(FakeGameStatsRepository())

        assertEquals(700, useCase("INTERMEDIATE").first())
        assertEquals(700, useCase.current("INTERMEDIATE"))
    }

    private class FakeGameStatsRepository : GameStatsRepository {
        override fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscovery>> = flowOf(emptyList())

        override fun observeRecentSessions(limit: Int): Flow<List<GameSession>> = flowOf(emptyList())

        override fun observeHighScore(): Flow<Int?> = flowOf(900)

        override fun observeHighScoreByDifficulty(difficulty: String): Flow<Int?> =
            flowOf(if (difficulty == "INTERMEDIATE") 700 else null)

        override suspend fun getHighScore(): Int? = 900

        override suspend fun getHighScoreByDifficulty(difficulty: String): Int? =
            if (difficulty == "INTERMEDIATE") 700 else null

        override suspend fun recordGameResult(record: GameResultRecord) = Unit
    }
}
