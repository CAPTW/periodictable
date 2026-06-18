package com.chemtable.interactive.feature.minigame.dex

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementProperty
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.core.util.FormulaParser
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.domain.repository.ElementRepository
import com.chemtable.interactive.domain.repository.GameStatsRepository
import com.chemtable.interactive.domain.repository.GlossaryRepository
import com.chemtable.interactive.domain.usecase.GetDiscoveredMoleculesUseCase
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import com.chemtable.interactive.domain.usecase.GetGlossaryUseCase
import com.chemtable.interactive.domain.usecase.GetHighScoreUseCase
import com.chemtable.interactive.domain.usecase.GetRecentGameSessionsUseCase
import com.chemtable.interactive.feature.minigame.MoleculeElementLinkResolver
import com.chemtable.interactive.feature.minigame.MoleculeGlossaryLinkResolver
import com.chemtable.interactive.feature.minigame.model.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MoleculeDexViewModelTest {

    @Test
    fun uiState_includesTimeAttackHighScore() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(
            MoleculeDexModeScore(GameMode.TIME_ATTACK.name, "타임어택", 300),
            state.modeHighScores.first { it.mode == GameMode.TIME_ATTACK.name },
        )
    }

    @Test
    fun uiState_mapsTimeAttackRecentSessionLabel() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.uiState.first { !it.isLoading }
        val session = state.recentSessions.single()

        assertEquals(GameMode.TIME_ATTACK.name, session.mode)
        assertEquals("타임어택", session.modeLabel)
        assertEquals("초급", session.difficultyLabel)
    }

    private fun viewModel(): MoleculeDexViewModel {
        val gameStatsRepository = FakeGameStatsRepository()
        return MoleculeDexViewModel(
            getDiscoveredMoleculesUseCase = GetDiscoveredMoleculesUseCase(gameStatsRepository),
            getHighScoreUseCase = GetHighScoreUseCase(gameStatsRepository),
            getRecentGameSessionsUseCase = GetRecentGameSessionsUseCase(gameStatsRepository),
            getElementsUseCase = GetElementsUseCase(FakeElementRepository()),
            getGlossaryUseCase = GetGlossaryUseCase(FakeGlossaryRepository()),
            moleculeElementLinkResolver = MoleculeElementLinkResolver(FormulaParser()),
            moleculeGlossaryLinkResolver = MoleculeGlossaryLinkResolver(),
        )
    }

    private class FakeGameStatsRepository : GameStatsRepository {
        override fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscovery>> = flowOf(emptyList())

        override fun observeRecentSessions(limit: Int): Flow<List<GameSession>> = flowOf(
            listOf(
                GameSession(
                    id = 1L,
                    score = 300,
                    success = false,
                    difficulty = "BEGINNER",
                    mode = GameMode.TIME_ATTACK.name,
                    missionFormula = null,
                    missionTargetCount = null,
                    playedAt = 1_700_000_000_000L,
                    moleculesMade = listOf("H2O"),
                )
            )
        )

        override fun observeHighScore(): Flow<Int?> = flowOf(500)

        override fun observeHighScoreByDifficulty(difficulty: String): Flow<Int?> = flowOf(null)

        override fun observeHighScoreByMode(mode: String): Flow<Int?> = flowOf(
            when (mode) {
                GameMode.MISSION.name -> 120
                GameMode.ENDLESS.name -> 200
                GameMode.TIME_ATTACK.name -> 300
                else -> null
            }
        )

        override fun observeHighScoreByDifficultyAndMode(
            difficulty: String,
            mode: String,
        ): Flow<Int?> = flowOf(null)

        override suspend fun getHighScore(): Int? = 500

        override suspend fun getHighScoreByDifficulty(difficulty: String): Int? = null

        override suspend fun getHighScoreByMode(mode: String): Int? = when (mode) {
            GameMode.MISSION.name -> 120
            GameMode.ENDLESS.name -> 200
            GameMode.TIME_ATTACK.name -> 300
            else -> null
        }

        override suspend fun getHighScoreByDifficultyAndMode(difficulty: String, mode: String): Int? = null

        override suspend fun recordGameResult(record: GameResultRecord) = Unit
    }

    private class FakeElementRepository : ElementRepository {
        override fun getElements(): Flow<List<Element>> = flowOf(emptyList())

        override fun getElementByAtomicNumber(number: Int): Flow<Element?> = flowOf(null)

        override fun searchElements(query: String): Flow<List<Element>> = flowOf(emptyList())

        override fun searchBySymbol(symbol: String): Flow<List<Element>> = flowOf(emptyList())

        override fun filterByProperty(
            property: ElementProperty,
            min: Double,
            max: Double,
        ): Flow<List<Element>> = flowOf(emptyList())
    }

    private class FakeGlossaryRepository : GlossaryRepository {
        override fun getAllTerms(): Flow<List<GlossaryTerm>> = flowOf(emptyList())

        override fun getTermsByCategory(category: String): Flow<List<GlossaryTerm>> = flowOf(emptyList())

        override fun searchTerms(query: String): Flow<List<GlossaryTerm>> = flowOf(emptyList())

        override suspend fun setBookmark(termId: String, bookmarked: Boolean) = Unit
    }
}
