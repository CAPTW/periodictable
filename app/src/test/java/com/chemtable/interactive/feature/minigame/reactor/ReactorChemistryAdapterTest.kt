package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.engine.FormulaMassResolver
import com.chemtable.interactive.feature.minigame.engine.RecipeBook
import com.chemtable.interactive.feature.minigame.reactor.adapter.ClassicRecipeBookReactorAdapter
import com.chemtable.interactive.feature.minigame.reactor.adapter.FormulaMassResolverReactorMassAdapter
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorProductSpecification
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorComposition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReactorChemistryAdapterTest {

    private val adapter = ClassicRecipeBookReactorAdapter(RecipeBook())

    @Test
    fun adapterDelegatesAllSixCanonicalMultisetRecipesWithoutDrift() {
        val expected = listOf(
            Triple(mapOf("H" to 2), "H2", "수소 분자"),
            Triple(mapOf("O" to 2), "O2", "산소 분자"),
            Triple(mapOf("N" to 2), "N2", "질소 분자"),
            Triple(mapOf("H" to 2, "O" to 1), "H2O", "물"),
            Triple(mapOf("C" to 1, "O" to 2), "CO2", "이산화탄소"),
            Triple(mapOf("Na" to 1, "Cl" to 1), "NaCl", "염화 소듐"),
        )

        assertEquals(6, RecipeBook.DEFAULT_RECIPES.size)
        expected.forEach { (composition, formula, name) ->
            assertEquals(
                ReactorProductSpecification(
                    formula = formula,
                    displayName = name,
                    composition = ReactorComposition.of(composition),
                ),
                adapter.findProduct(ReactorComposition.of(composition)),
            )
        }
    }

    @Test
    fun undefinedCompositionIsRejectedWithoutFormulaParsing() {
        assertNull(adapter.findProduct(ReactorComposition.of(mapOf("H" to 1, "Na" to 1))))
    }

    @Test
    fun massAdapterReturnsTheExistingResolverResultForProductFormula() {
        val massAdapter = FormulaMassResolverReactorMassAdapter(
            FormulaMassResolver { formula ->
                when (formula) {
                    "H2O" -> 18.015
                    else -> error("unexpected formula")
                }
            },
        )
        val water = ReactorProductSpecification(
            formula = "H2O",
            displayName = "물",
            composition = ReactorComposition.of(mapOf("H" to 2, "O" to 1)),
        )

        assertEquals(18.015, massAdapter.molarMassOf(water), 0.0)
    }
}
