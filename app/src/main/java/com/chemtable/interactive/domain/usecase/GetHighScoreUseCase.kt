package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHighScoreUseCase @Inject constructor(
    private val repository: GameStatsRepository
) {
    operator fun invoke(
        difficulty: String? = null,
        mode: String? = null,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Flow<Int?> {
        val normalizedDifficulty = difficulty?.takeIf { it.isNotBlank() }
        val normalizedMode = mode?.takeIf { it.isNotBlank() }
        return when {
            normalizedDifficulty != null && normalizedMode != null ->
                repository.observeHighScoreByDifficultyAndMode(normalizedDifficulty, normalizedMode, boardSize)
            normalizedDifficulty != null -> repository.observeHighScoreByDifficulty(normalizedDifficulty, boardSize)
            normalizedMode != null -> repository.observeHighScoreByMode(normalizedMode, boardSize)
            else -> repository.observeHighScore(boardSize)
        }
    }

    suspend fun current(
        difficulty: String? = null,
        mode: String? = null,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Int? {
        val normalizedDifficulty = difficulty?.takeIf { it.isNotBlank() }
        val normalizedMode = mode?.takeIf { it.isNotBlank() }
        return when {
            normalizedDifficulty != null && normalizedMode != null ->
                repository.getHighScoreByDifficultyAndMode(normalizedDifficulty, normalizedMode, boardSize)
            normalizedDifficulty != null -> repository.getHighScoreByDifficulty(normalizedDifficulty, boardSize)
            normalizedMode != null -> repository.getHighScoreByMode(normalizedMode, boardSize)
            else -> repository.getHighScore(boardSize)
        }
    }
}
