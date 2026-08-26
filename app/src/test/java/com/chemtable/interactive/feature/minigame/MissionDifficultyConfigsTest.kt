package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.feature.minigame.engine.RecipeBook
import com.chemtable.interactive.feature.minigame.model.Difficulty
import com.chemtable.interactive.feature.minigame.model.MissionDifficultyConfigs
import com.chemtable.interactive.feature.minigame.model.MissionDifficultyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionDifficultyConfigsTest {

    @Test
    fun beginnerConfig_matchesMissionMvpDefaults() {
        val config = MissionDifficultyConfigs.forDifficulty(Difficulty.BEGINNER)

        assertEquals(Difficulty.BEGINNER, config.difficulty)
        assertEquals(4, config.initialBlockCount)
        assertEquals(listOf("H", "H", "H", "H", "O", "O", "O", "Na", "Cl"), config.spawnSymbols)
        assertEquals(null, config.movesLeft)
        assertEquals(120_000L, config.timeAttackLimitMillis)
        assertEquals("H2O", config.missionCandidates.first().formula)
        assertEquals(2, config.missionCandidates.first().count)
    }

    @Test
    fun intermediateConfig_addsCarbonNitrogenAndCo2Mission() {
        val config = MissionDifficultyConfigs.forDifficulty(Difficulty.INTERMEDIATE)

        assertEquals(Difficulty.INTERMEDIATE, config.difficulty)
        assertEquals(4, config.initialBlockCount)
        assertTrue(config.spawnSymbols.contains("C"))
        assertTrue(config.spawnSymbols.contains("N"))
        assertEquals(null, config.movesLeft)
        assertEquals(90_000L, config.timeAttackLimitMillis)
        assertEquals("CO2", config.missionCandidates.first().formula)
        assertEquals(1, config.missionCandidates.first().count)
    }

    @Test
    fun advancedConfig_hasMoveLimit() {
        val config = MissionDifficultyConfigs.forDifficulty(Difficulty.ADVANCED)

        assertEquals(Difficulty.ADVANCED, config.difficulty)
        assertEquals(5, config.initialBlockCount)
        assertEquals(36, config.movesLeft)
        assertEquals(60_000L, config.timeAttackLimitMillis)
        assertEquals("CO2", config.missionCandidates.first().formula)
        assertEquals(2, config.missionCandidates.first().count)
    }

    @Test
    fun allConfigs_haveSpawnSymbolsAndRecipeBackedMissions() {
        val productFormulas = RecipeBook.DEFAULT_RECIPES.map { it.productFormula }.toSet()

        Difficulty.values().forEach { difficulty ->
            val config = MissionDifficultyConfigs.forDifficulty(difficulty)
            assertTrue("${difficulty.name} spawn pool is empty", config.spawnSymbols.isNotEmpty())
            assertTrue("${difficulty.name} missions are empty", config.missionCandidates.isNotEmpty())
            assertTrue("${difficulty.name} time attack limit must be positive", config.timeAttackLimitMillis > 0L)
            config.missionCandidates.forEach { target ->
                assertTrue("${target.formula} is not recipe-backed", target.formula in productFormulas)
                assertTrue("${target.formula} count must be positive", target.count > 0)
                assertNotNull(RecipeBook.DEFAULT_RECIPES.firstOrNull { it.productFormula == target.formula })
            }
        }
    }

    @Test
    fun difficultyConfigDoesNotOwnACompetingBoardSize() {
        assertTrue(MissionDifficultyConfig::class.java.declaredFields.none { it.name == "boardSize" })
    }
}
