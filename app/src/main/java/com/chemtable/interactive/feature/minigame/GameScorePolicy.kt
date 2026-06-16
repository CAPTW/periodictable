package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.feature.minigame.model.Difficulty

internal fun calculateComboBonus(combo: Int): Int {
    if (combo <= 1) return 0
    val cappedCombo = combo.coerceAtMost(MAX_COMBO_BONUS_STEPS + 1)
    return (cappedCombo - 1) * COMBO_BONUS_PER_LEVEL
}

internal fun calculateMoveScore(mergedScore: Int, combo: Int, difficulty: Difficulty): Int {
    if (mergedScore <= 0) return 0
    val comboBonus = calculateComboBonus(combo)
    return mergedScore + comboBonus
}

internal fun calculateMissionSuccessBonus(difficulty: Difficulty): Int =
    when (difficulty) {
        Difficulty.BEGINNER -> BEGINNER_MISSION_SUCCESS_BONUS
        Difficulty.INTERMEDIATE -> INTERMEDIATE_MISSION_SUCCESS_BONUS
        Difficulty.ADVANCED -> ADVANCED_MISSION_SUCCESS_BONUS
    }

private const val COMBO_BONUS_PER_LEVEL = 5
private const val MAX_COMBO_BONUS_STEPS = 3
private const val BEGINNER_MISSION_SUCCESS_BONUS = 20
private const val INTERMEDIATE_MISSION_SUCCESS_BONUS = 35
private const val ADVANCED_MISSION_SUCCESS_BONUS = 55
