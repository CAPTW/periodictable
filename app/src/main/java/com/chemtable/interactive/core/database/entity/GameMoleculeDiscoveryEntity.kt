package com.chemtable.interactive.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_molecule_discoveries",
    indices = [
        Index(
            value = ["lastDiscoveredAt"],
            name = "index_game_molecule_discoveries_lastDiscoveredAt",
        ),
        Index(
            value = ["discoveryCount"],
            name = "index_game_molecule_discoveries_discoveryCount",
        ),
    ],
)
data class GameMoleculeDiscoveryEntity(
    @PrimaryKey val formula: String,
    val firstDiscoveredAt: Long,
    val lastDiscoveredAt: Long,
    val discoveryCount: Int,
)
