package com.chemtable.interactive.data.repository

import androidx.room.withTransaction
import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.core.database.ChemTableDatabase
import com.chemtable.interactive.core.database.dao.GameStatsDao
import com.chemtable.interactive.core.database.entity.GameMoleculeDiscoveryEntity
import com.chemtable.interactive.data.mapper.toDomain
import com.chemtable.interactive.data.mapper.toEntity
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GameStatsRepositoryImpl @Inject constructor(
    private val database: ChemTableDatabase,
    private val dao: GameStatsDao
) : GameStatsRepository {

    override fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscovery>> =
        dao.observeDiscoveredMolecules().map { list -> list.map { it.toDomain() } }

    override fun observeRecentSessions(limit: Int): Flow<List<GameSession>> =
        dao.observeRecentSessions(limit).map { list -> list.map { it.toDomain() } }

    override fun observeHighScore(boardSize: ClassicBoardSize): Flow<Int?> =
        dao.observeHighScore(boardSize.dimension)

    override fun observeHighScoreByDifficulty(
        difficulty: String,
        boardSize: ClassicBoardSize,
    ): Flow<Int?> = dao.observeHighScoreByDifficulty(difficulty, boardSize.dimension)

    override fun observeHighScoreByMode(mode: String, boardSize: ClassicBoardSize): Flow<Int?> =
        dao.observeHighScoreByMode(mode, boardSize.dimension)

    override fun observeHighScoreByDifficultyAndMode(
        difficulty: String,
        mode: String,
        boardSize: ClassicBoardSize,
    ): Flow<Int?> = dao.observeHighScoreByDifficultyAndMode(difficulty, mode, boardSize.dimension)

    override suspend fun getHighScore(boardSize: ClassicBoardSize): Int? =
        dao.getHighScore(boardSize.dimension)

    override suspend fun getHighScoreByDifficulty(
        difficulty: String,
        boardSize: ClassicBoardSize,
    ): Int? = dao.getHighScoreByDifficulty(difficulty, boardSize.dimension)

    override suspend fun getHighScoreByMode(mode: String, boardSize: ClassicBoardSize): Int? =
        dao.getHighScoreByMode(mode, boardSize.dimension)

    override suspend fun getHighScoreByDifficultyAndMode(
        difficulty: String,
        mode: String,
        boardSize: ClassicBoardSize,
    ): Int? = dao.getHighScoreByDifficultyAndMode(difficulty, mode, boardSize.dimension)

    override suspend fun recordGameResult(record: GameResultRecord) {
        val normalizedMoleculesMade = normalizeGameResultMolecules(record.moleculesMade)
        val normalizedRecord = record.copy(moleculesMade = normalizedMoleculesMade)

        database.withTransaction {
            dao.insertGameSession(normalizedRecord.toEntity())

            countGameResultMolecules(normalizedMoleculesMade)
                .forEach { (formula, count) ->
                    recordDiscovery(formula, count, record.playedAt)
                }
        }
    }

    private suspend fun recordDiscovery(formula: String, count: Int, discoveredAt: Long) {
        val inserted = dao.insertDiscovery(
            GameMoleculeDiscoveryEntity(
                formula = formula,
                firstDiscoveredAt = discoveredAt,
                lastDiscoveredAt = discoveredAt,
                discoveryCount = count,
            )
        )

        if (inserted == -1L) {
            val existing = dao.getDiscoveredMolecule(formula) ?: return
            dao.upsertDiscovery(
                existing.copy(
                    lastDiscoveredAt = discoveredAt,
                    discoveryCount = existing.discoveryCount + count,
                )
            )
        }
    }
}

internal fun normalizeGameResultMolecules(moleculesMade: List<String>): List<String> =
    moleculesMade
        .map { it.trim() }
        .filter { it.isNotBlank() }

internal fun countGameResultMolecules(moleculesMade: List<String>): Map<String, Int> =
    moleculesMade.groupingBy { it }.eachCount()
