package com.chemtable.interactive.data.mapper

import com.chemtable.interactive.core.database.entity.GameMoleculeDiscoveryEntity
import com.chemtable.interactive.core.database.entity.GameSessionEntity
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession

fun GameMoleculeDiscoveryEntity.toDomain(): GameMoleculeDiscovery = GameMoleculeDiscovery(
    formula = formula,
    firstDiscoveredAt = firstDiscoveredAt,
    lastDiscoveredAt = lastDiscoveredAt,
    discoveryCount = discoveryCount,
)

fun GameSessionEntity.toDomain(): GameSession = GameSession(
    id = id,
    score = score,
    success = success,
    difficulty = difficulty,
    mode = mode,
    missionFormula = missionFormula,
    missionTargetCount = missionTargetCount,
    playedAt = playedAt,
    moleculesMade = moleculesMade,
)

fun GameResultRecord.toEntity(): GameSessionEntity = GameSessionEntity(
    score = score,
    success = success,
    difficulty = difficulty,
    mode = mode,
    missionFormula = missionFormula,
    missionTargetCount = missionTargetCount,
    playedAt = playedAt,
    moleculesMade = moleculesMade,
)
