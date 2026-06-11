package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.core.model.GlossaryCategory
import com.chemtable.interactive.core.model.GlossaryTerm
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoleculeGlossaryLinkResolverTest {

    private val resolver = MoleculeGlossaryLinkResolver()
    private val terms = listOf(
        term("molecule", "분자"),
        term("molecular_formula", "분자식"),
        term("covalent_bond", "공유 결합"),
        term("compound", "화합물"),
        term("molar_mass", "몰 질량"),
        term("chemical_formula", "화학식"),
        term("salt", "염"),
        term("ionic_bond", "이온 결합"),
        term("electronegativity", "전기음성도"),
    )

    @Test
    fun h2_linksToMoleculeMolecularFormulaAndCovalentBond() {
        assertEquals(
            listOf("molecule", "molecular_formula", "covalent_bond"),
            idsFor("H2"),
        )
    }

    @Test
    fun o2_linksToMoleculeMolecularFormulaAndCovalentBond() {
        assertEquals(
            listOf("molecule", "molecular_formula", "covalent_bond"),
            idsFor("O2"),
        )
    }

    @Test
    fun n2_linksToMoleculeMolecularFormulaAndCovalentBond() {
        assertEquals(
            listOf("molecule", "molecular_formula", "covalent_bond"),
            idsFor("N2"),
        )
    }

    @Test
    fun h2o_linksToMoleculeCompoundAndMolarMass() {
        assertEquals(
            listOf("molecule", "compound", "molar_mass"),
            idsFor("H2O"),
        )
    }

    @Test
    fun co2_linksToMoleculeCompoundAndMolarMass() {
        assertEquals(
            listOf("molecule", "compound", "molar_mass"),
            idsFor("CO2"),
        )
    }

    @Test
    fun nacl_linksToSaltIonicBondAndElectronegativity() {
        assertEquals(
            listOf("salt", "ionic_bond", "electronegativity"),
            idsFor("NaCl"),
        )
    }

    @Test
    fun h2o_candidateTermsIncludeChemicalFormulaAfterVisiblePriority() {
        assertEquals(
            listOf("molecule", "compound", "molar_mass", "chemical_formula"),
            resolver.candidateTermIdsFor("H2O"),
        )
    }

    @Test
    fun nacl_candidateTermsIncludeChemicalFormulaAfterVisiblePriority() {
        assertEquals(
            listOf("salt", "ionic_bond", "electronegativity", "chemical_formula"),
            resolver.candidateTermIdsFor("NaCl"),
        )
    }

    @Test
    fun unknownFormula_returnsEmptyList() {
        assertTrue(resolver.linksForFormula("XeF4", terms).isEmpty())
    }

    @Test
    fun missingTermInventory_excludesMissingTerm() {
        val withoutCovalentBond = terms.filterNot { it.id == "covalent_bond" }

        assertEquals(
            listOf("molecule", "molecular_formula"),
            idsFor("H2", withoutCovalentBond),
        )
    }

    @Test
    fun linksUseGlossaryKoreanTitleAsLabel() {
        val labels = resolver.linksForFormula("CO2", terms).map { it.label }

        assertEquals(listOf("분자", "화합물", "몰 질량"), labels)
    }

    @Test
    fun glossaryAsset_containsMinigameTargetTerms() {
        val ids = glossaryIds(glossaryAsset().readText())

        assertTrue(ids.containsAll(terms.map { it.id }))
    }

    @Test
    fun glossaryAsset_phase2ETermsReferenceExistingRelatedTerms() {
        val assetText = glossaryAsset().readText()
        val ids = glossaryIds(assetText)

        for (termId in listOf("molecule", "chemical_formula", "salt")) {
            val missing = relatedTermIdsFor(assetText, termId).filterNot { it in ids }

            assertTrue("$termId has missing related terms: $missing", missing.isEmpty())
        }
    }

    private fun idsFor(formula: String, sourceTerms: List<GlossaryTerm> = terms): List<String> =
        resolver.linksForFormula(formula, sourceTerms).map { it.termId }

    private fun term(id: String, label: String) = GlossaryTerm(
        id = id,
        termKo = label,
        termEn = id,
        definition = "",
        simpleExplanation = "",
        category = GlossaryCategory.GENERAL,
        interactiveType = null,
        relatedElements = emptyList(),
        relatedTerms = emptyList(),
    )

    private fun glossaryAsset(): File =
        listOf(
            File("src/main/assets/glossary.json"),
            File("app/src/main/assets/glossary.json"),
        ).first { it.isFile }

    private fun glossaryIds(assetText: String): Set<String> =
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(assetText)
            .map { it.groupValues[1] }
            .toSet()

    private fun relatedTermIdsFor(assetText: String, termId: String): List<String> {
        val entry = Regex(
            "\\{\\s*\"id\"\\s*:\\s*\"${Regex.escape(termId)}\"[\\s\\S]*?\"relatedTerms\"\\s*:\\s*\\[([\\s\\S]*?)]",
        ).find(assetText) ?: return emptyList()

        return Regex("\"([^\"]+)\"")
            .findAll(entry.groupValues[1])
            .map { it.groupValues[1] }
            .toList()
    }
}
