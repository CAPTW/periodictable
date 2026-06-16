package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHighScoreUseCase @Inject constructor(
    private val repository: GameStatsRepository
) {
    operator fun invoke(difficulty: String? = null): Flow<Int?> =
        difficulty
            ?.takeIf { it.isNotBlank() }
            ?.let { repository.observeHighScoreByDifficulty(it) }
            ?: repository.observeHighScore()

    suspend fun current(difficulty: String? = null): Int? =
        difficulty
            ?.takeIf { it.isNotBlank() }
            ?.let { repository.getHighScoreByDifficulty(it) }
            ?: repository.getHighScore()
}
