package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.feature.minigame.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class GameScorePolicyTest {

    @Test
    fun calculateComboBonus_isZeroForFirstCombo() {
        assertEquals(0, calculateComboBonus(1))
        assertEquals(0, calculateComboBonus(0))
    }

    @Test
    fun calculateComboBonus_scalesWithComboButCapsAtLevelThree() {
        assertEquals(5, calculateComboBonus(2))
        assertEquals(10, calculateComboBonus(3))
        assertEquals(15, calculateComboBonus(4))
        assertEquals(15, calculateComboBonus(12))
    }

    @Test
    fun calculateMoveScore_addsComboBonusOnly() {
        assertEquals(18, calculateMoveScore(18, combo = 1, difficulty = Difficulty.BEGINNER))
        assertEquals(23, calculateMoveScore(18, combo = 2, difficulty = Difficulty.BEGINNER))
        assertEquals(28, calculateMoveScore(18, combo = 3, difficulty = Difficulty.INTERMEDIATE))
    }

    @Test
    fun calculateMissionSuccessBonus_variesByDifficulty() {
        assertEquals(20, calculateMissionSuccessBonus(Difficulty.BEGINNER))
        assertEquals(35, calculateMissionSuccessBonus(Difficulty.INTERMEDIATE))
        assertEquals(55, calculateMissionSuccessBonus(Difficulty.ADVANCED))
    }
}
