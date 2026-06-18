package com.chemtable.interactive.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_sessions",
    indices = [
        Index(
            value = ["playedAt"],
            name = "index_game_sessions_playedAt",
        ),
        Index(
            value = ["score"],
            name = "index_game_sessions_score",
        ),
        Index(
            value = ["difficulty"],
            name = "index_game_sessions_difficulty",
        ),
        Index(
            value = ["mode"],
            name = "index_game_sessions_mode",
        ),
    ],
)
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val success: Boolean,
    val difficulty: String,
    val mode: String,
    val missionFormula: String?,
    val missionTargetCount: Int?,
    val playedAt: Long,
    val moleculesMade: List<String>,
)
