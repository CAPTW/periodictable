package com.chemtable.interactive.feature.minigame.dex

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementCategory
import com.chemtable.interactive.core.model.GlossaryCategory
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.core.model.StateOfMatter
import com.chemtable.interactive.core.util.FormulaParser
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.feature.minigame.MoleculeElementLinkResolver
import com.chemtable.interactive.feature.minigame.MoleculeGlossaryLinkResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class MoleculeDexUiStateTest {

    private val elementLinkResolver = MoleculeElementLinkResolver(FormulaParser())
    private val glossaryLinkResolver = MoleculeGlossaryLinkResolver()

    @Test
    fun buildUiState_withNoDiscoveries_returnsEmptyLoadedState() {
        val state = buildMoleculeDexUiState(
            discoveries = emptyList(),
            highScore = null,
            recentSessions = emptyList(),
            elements = emptyList(),
            glossaryTerms = emptyList(),
            elementLinkResolver = elementLinkResolver,
            glossaryLinkResolver = glossaryLinkResolver,
        )

        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertEquals(0, state.discoveredCount)
        assertEquals(null, state.highScore)
        assertEquals(emptyList<MoleculeDexSessionItem>(), state.recentSessions)
    }

    @Test
    fun errorState_isNotTreatedAsEmpty() {
        val state = MoleculeDexUiState(
            isLoading = false,
            errorMessage = "load failed",
        )

        assertFalse(state.isEmpty)
        assertEquals("load failed", state.errorMessage)
    }

    @Test
    fun buildUiState_mapsDiscoveriesHighScoreSessionsAndLinks() {
        val state = buildMoleculeDexUiState(
            discoveries = listOf(
                GameMoleculeDiscovery(
                    formula = "CO2",
                    firstDiscoveredAt = 100L,
                    lastDiscoveredAt = 300L,
                    discoveryCount = 2,
                ),
                GameMoleculeDiscovery(
                    formula = "H2O",
                    firstDiscoveredAt = 200L,
                    lastDiscoveredAt = 250L,
                    discoveryCount = 1,
                ),
            ),
            highScore = 900,
            modeHighScores = mapOf(
                "MISSION" to 700,
                "ENDLESS" to 900,
                "TIME_ATTACK" to 650,
            ),
            difficultyHighScores = mapOf(
                "BEGINNER" to 500,
                "INTERMEDIATE" to 900,
                "ADVANCED" to null,
            ),
            recentSessions = listOf(
                GameSession(
                    id = 7L,
                    score = 900,
                    success = true,
                    difficulty = "INTERMEDIATE",
                    mode = "TIME_ATTACK",
                    missionFormula = "H2O",
                    missionTargetCount = 2,
                    playedAt = 400L,
                    moleculesMade = listOf("CO2", "H2O"),
                ),
            ),
            elements = listOf(
                element(1, "H", "수소"),
                element(6, "C", "탄소"),
                element(8, "O", "산소"),
            ),
            glossaryTerms = listOf(
                term("molecule", "분자"),
                term("compound", "화합물"),
                term("molar_mass", "몰 질량"),
            ),
            elementLinkResolver = elementLinkResolver,
            glossaryLinkResolver = glossaryLinkResolver,
        )

        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(2, state.discoveredCount)
        assertEquals(900, state.highScore)
        assertEquals(listOf("CO2", "H2O"), state.discoveries.map { it.formula })

        val carbonDioxide = state.discoveries.first()
        assertEquals(2, carbonDioxide.discoveryCount)
        assertEquals(listOf("C", "O"), carbonDioxide.elementLinks.map { it.symbol })
        assertEquals(listOf("molecule", "compound", "molar_mass"), carbonDioxide.glossaryLinks.map { it.termId })

        val session = state.recentSessions.single()
        assertEquals(7L, session.id)
        assertEquals(900, session.score)
        assertEquals(true, session.success)
        assertEquals("INTERMEDIATE", session.difficulty)
        assertEquals("중급", session.difficultyLabel)
        assertEquals("TIME_ATTACK", session.mode)
        assertEquals("타임어택", session.modeLabel)
        assertEquals(listOf("CO2", "H2O"), session.moleculesMade)

        assertEquals(
            listOf(
                MoleculeDexModeScore("MISSION", "미션", 700),
                MoleculeDexModeScore("ENDLESS", "엔들리스", 900),
                MoleculeDexModeScore("TIME_ATTACK", "타임어택", 650),
            ),
            state.modeHighScores,
        )
        assertEquals(
            listOf(
                MoleculeDexDifficultyScore("BEGINNER", "초급", 500),
                MoleculeDexDifficultyScore("INTERMEDIATE", "중급", 900),
                MoleculeDexDifficultyScore("ADVANCED", "고급", null),
            ),
            state.difficultyHighScores,
        )
    }

    @Test
    fun difficultyLabelFor_unknownDifficultyFallsBackToOriginalValue() {
        assertEquals("CUSTOM", difficultyLabelFor("CUSTOM"))
        assertEquals("알 수 없음", difficultyLabelFor(""))
    }

    @Test
    fun modeLabelFor_mapsKnownModesAndFallsBackForUnknown() {
        assertEquals("미션", modeLabelFor("MISSION"))
        assertEquals("엔들리스", modeLabelFor("endless"))
        assertEquals("타임어택", modeLabelFor("time_attack"))
        assertEquals("CUSTOM", modeLabelFor("CUSTOM"))
        assertEquals("알 수 없음", modeLabelFor(""))
    }

    @Test
    fun formatDexTimestamp_usesProvidedTimezone() {
        assertEquals(
            "1970.01.01 00:00",
            formatDexTimestamp(0L, TimeZone.getTimeZone("UTC")),
        )
        assertEquals(
            "1970.01.01 09:00",
            formatDexTimestamp(0L, TimeZone.getTimeZone("Asia/Seoul")),
        )
    }

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

    private fun element(atomicNumber: Int, symbol: String, nameKo: String): Element = Element(
        atomicNumber = atomicNumber,
        symbol = symbol,
        name = symbol,
        nameKo = nameKo,
        latinName = null,
        latinPronunciation = null,
        englishPronunciation = null,
        discoveryYear = null,
        discoverer = null,
        discoveryCountry = null,
        costPer100gUsd = null,
        costReferenceDate = null,
        casNumber = null,
        pubchemCid = null,
        rtecsNumber = null,
        protonCount = null,
        neutronCount = null,
        electronCount = null,
        electronShells = null,
        commonIonCharge = null,
        ionizationPotential = null,
        ionizationPossibility = null,
        covalentRadius = null,
        vanDerWaalsRadius = null,
        block = null,
        meltingPoint = null,
        boilingPoint = null,
        heatOfFusion = null,
        liquidDensity = null,
        electricalConductivity = null,
        electricalType = null,
        resistivity = null,
        superconductingTemperature = null,
        magnetism = null,
        volumeMagneticSusceptibility = null,
        massMagneticSusceptibility = null,
        molarMagneticSusceptibility = null,
        crystalStructure = null,
        crystalSystem = null,
        latticeA = null,
        latticeB = null,
        latticeC = null,
        latticeAlpha = null,
        latticeBeta = null,
        latticeGamma = null,
        crystalHabit = null,
        debyeTemperature = null,
        hardnessBrinell = null,
        hardnessMohs = null,
        hardnessVickers = null,
        bulkModulus = null,
        youngModulus = null,
        poissonRatio = null,
        shearModulus = null,
        speedOfSound = null,
        refractiveIndex = null,
        electronAffinity = null,
        standardElectrodePotential = null,
        radioactivityLevel = null,
        reactivityLevel = null,
        hazardHealth = null,
        hazardFlammability = null,
        hazardReactivity = null,
        hazardSpecial = null,
        abundanceUniverse = null,
        abundanceSun = null,
        abundanceOcean = null,
        abundanceHuman = null,
        abundanceCrust = null,
        abundanceMeteorite = null,
        dataSource = null,
        dataLicense = null,
        dataUpdatedAt = null,
        dataConfidence = null,
        category = ElementCategory.NONMETAL,
        stateOfMatter = StateOfMatter.GAS,
        electronConfiguration = "",
        molarMass = 1.0,
        heatOfVaporization = null,
        specificHeatCapacity = null,
        thermalExpansionCoefficient = null,
        halfLife = null,
        neutronCrossSection = null,
        barn = null,
        thermalConductivity = null,
        electronegativity = null,
        atomicRadius = null,
        period = 1,
        group = 1,
    )
}
