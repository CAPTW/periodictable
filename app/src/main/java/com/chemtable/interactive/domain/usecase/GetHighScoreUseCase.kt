package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHighScoreUseCase @Inject constructor(
    private val repository: GameStatsRepository
) {
    operator fun invoke(difficulty: String? = null, mode: String? = null): Flow<Int?> {
        val normalizedDifficulty = difficulty?.takeIf { it.isNotBlank() }
        val normalizedMode = mode?.takeIf { it.isNotBlank() }
        return when {
            normalizedDifficulty != null && normalizedMode != null ->
                repository.observeHighScoreByDifficultyAndMode(normalizedDifficulty, normalizedMode)
            normalizedDifficulty != null -> repository.observeHighScoreByDifficulty(normalizedDifficulty)
            normalizedMode != null -> repository.observeHighScoreByMode(normalizedMode)
            else -> repository.observeHighScore()
        }
    }

    suspend fun current(difficulty: String? = null, mode: String? = null): Int? {
        val normalizedDifficulty = difficulty?.takeIf { it.isNotBlank() }
        val normalizedMode = mode?.takeIf { it.isNotBlank() }
        return when {
            normalizedDifficulty != null && normalizedMode != null ->
                repository.getHighScoreByDifficultyAndMode(normalizedDifficulty, normalizedMode)
            normalizedDifficulty != null -> repository.getHighScoreByDifficulty(normalizedDifficulty)
            normalizedMode != null -> repository.getHighScoreByMode(normalizedMode)
            else -> repository.getHighScore()
        }
    }
}
