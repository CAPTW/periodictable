package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.util.FormulaParser
import com.chemtable.interactive.feature.minigame.engine.RecipeBook
import com.chemtable.interactive.feature.minigame.model.DiscoveredMolecule
import com.chemtable.interactive.feature.minigame.model.MoleculeElementLink
import javax.inject.Inject

/**
 * Builds ElementDetail link chips for generated formulas.
 *
 * RecipeBook composition is preferred for game-generated products. FormulaParser is only a
 * display/navigation fallback here; recipe matching remains owned by RecipeBook/BoardEngine.
 */
class MoleculeElementLinkResolver @Inject constructor(
    private val formulaParser: FormulaParser,
) {
    private val recipeBook = RecipeBook()

    fun discoveredMoleculesFor(formulas: List<String>, elements: List<Element>): List<DiscoveredMolecule> {
        val elementBySymbol = elements.associateBy { it.symbol }
        return formulas.map { formula ->
            DiscoveredMolecule(
                formula = formula,
                elementLinks = linksForFormula(formula, elementBySymbol),
            )
        }
    }

    fun linksForFormula(formula: String, elements: List<Element>): List<MoleculeElementLink> =
        linksForFormula(formula, elements.associateBy { it.symbol })

    internal fun linksForFormula(
        formula: String,
        elementBySymbol: Map<String, Element>,
    ): List<MoleculeElementLink> =
        compositionForFormula(formula).mapNotNull { (symbol, count) ->
            // Unknown parsed symbols have no atomicNumber target, so omit their chips.
            val element = elementBySymbol[symbol] ?: return@mapNotNull null
            MoleculeElementLink(
                symbol = symbol,
                count = count,
                atomicNumber = element.atomicNumber,
                displayName = element.nameKo.ifBlank { element.name },
            )
        }

    internal fun compositionForFormula(formula: String): Map<String, Int> {
        val normalized = formula.trim()
        if (normalized.isEmpty()) return emptyMap()

        recipeBook.compositionForProduct(normalized)?.let { return it }

        return runCatching {
            formulaParser.parse(normalized)
                .components
                .associate { component -> component.symbol to component.count }
                .filterValues { it > 0 }
        }.getOrDefault(emptyMap())
    }
}
