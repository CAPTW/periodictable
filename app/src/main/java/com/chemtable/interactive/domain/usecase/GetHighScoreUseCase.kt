package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHighScoreUseCase @Inject constructor(
    private val repository: GameStatsRepository
) {
    operator fun invoke(): Flow<Int?> = repository.observeHighScore()

    suspend fun current(): Int? = repository.getHighScore()
}
