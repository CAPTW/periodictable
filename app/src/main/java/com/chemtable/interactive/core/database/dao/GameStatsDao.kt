package com.chemtable.interactive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chemtable.interactive.core.database.entity.GameMoleculeDiscoveryEntity
import com.chemtable.interactive.core.database.entity.GameSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_molecule_discoveries ORDER BY lastDiscoveredAt DESC, formula ASC")
    fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscoveryEntity>>

    @Query("SELECT * FROM game_molecule_discoveries WHERE formula = :formula LIMIT 1")
    suspend fun getDiscoveredMolecule(formula: String): GameMoleculeDiscoveryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiscovery(entity: GameMoleculeDiscoveryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDiscovery(entity: GameMoleculeDiscoveryEntity): Long

    @Query(
        """
        UPDATE game_molecule_discoveries
        SET discoveryCount = discoveryCount + 1,
            lastDiscoveredAt = :discoveredAt
        WHERE formula = :formula
        """
    )
    suspend fun incrementDiscovery(formula: String, discoveredAt: Long): Int

    @Insert
    suspend fun insertGameSession(entity: GameSessionEntity): Long

    @Query("SELECT * FROM game_sessions ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<GameSessionEntity>>

    @Query("SELECT MAX(score) FROM game_sessions")
    fun observeHighScore(): Flow<Int?>

    @Query("SELECT MAX(score) FROM game_sessions WHERE difficulty = :difficulty")
    fun observeHighScoreByDifficulty(difficulty: String): Flow<Int?>

    @Query("SELECT MAX(score) FROM game_sessions WHERE mode = :mode")
    fun observeHighScoreByMode(mode: String): Flow<Int?>

    @Query("SELECT MAX(score) FROM game_sessions WHERE difficulty = :difficulty AND mode = :mode")
    fun observeHighScoreByDifficultyAndMode(difficulty: String, mode: String): Flow<Int?>

    @Query("SELECT MAX(score) FROM game_sessions")
    suspend fun getHighScore(): Int?

    @Query("SELECT MAX(score) FROM game_sessions WHERE difficulty = :difficulty")
    suspend fun getHighScoreByDifficulty(difficulty: String): Int?

    @Query("SELECT MAX(score) FROM game_sessions WHERE mode = :mode")
    suspend fun getHighScoreByMode(mode: String): Int?

    @Query("SELECT MAX(score) FROM game_sessions WHERE difficulty = :difficulty AND mode = :mode")
    suspend fun getHighScoreByDifficultyAndMode(difficulty: String, mode: String): Int?
}
