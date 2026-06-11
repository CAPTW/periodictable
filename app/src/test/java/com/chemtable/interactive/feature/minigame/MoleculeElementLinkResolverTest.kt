package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.core.util.FormulaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoleculeElementLinkResolverTest {

    private val resolver = MoleculeElementLinkResolver(FormulaParser())

    @Test
    fun compositionForFormula_resolvesH2O() {
        assertEquals(mapOf("H" to 2, "O" to 1), resolver.compositionForFormula("H2O"))
    }

    @Test
    fun compositionForFormula_resolvesCO2() {
        assertEquals(mapOf("C" to 1, "O" to 2), resolver.compositionForFormula("CO2"))
    }

    @Test
    fun compositionForFormula_resolvesNaCl() {
        assertEquals(mapOf("Na" to 1, "Cl" to 1), resolver.compositionForFormula("NaCl"))
    }

    @Test
    fun compositionForFormula_fallsBackToParserForNonRecipeFormula() {
        assertEquals(mapOf("Ca" to 1, "H" to 2, "O" to 2), resolver.compositionForFormula("Ca(OH)2"))
    }

    @Test
    fun linksForFormula_dropsUnknownSymbolsWithoutCrash() {
        val links = resolver.linksForFormula("Xx2", emptyMap())

        assertTrue(links.isEmpty())
    }

    @Test
    fun compositionForFormula_returnsEmptyForInvalidFormula() {
        assertTrue(resolver.compositionForFormula("???").isEmpty())
    }
}
