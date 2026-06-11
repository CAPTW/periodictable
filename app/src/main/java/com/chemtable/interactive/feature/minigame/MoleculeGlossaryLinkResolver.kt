package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.feature.minigame.model.MoleculeGlossaryLink
import javax.inject.Inject

/**
 * Builds glossary chips for generated molecule formulas from existing glossary inventory only.
 */
class MoleculeGlossaryLinkResolver @Inject constructor() {

    fun linksForFormula(formula: String, terms: List<GlossaryTerm>): List<MoleculeGlossaryLink> {
        val termById = terms.associateBy { it.id }
        return candidateTermIdsFor(formula).mapNotNull { termId ->
            val term = termById[termId] ?: return@mapNotNull null
            MoleculeGlossaryLink(
                termId = termId,
                label = term.termKo.takeIf { it.isNotBlank() }
                    ?: term.termEn.takeIf { it.isNotBlank() }
                    ?: termId,
            )
        }.take(MAX_LINKS_PER_FORMULA)
    }

    internal fun candidateTermIdsFor(formula: String): List<String> =
        termIdsByFormula[formula.trim()].orEmpty()

    private companion object {
        private const val MAX_LINKS_PER_FORMULA = 3

        private val termIdsByFormula = mapOf(
            "H2" to listOf("molecule", "molecular_formula", "covalent_bond"),
            "O2" to listOf("molecule", "molecular_formula", "covalent_bond"),
            "N2" to listOf("molecule", "molecular_formula", "covalent_bond"),
            "H2O" to listOf("molecule", "compound", "molar_mass", "chemical_formula"),
            "CO2" to listOf("molecule", "compound", "molar_mass", "chemical_formula"),
            "NaCl" to listOf("salt", "ionic_bond", "electronegativity", "chemical_formula"),
        )
    }
}
