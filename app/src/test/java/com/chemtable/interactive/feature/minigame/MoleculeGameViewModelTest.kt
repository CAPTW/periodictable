package com.chemtable.interactive.feature.minigame

import androidx.lifecycle.SavedStateHandle
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementCategory
import com.chemtable.interactive.core.model.ElementProperty
import com.chemtable.interactive.core.model.StateOfMatter
import com.chemtable.interactive.core.model.GlossaryCategory
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.core.util.FormulaParser
import com.chemtable.interactive.core.util.MolarMassCalculator
import com.chemtable.interactive.domain.repository.ElementRepository
import com.chemtable.interactive.domain.repository.GlossaryRepository
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import com.chemtable.interactive.domain.usecase.GetGlossaryUseCase
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameEvent
import com.chemtable.interactive.feature.minigame.model.GamePhase
import com.chemtable.interactive.feature.minigame.model.GameUiState
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

class MoleculeGameViewModelTest {

    private fun createElement(
        atomicNumber: Int,
        symbol: String,
        name: String = "",
        nameKo: String = "",
        category: ElementCategory = ElementCategory.NONMETAL,
        molarMass: Double = 0.0,
        period: Int = 1,
        group: Int = 1
    ): Element {
        return Element(
            atomicNumber = atomicNumber,
            symbol = symbol,
            name = name,
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
            category = category,
            stateOfMatter = StateOfMatter.GAS,
            electronConfiguration = "",
            molarMass = molarMass,
            heatOfVaporization = null,
            specificHeatCapacity = null,
            thermalExpansionCoefficient = null,
            halfLife = null,
            neutronCrossSection = null,
            barn = null,
            thermalConductivity = null,
            electronegativity = null,
            atomicRadius = null,
            period = period,
            group = group
        )
    }

    private val elements = listOf(
        createElement(1, "H", name = "Hydrogen", nameKo = "수소", category = ElementCategory.NONMETAL, molarMass = 1.008, period = 1, group = 1),
        createElement(8, "O", name = "Oxygen", nameKo = "산소", category = ElementCategory.NONMETAL, molarMass = 15.999, period = 2, group = 16),
        createElement(11, "Na", name = "Sodium", nameKo = "나트륨", category = ElementCategory.ALKALI_METAL, molarMass = 22.99, period = 3, group = 1),
        createElement(17, "Cl", name = "Chlorine", nameKo = "염소", category = ElementCategory.HALOGEN, molarMass = 35.45, period = 3, group = 17)
    )

    private val terms = listOf(
        GlossaryTerm("molecule", "분자", "Molecule", "", "", GlossaryCategory.GENERAL, null, emptyList(), emptyList()),
        GlossaryTerm("compound", "화합물", "Compound", "", "", GlossaryCategory.GENERAL, null, emptyList(), emptyList()),
        GlossaryTerm("molar_mass", "몰 질량", "Molar Mass", "", "", GlossaryCategory.GENERAL, null, emptyList(), emptyList()),
        GlossaryTerm("salt", "염", "Salt", "", "", GlossaryCategory.GENERAL, null, emptyList(), emptyList())
    )

    private val elementRepository = object : ElementRepository {
        override fun getElements(): Flow<List<Element>> = flowOf(elements)
        override fun getElementByAtomicNumber(number: Int): Flow<Element?> = flowOf(elements.find { it.atomicNumber == number })
        override fun searchElements(query: String): Flow<List<Element>> = flowOf(emptyList())
        override fun searchBySymbol(symbol: String): Flow<List<Element>> = flowOf(emptyList())
        override fun filterByProperty(property: ElementProperty, min: Double, max: Double): Flow<List<Element>> = flowOf(emptyList())
    }

    private val glossaryRepository = object : GlossaryRepository {
        override fun getAllTerms(): Flow<List<GlossaryTerm>> = flowOf(terms)
        override fun getTermsByCategory(category: String): Flow<List<GlossaryTerm>> = flowOf(emptyList())
        override fun searchTerms(query: String): Flow<List<GlossaryTerm>> = flowOf(emptyList())
        override suspend fun setBookmark(termId: String, bookmarked: Boolean) {}
    }

    private val getElementsUseCase = GetElementsUseCase(elementRepository)
    private val getGlossaryUseCase = GetGlossaryUseCase(glossaryRepository)

    private val parser = FormulaParser()
    private val molarMassCalculator = MolarMassCalculator(parser)
    private val elementLinkResolver = MoleculeElementLinkResolver(parser)
    private val glossaryLinkResolver = MoleculeGlossaryLinkResolver()

    private lateinit var viewModel: MoleculeGameViewModel



    @Before
    fun setUp() {
        viewModel = MoleculeGameViewModel(
            savedStateHandle = SavedStateHandle(),
            getElementsUseCase = getElementsUseCase,
            getGlossaryUseCase = getGlossaryUseCase,
            molarMassCalculator = molarMassCalculator,
            moleculeElementLinkResolver = elementLinkResolver,
            moleculeGlossaryLinkResolver = glossaryLinkResolver
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun forceSetState(transform: (GameUiState) -> GameUiState) {
        val field: Field = MoleculeGameViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val uiStateFlow = field.get(viewModel) as MutableStateFlow<GameUiState>
        uiStateFlow.value = transform(uiStateFlow.value)
    }

    private fun forceSetPlayingBoard(board: BoardState) {
        forceSetState { it.copy(phase = GamePhase.PLAYING, board = board) }
    }

    @Test
    fun blockTapped_withMoleculeBlock_updatesSelectedMoleculeSheet() = runBlocking {
        // Given
        val blockId = 99L
        val formula = "H2O"
        val mBlock = MoleculeBlock(
            id = blockId,
            formula = formula,
            massScore = 18.015,
            composition = mapOf("H" to 2, "O" to 1)
        )
        val grid = listOf(
            listOf(mBlock, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null)
        )
        val board = BoardState(size = 4, grid = grid)
        forceSetPlayingBoard(board)

        // When
        viewModel.onEvent(GameEvent.BlockTapped(blockId))

        // Then
        val sheet = viewModel.uiState.value.selectedMoleculeSheet
        assertNotNull(sheet)
        val nonNullSheet = sheet!!
        assertEquals(blockId, nonNullSheet.blockId)
        assertEquals(formula, nonNullSheet.formula)
        assertEquals(18.015, nonNullSheet.molarMass, 0.001)
        // Verify resolved element/glossary links are populated
        assertEquals(2, nonNullSheet.elementLinks.size) // H and O
        // H2O mappings: molecule, compound, molar_mass, chemical_formula
        // Links resolution take(3) applies -> molecule, compound, molar_mass
        assertEquals(3, nonNullSheet.glossaryLinks.size)
    }

    @Test
    fun blockTapped_withElementBlock_doesNotChangeSheet() = runBlocking {
        // Given
        val blockId = 88L
        val eBlock = ElementBlock(
            id = blockId,
            atomicNumber = 1,
            symbol = "H",
            molarMass = 1.008
        )
        val grid = listOf(
            listOf(eBlock, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null)
        )
        val board = BoardState(size = 4, grid = grid)
        forceSetPlayingBoard(board)

        // When
        viewModel.onEvent(GameEvent.BlockTapped(blockId))

        // Then
        assertNull(viewModel.uiState.value.selectedMoleculeSheet)
    }

    @Test
    fun blockTapped_withInvalidBlockId_doesNotChangeSheet() = runBlocking {
        // Given
        val grid = listOf(
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null)
        )
        val board = BoardState(size = 4, grid = grid)
        forceSetPlayingBoard(board)

        // When
        viewModel.onEvent(GameEvent.BlockTapped(9999L))

        // Then
        assertNull(viewModel.uiState.value.selectedMoleculeSheet)
    }

    @Test
    fun closeMoleculeSheet_resetsSelectedMoleculeSheetToNull() = runBlocking {
        // Given
        val blockId = 99L
        val mBlock = MoleculeBlock(
            id = blockId,
            formula = "CO2",
            massScore = 44.01,
            composition = mapOf("C" to 1, "O" to 2)
        )
        val grid = listOf(
            listOf(mBlock, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null)
        )
        val board = BoardState(size = 4, grid = grid)
        forceSetPlayingBoard(board)

        // Tapping to show sheet first
        viewModel.onEvent(GameEvent.BlockTapped(blockId))
        assertNotNull(viewModel.uiState.value.selectedMoleculeSheet)

        // When
        viewModel.onEvent(GameEvent.CloseMoleculeSheet)

        // Then
        assertNull(viewModel.uiState.value.selectedMoleculeSheet)
    }

    @Test
    fun startGame_resetsSelectedMoleculeSheetToNull() = runBlocking {
        // Given
        val blockId = 99L
        val mBlock = MoleculeBlock(
            id = blockId,
            formula = "H2O",
            massScore = 18.015,
            composition = mapOf("H" to 2, "O" to 1)
        )
        val grid = listOf(
            listOf(mBlock, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null)
        )
        val board = BoardState(size = 4, grid = grid)
        forceSetPlayingBoard(board)
        viewModel.onEvent(GameEvent.BlockTapped(blockId))
        assertNotNull(viewModel.uiState.value.selectedMoleculeSheet)

        // When
        viewModel.onEvent(GameEvent.StartGame)

        // Then
        assertNull(viewModel.uiState.value.selectedMoleculeSheet)
        assertEquals(GamePhase.PLAYING, viewModel.uiState.value.phase)
    }
}
