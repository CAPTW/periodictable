package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordGameResultUseCaseTest {

    @Test
    fun invoke_delegatesRecordToRepository() = runBlocking {
        val repository = FakeGameStatsRepository()
        val useCase = RecordGameResultUseCase(repository)
        val record = GameResultRecord(
            score = 500,
            success = true,
            difficulty = "BEGINNER",
            missionFormula = "H2O",
            missionTargetCount = 1,
            playedAt = 1000L,
            moleculesMade = listOf("H2O"),
        )

        useCase(record)

        assertEquals(record, repository.recorded)
    }

    private class FakeGameStatsRepository : GameStatsRepository {
        var recorded: GameResultRecord? = null

        override fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscovery>> = flowOf(emptyList())

        override fun observeRecentSessions(limit: Int): Flow<List<GameSession>> = flowOf(emptyList())

        override fun observeHighScore(): Flow<Int?> = flowOf(null)

        override fun observeHighScoreByDifficulty(difficulty: String): Flow<Int?> = flowOf(null)

        override fun observeHighScoreByMode(mode: String): Flow<Int?> = flowOf(null)

        override fun observeHighScoreByDifficultyAndMode(difficulty: String, mode: String): Flow<Int?> =
            flowOf(null)

        override suspend fun getHighScore(): Int? = null

        override suspend fun getHighScoreByDifficulty(difficulty: String): Int? = null

        override suspend fun getHighScoreByMode(mode: String): Int? = null

        override suspend fun getHighScoreByDifficultyAndMode(difficulty: String, mode: String): Int? = null

        override suspend fun recordGameResult(record: GameResultRecord) {
            recorded = record
        }
    }
}
