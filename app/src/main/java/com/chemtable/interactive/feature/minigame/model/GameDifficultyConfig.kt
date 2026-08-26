package com.chemtable.interactive.feature.minigame.model

data class MissionDifficultyConfig(
    val difficulty: Difficulty,
    val initialBlockCount: Int,
    val spawnSymbols: List<String>,
    val missionCandidates: List<MissionTargetSpec>,
    val movesLeft: Int?,
    val timeAttackLimitMillis: Long,
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
                timeAttackLimitMillis = 120_000L,
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
                timeAttackLimitMillis = 90_000L,
            )
            Difficulty.ADVANCED -> MissionDifficultyConfig(
                difficulty = Difficulty.ADVANCED,
                initialBlockCount = 5,
                spawnSymbols = listOf("H", "H", "O", "C", "N", "Na", "Cl"),
                missionCandidates = listOf(
                    MissionTargetSpec("CO2", 2),
                    MissionTargetSpec("NaCl", 3),
                    MissionTargetSpec("H2O", 3),
                ),
                movesLeft = 36,
                timeAttackLimitMillis = 60_000L,
            )
        }
}
