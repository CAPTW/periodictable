package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession
import kotlinx.coroutines.flow.Flow

interface GameStatsRepository {
    fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscovery>>
    fun observeRecentSessions(limit: Int = 10): Flow<List<GameSession>>
    fun observeHighScore(): Flow<Int?>
    suspend fun getHighScore(): Int?
    suspend fun recordGameResult(record: GameResultRecord)
}
