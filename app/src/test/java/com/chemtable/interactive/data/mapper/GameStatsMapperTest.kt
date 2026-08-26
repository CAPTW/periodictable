package com.chemtable.interactive.data.mapper

import com.chemtable.interactive.core.database.entity.GameMoleculeDiscoveryEntity
import com.chemtable.interactive.core.database.entity.GameSessionEntity
import com.chemtable.interactive.domain.model.GameResultRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStatsMapperTest {

    @Test
    fun moleculeDiscoveryEntityToDomain_mapsAllFields() {
        val entity = GameMoleculeDiscoveryEntity(
            formula = "H2O",
            firstDiscoveredAt = 100L,
            lastDiscoveredAt = 200L,
            discoveryCount = 3,
        )

        val domain = entity.toDomain()

        assertEquals("H2O", domain.formula)
        assertEquals(100L, domain.firstDiscoveredAt)
        assertEquals(200L, domain.lastDiscoveredAt)
        assertEquals(3, domain.discoveryCount)
    }

    @Test
    fun gameSessionEntityToDomain_mapsAllFields() {
        val entity = GameSessionEntity(
            id = 7L,
            score = 1200,
            success = true,
            difficulty = "BEGINNER",
            mode = "MISSION",
            missionFormula = "CO2",
            missionTargetCount = 2,
            playedAt = 300L,
            moleculesMade = listOf("CO2", "NaCl"),
            boardSize = 6,
        )

        val domain = entity.toDomain()

        assertEquals(7L, domain.id)
        assertEquals(1200, domain.score)
        assertEquals(true, domain.success)
        assertEquals("BEGINNER", domain.difficulty)
        assertEquals("MISSION", domain.mode)
        assertEquals("CO2", domain.missionFormula)
        assertEquals(2, domain.missionTargetCount)
        assertEquals(300L, domain.playedAt)
        assertEquals(listOf("CO2", "NaCl"), domain.moleculesMade)
        assertEquals(6, domain.boardSize)
    }

    @Test
    fun gameResultRecordToEntity_usesNewSessionIdAndMapsAllFields() {
        val record = GameResultRecord(
            score = 850,
            success = false,
            difficulty = "BEGINNER",
            mode = "MISSION",
            missionFormula = null,
            missionTargetCount = null,
            playedAt = 400L,
            moleculesMade = listOf("H2O", "H2O", "CO2"),
            boardSize = 5,
        )

        val entity = record.toEntity()

        assertEquals(0L, entity.id)
        assertEquals(850, entity.score)
        assertEquals(false, entity.success)
        assertEquals("BEGINNER", entity.difficulty)
        assertEquals("MISSION", entity.mode)
        assertEquals(null, entity.missionFormula)
        assertEquals(null, entity.missionTargetCount)
        assertEquals(400L, entity.playedAt)
        assertEquals(listOf("H2O", "H2O", "CO2"), entity.moleculesMade)
        assertEquals(5, entity.boardSize)
    }
}
