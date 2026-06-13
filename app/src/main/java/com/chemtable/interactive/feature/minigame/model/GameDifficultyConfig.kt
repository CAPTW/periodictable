package com.chemtable.interactive.feature.minigame.model

data class MissionDifficultyConfig(
    val difficulty: Difficulty,
    val boardSize: Int = 4,
    val initialBlockCount: Int,
    val spawnSymbols: List<String>,
    val missionCandidates: List<MissionTargetSpec>,
    val movesLeft: Int?,
)

data class MissionTargetSpec(
    val formula: String,
    val count: Int,
)

object MissionDifficultyConfigs {
    fun forDifficulty(difficulty: Difficulty): MissionDifficultyConfig =
        when (difficulty) {
            Difficulty.BEGINNER -> MissionDifficultyConfig(
                difficulty = Difficulty.BEGINNER,
                initialBlockCount = 4,
                spawnSymbols = listOf("H", "H", "H", "H", "O", "O", "O", "Na", "Cl"),
                missionCandidates = listOf(
                    MissionTargetSpec("H2O", 2),
                    MissionTargetSpec("NaCl", 1),
                ),
                movesLeft = null,
            )
            Difficulty.INTERMEDIATE -> MissionDifficultyConfig(
                difficulty = Difficulty.INTERMEDIATE,
                initialBlockCount = 4,
                spawnSymbols = listOf("H", "H", "O", "O", "C", "C", "N", "Na", "Cl"),
                missionCandidates = listOf(
                    MissionTargetSpec("CO2", 1),
                    MissionTargetSpec("H2O", 2),
                    MissionTargetSpec("NaCl", 2),
                ),
                movesLeft = null,
            )
            Difficulty.ADVANCED -> MissionDifficultyConfig(
                difficulty = Difficulty.ADVANCED,
                initialBlockCount = 5,
                spawnSymbols = listOf("H", "O", "C", "N", "Na", "Cl"),
                missionCandidates = listOf(
                    MissionTargetSpec("CO2", 2),
                    MissionTargetSpec("NaCl", 3),
                    MissionTargetSpec("H2O", 3),
                ),
                movesLeft = 35,
            )
        }
}
