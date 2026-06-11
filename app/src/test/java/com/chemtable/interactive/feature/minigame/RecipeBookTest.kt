package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.feature.minigame.engine.RecipeBook
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeBookTest {

    private val book = RecipeBook()

    private fun el(symbol: String, id: Long = 1L) =
        ElementBlock(id = id, atomicNumber = 0, symbol = symbol, nameKo = "", molarMass = 0.0)

    private fun mol(formula: String, composition: Map<String, Int>, id: Long = 1L) =
        MoleculeBlock(id = id, formula = formula, massScore = 0.0, composition = composition)

    @Test
    fun h_plus_h_makes_h2() {
        assertEquals("H2", book.match(el("H", 1), el("H", 2))?.productFormula)
    }

    @Test
    fun o_plus_o_makes_o2() {
        assertEquals("O2", book.match(el("O", 1), el("O", 2))?.productFormula)
    }

    @Test
    fun n_plus_n_makes_n2() {
        assertEquals("N2", book.match(el("N", 1), el("N", 2))?.productFormula)
    }

    @Test
    fun h2_plus_o_makes_h2o() {
        val h2 = mol("H2", mapOf("H" to 2))
        assertEquals("H2O", book.match(h2, el("O"))?.productFormula)
    }

    @Test
    fun c_plus_o2_makes_co2() {
        val o2 = mol("O2", mapOf("O" to 2))
        assertEquals("CO2", book.match(el("C"), o2)?.productFormula)
    }

    @Test
    fun na_plus_cl_makes_nacl() {
        assertEquals("NaCl", book.match(el("Na", 1), el("Cl", 2))?.productFormula)
    }

    @Test
    fun compositionForProduct_returnsKnownProductComposition() {
        assertEquals(mapOf("H" to 2, "O" to 1), book.compositionForProduct("H2O"))
        assertEquals(mapOf("C" to 1, "O" to 2), book.compositionForProduct("CO2"))
        assertEquals(mapOf("Na" to 1, "Cl" to 1), book.compositionForProduct("NaCl"))
    }

    @Test
    fun h_plus_na_has_no_match() {
        assertNull(book.match(el("H", 1), el("Na", 2)))
    }
}
