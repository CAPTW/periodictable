package com.chemtable.interactive.domain.model

data class GameMoleculeDiscovery(
    val formula: String,
    val firstDiscoveredAt: Long,
    val lastDiscoveredAt: Long,
    val discoveryCount: Int,
)
