package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession
import kotlinx.coroutines.flow.Flow

interface GameStatsRepository {
    fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscovery>>
    fun observeRecentSessions(limit: Int = 10): Flow<List<GameSession>>
    fun observeHighScore(boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT): Flow<Int?>
    fun observeHighScoreByDifficulty(
        difficulty: String,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Flow<Int?>
    fun observeHighScoreByMode(
        mode: String,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Flow<Int?>
    fun observeHighScoreByDifficultyAndMode(
        difficulty: String,
        mode: String,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Flow<Int?>
    suspend fun getHighScore(boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT): Int?
    suspend fun getHighScoreByDifficulty(
        difficulty: String,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Int?
    suspend fun getHighScoreByMode(
        mode: String,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Int?
    suspend fun getHighScoreByDifficultyAndMode(
        difficulty: String,
        mode: String,
        boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
    ): Int?
    suspend fun recordGameResult(record: GameResultRecord)
}
