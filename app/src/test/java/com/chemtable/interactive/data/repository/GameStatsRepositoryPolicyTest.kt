package com.chemtable.interactive.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class GameStatsRepositoryPolicyTest {

    @Test
    fun normalizeGameResultMolecules_trimsAndDropsBlankFormulas() {
        val normalized = normalizeGameResultMolecules(
            listOf(" H2O ", "", "CO2", "   ", "NaCl")
        )

        assertEquals(listOf("H2O", "CO2", "NaCl"), normalized)
    }

    @Test
    fun countGameResultMolecules_countsDuplicateFormulaOccurrences() {
        val counts = countGameResultMolecules(
            listOf("H2O", "CO2", "H2O", "NaCl", "CO2")
        )

        assertEquals(mapOf("H2O" to 2, "CO2" to 2, "NaCl" to 1), counts)
    }
}
