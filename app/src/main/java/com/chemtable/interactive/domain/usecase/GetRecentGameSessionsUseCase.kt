package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentGameSessionsUseCase @Inject constructor(
    private val repository: GameStatsRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<GameSession>> = repository.observeRecentSessions(limit)
}
