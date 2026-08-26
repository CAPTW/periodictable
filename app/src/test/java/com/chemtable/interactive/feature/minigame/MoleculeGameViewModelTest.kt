package com.chemtable.interactive.feature.minigame

import androidx.lifecycle.SavedStateHandle
import com.chemtable.interactive.core.model.AppSettings
import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementCategory
import com.chemtable.interactive.core.model.ElementProperty
import com.chemtable.interactive.core.model.StateOfMatter
import com.chemtable.interactive.core.model.GlossaryCategory
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.core.util.FormulaParser
import com.chemtable.interactive.core.util.MolarMassCalculator
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.domain.repository.ElementRepository
import com.chemtable.interactive.domain.repository.GameStatsRepository
import com.chemtable.interactive.domain.repository.GlossaryRepository
import com.chemtable.interactive.domain.repository.SettingsRepository
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import com.chemtable.interactive.domain.usecase.GetGlossaryUseCase
import com.chemtable.interactive.domain.usecase.RecordGameResultUseCase
import com.chemtable.interactive.feature.minigame.engine.BoardEngine
import com.chemtable.interactive.feature.minigame.engine.FormulaMassResolver
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.Direction
import com.chemtable.interactive.feature.minigame.model.Difficulty
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameEffect
import com.chemtable.interactive.feature.minigame.model.GameEvent
import com.chemtable.interactive.feature.minigame.model.GameMode
import com.chemtable.interactive.feature.minigame.model.GamePhase
import com.chemtable.interactive.feature.minigame.model.GameUiState
import com.chemtable.interactive.feature.minigame.model.MissionTarget
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import com.chemtable.interactive.feature.minigame.model.SpawnableElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field
import kotlin.random.Random

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
        createElement(6, "C", name = "Carbon", nameKo = "탄소", category = ElementCategory.NONMETAL, molarMass = 12.011, period = 2, group = 14),
        createElement(7, "N", name = "Nitrogen", nameKo = "질소", category = ElementCategory.NONMETAL, molarMass = 14.007, period = 2, group = 15),
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
    private lateinit var gameStatsRepository: FakeGameStatsRepository
    private lateinit var settingsRepository: FakeSettingsRepository

    @Before
    fun setUp() {
        gameStatsRepository = FakeGameStatsRepository()
        settingsRepository = FakeSettingsRepository()
        viewModel = createViewModel(settingsRepository)
    }

    private fun createViewModel(settings: SettingsRepository): MoleculeGameViewModel =
        MoleculeGameViewModel(
            savedStateHandle = SavedStateHandle(),
            getElementsUseCase = getElementsUseCase,
            getGlossaryUseCase = getGlossaryUseCase,
            recordGameResultUseCase = RecordGameResultUseCase(gameStatsRepository),
            settingsRepository = settings,
            molarMassCalculator = molarMassCalculator,
            moleculeElementLinkResolver = elementLinkResolver,
            moleculeGlossaryLinkResolver = glossaryLinkResolver
        )

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

    private fun forceSetEngine(engine: BoardEngine) {
        val field: Field = MoleculeGameViewModel::class.java.getDeclaredField("engine")
        field.isAccessible = true
        field.set(viewModel, engine)
    }

    private fun h2oSuccessBoard(): BoardState {
        val firstHydrogenPair = MoleculeBlock(
            id = 1L,
            formula = "H2",
            massScore = 2.016,
            composition = mapOf("H" to 2)
        )
        val firstOxygen = ElementBlock(
            id = 2L,
            atomicNumber = 8,
            symbol = "O",
            molarMass = 15.999
        )
        val secondHydrogenPair = MoleculeBlock(
            id = 3L,
            formula = "H2",
            massScore = 2.016,
            composition = mapOf("H" to 2)
        )
        val secondOxygen = ElementBlock(
            id = 4L,
            atomicNumber = 8,
            symbol = "O",
            molarMass = 15.999
        )
        return BoardState(
            size = 4,
            grid = listOf(
                listOf(firstHydrogenPair, firstOxygen, secondHydrogenPair, secondOxygen),
                listOf(null, null, null, null),
                listOf(null, null, null, null),
                listOf(null, null, null, null)
            )
        )
    }

    private fun forceSetH2oMissionBoard(score: Int = 0) {
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                board = h2oSuccessBoard(),
                score = score,
                combo = 0,
                missionTarget = MissionTarget(formula = "H2O", count = 2, progress = 0),
                discoveredMolecules = emptyList(),
                resultSuccess = false,
                selectedMoleculeSheet = null,
            )
        }
    }

    private fun gameOverAfterMoveBoard(): BoardState {
        fun carbon(id: Long) = ElementBlock(
            id = id,
            atomicNumber = 6,
            symbol = "C",
            molarMass = 12.011
        )
        fun sodium(id: Long) = ElementBlock(
            id = id,
            atomicNumber = 11,
            symbol = "Na",
            molarMass = 22.99
        )
        return BoardState(
            size = 4,
            grid = listOf(
                listOf(null, carbon(1), sodium(2), carbon(3)),
                listOf(sodium(4), carbon(5), sodium(6), carbon(7)),
                listOf(carbon(8), sodium(9), carbon(10), sodium(11)),
                listOf(sodium(12), carbon(13), sodium(14), carbon(15))
            )
        )
    }

    private fun alreadyGameOverBoard(): BoardState {
        fun carbon(id: Long) = ElementBlock(
            id = id,
            atomicNumber = 6,
            symbol = "C",
            molarMass = 12.011
        )
        fun sodium(id: Long) = ElementBlock(
            id = id,
            atomicNumber = 11,
            symbol = "Na",
            molarMass = 22.99
        )
        return BoardState(
            size = 4,
            grid = listOf(
                listOf(carbon(1), sodium(2), carbon(3), sodium(4)),
                listOf(sodium(5), carbon(6), sodium(7), carbon(8)),
                listOf(carbon(9), sodium(10), carbon(11), sodium(12)),
                listOf(sodium(13), carbon(14), sodium(15), carbon(16))
            )
        )
    }

    private fun validNonMissionMoveBoard(): BoardState {
        val firstHydrogen = ElementBlock(
            id = 1L,
            atomicNumber = 1,
            symbol = "H",
            molarMass = 1.008
        )
        val secondHydrogen = ElementBlock(
            id = 2L,
            atomicNumber = 1,
            symbol = "H",
            molarMass = 1.008
        )
        return BoardState(
            size = 4,
            grid = listOf(
                listOf(firstHydrogen, secondHydrogen, null, null),
                listOf(null, null, null, null),
                listOf(null, null, null, null),
                listOf(null, null, null, null)
            )
        )
    }

    private fun noRecipeGameOverEngine(): BoardEngine = BoardEngine(
        resolver = FormulaMassResolver { 0.0 },
        spawnPool = listOf(
            SpawnableElement(
                atomicNumber = 6,
                symbol = "C",
                nameKo = "탄소",
                molarMass = 12.011,
                category = ElementCategory.NONMETAL
            )
        ),
        random = Random(0L)
    )

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

    @Test
    fun selectDifficulty_updatesStateWhileIntro() = runBlocking {
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))

        assertEquals(Difficulty.INTERMEDIATE, viewModel.uiState.value.difficulty)
        assertEquals(GamePhase.INTRO, viewModel.uiState.value.phase)
    }

    @Test
    fun selectDifficulty_isIgnoredWhilePlaying() = runBlocking {
        forceSetState { it.copy(phase = GamePhase.PLAYING, difficulty = Difficulty.BEGINNER) }

        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.ADVANCED))

        assertEquals(Difficulty.BEGINNER, viewModel.uiState.value.difficulty)
    }

    @Test
    fun initialState_usesMissionMode() = runBlocking {
        assertEquals(GameMode.MISSION, viewModel.uiState.value.mode)
        assertEquals(GamePhase.INTRO, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.timeLeftMillis)
        assertNull(viewModel.uiState.value.timeLimitMillis)
        assertNull(viewModel.uiState.value.lastTimerTickAtMillis)
    }

    @Test
    fun gameMode_includesTimeAttack() = runBlocking {
        assertTrue(GameMode.entries.contains(GameMode.TIME_ATTACK))
    }

    @Test
    fun selectMode_updatesStateWhileIntro() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.ENDLESS))

        assertEquals(GameMode.ENDLESS, viewModel.uiState.value.mode)
        assertEquals(GamePhase.INTRO, viewModel.uiState.value.phase)
    }

    @Test
    fun selectMode_isIgnoredWhilePlaying() = runBlocking {
        forceSetState { it.copy(phase = GamePhase.PLAYING, mode = GameMode.MISSION) }

        viewModel.onEvent(GameEvent.SelectMode(GameMode.ENDLESS))

        assertEquals(GameMode.MISSION, viewModel.uiState.value.mode)
    }

    @Test
    fun startGame_keepsSelectedModeForSessionState() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.ENDLESS))

        viewModel.onEvent(GameEvent.StartGame)

        assertEquals(GamePhase.PLAYING, viewModel.uiState.value.phase)
        assertEquals(GameMode.ENDLESS, viewModel.uiState.value.mode)
    }

    @Test
    fun startGame_afterSelectingEndlessClearsMissionAndMoveLimit() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.ENDLESS))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.ADVANCED))

        viewModel.onEvent(GameEvent.StartGame)

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(GameMode.ENDLESS, state.mode)
        assertEquals(Difficulty.ADVANCED, state.difficulty)
        assertNull(state.missionTarget)
        assertNull(state.movesLeft)
    }

    @Test
    fun startGame_afterSelectingTimeAttackSetsTimerAndClearsMissionAndMoveLimit() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))

        viewModel.onEvent(GameEvent.StartGame)

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(GameMode.TIME_ATTACK, state.mode)
        assertEquals(Difficulty.INTERMEDIATE, state.difficulty)
        assertNull(state.missionTarget)
        assertNull(state.movesLeft)
        assertEquals(90_000L, state.timeLimitMillis)
        assertEquals(90_000L, state.timeLeftMillis)
        assertNull(state.lastTimerTickAtMillis)
    }

    @Test
    fun timeAttackFirstTimerTickSetsLastTickWithoutDecrementing() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))
        viewModel.onEvent(GameEvent.StartGame)
        viewModel.onEvent(GameEvent.SkipTutorial)

        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 1_000L))

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(90_000L, state.timeLeftMillis)
        assertEquals(1_000L, state.lastTimerTickAtMillis)
    }

    @Test
    fun timeAttackSecondTimerTickDecrementsByElapsedMillis() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))
        viewModel.onEvent(GameEvent.StartGame)
        viewModel.onEvent(GameEvent.SkipTutorial)

        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 1_000L))
        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 2_250L))

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(88_750L, state.timeLeftMillis)
        assertEquals(2_250L, state.lastTimerTickAtMillis)
    }

    @Test
    fun timeAttackTimerTickIsIgnoredWhilePaused() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))
        viewModel.onEvent(GameEvent.StartGame)
        viewModel.onEvent(GameEvent.SkipTutorial)
        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 1_000L))

        viewModel.onEvent(GameEvent.Pause)
        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 5_000L))

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PAUSED, state.phase)
        assertEquals(90_000L, state.timeLeftMillis)
        assertNull(state.lastTimerTickAtMillis)
    }

    @Test
    fun timeAttackResumeResetsTickBaselineWithoutPausedElapsed() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))
        viewModel.onEvent(GameEvent.StartGame)
        viewModel.onEvent(GameEvent.SkipTutorial)
        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 1_000L))
        viewModel.onEvent(GameEvent.Pause)
        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 5_000L))

        viewModel.onEvent(GameEvent.Resume)
        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 6_000L))

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(90_000L, state.timeLeftMillis)
        assertEquals(6_000L, state.lastTimerTickAtMillis)
    }

    @Test
    fun timeAttackTimerReachingZeroEndsAndRecordsTimeAttackResult() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.ADVANCED))
        viewModel.onEvent(GameEvent.StartGame)
        viewModel.onEvent(GameEvent.SkipTutorial)

        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 1_000L))
        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 61_000L))

        val state = viewModel.uiState.value
        assertEquals(GamePhase.RESULT, state.phase)
        assertEquals(0L, state.timeLeftMillis)
        assertEquals(false, state.resultSuccess)
        val record = gameStatsRepository.records.single()
        assertEquals("TIME_ATTACK", record.mode)
        assertEquals(false, record.success)
        assertNull(record.missionFormula)
        assertNull(record.missionTargetCount)
    }

    @Test
    fun timeAttackTimerTickIsIgnoredWhileTutorialIsVisible() = runBlocking {
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))
        viewModel.onEvent(GameEvent.StartGame)

        viewModel.onEvent(GameEvent.TimerTick(nowMillis = 5_000L))

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(true, state.showTutorial)
        assertEquals(90_000L, state.timeLeftMillis)
        assertNull(state.lastTimerTickAtMillis)
    }

    @Test
    fun timeAttackMergeDoesNotApplyMissionSuccessBonus() = runBlocking {
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                mode = GameMode.TIME_ATTACK,
                board = h2oSuccessBoard(),
                score = 0,
                combo = 0,
                missionTarget = null,
                movesLeft = null,
                timeLeftMillis = 90_000L,
                timeLimitMillis = 90_000L,
                lastTimerTickAtMillis = 1_000L,
                discoveredMolecules = emptyList(),
                resultSuccess = false,
                selectedMoleculeSheet = null,
            )
        }

        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        assertEquals(GamePhase.PLAYING, viewModel.uiState.value.phase)
        assertEquals(36, viewModel.uiState.value.score)
        assertEquals(emptyList<GameResultRecord>(), gameStatsRepository.records)
    }

    @Test
    fun startGame_afterSelectingIntermediateUsesIntermediateMissionConfig() = runBlocking {
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))

        viewModel.onEvent(GameEvent.StartGame)

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(Difficulty.INTERMEDIATE, state.difficulty)
        assertEquals(MissionTarget(formula = "CO2", count = 1, progress = 0), state.missionTarget)
        assertNull(state.movesLeft)
    }

    @Test
    fun startGame_afterSelectingAdvancedSetsMoveLimit() = runBlocking {
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.ADVANCED))

        viewModel.onEvent(GameEvent.StartGame)

        val state = viewModel.uiState.value
        assertEquals(GamePhase.PLAYING, state.phase)
        assertEquals(Difficulty.ADVANCED, state.difficulty)
        assertEquals(MissionTarget(formula = "CO2", count = 2, progress = 0), state.missionTarget)
        assertEquals(36, state.movesLeft)
    }

    @Test
    fun validMove_decreasesMovesLeftWhenMoveLimitExists() = runBlocking {
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                board = validNonMissionMoveBoard(),
                difficulty = Difficulty.ADVANCED,
                missionTarget = MissionTarget(formula = "CO2", count = 2, progress = 0),
                movesLeft = 36,
            )
        }

        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        assertEquals(35, viewModel.uiState.value.movesLeft)
        assertEquals(GamePhase.PLAYING, viewModel.uiState.value.phase)
    }

    @Test
    fun invalidMove_doesNotDecreaseMovesLeft() = runBlocking {
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                board = BoardState.empty(4),
                difficulty = Difficulty.ADVANCED,
                missionTarget = MissionTarget(formula = "CO2", count = 2, progress = 0),
                movesLeft = 36,
            )
        }

        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        assertEquals(36, viewModel.uiState.value.movesLeft)
        assertEquals(GamePhase.PLAYING, viewModel.uiState.value.phase)
    }

    @Test
    fun moveLimitReachingZeroEndsAsFailedResultWhenMissionIncomplete() = runBlocking {
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                board = validNonMissionMoveBoard(),
                difficulty = Difficulty.ADVANCED,
                missionTarget = MissionTarget(formula = "CO2", count = 2, progress = 0),
                movesLeft = 1,
                resultSuccess = false,
            )
        }

        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        assertEquals(0, viewModel.uiState.value.movesLeft)
        assertEquals(GamePhase.RESULT, viewModel.uiState.value.phase)
        assertEquals(false, viewModel.uiState.value.resultSuccess)
    }

    @Test
    fun resultRecordingUsesSelectedDifficulty() = runBlocking {
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.INTERMEDIATE))
        forceSetH2oMissionBoard()

        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        val record = gameStatsRepository.records.single()
        assertEquals("INTERMEDIATE", record.difficulty)
        assertEquals("MISSION", record.mode)
    }

    @Test
    fun endlessMergeDoesNotApplyMissionSuccessBonus() = runBlocking {
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                mode = GameMode.ENDLESS,
                board = h2oSuccessBoard(),
                score = 0,
                combo = 0,
                missionTarget = null,
                movesLeft = null,
                discoveredMolecules = emptyList(),
                resultSuccess = false,
                selectedMoleculeSheet = null,
            )
        }

        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        assertEquals(GamePhase.PLAYING, viewModel.uiState.value.phase)
        assertEquals(36, viewModel.uiState.value.score)
        assertEquals(emptyList<GameResultRecord>(), gameStatsRepository.records)
    }

    @Test
    fun endlessGameOver_recordsEndlessResultWithoutMissionTarget() = runBlocking {
        forceSetEngine(noRecipeGameOverEngine())
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                mode = GameMode.ENDLESS,
                board = alreadyGameOverBoard(),
                score = 77,
                combo = 0,
                missionTarget = null,
                movesLeft = null,
                discoveredMolecules = emptyList(),
                resultSuccess = false,
                selectedMoleculeSheet = null,
            )
        }

        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        assertEquals(GamePhase.RESULT, viewModel.uiState.value.phase)
        assertEquals(false, viewModel.uiState.value.resultSuccess)
        val record = gameStatsRepository.records.single()
        assertEquals(77, record.score)
        assertEquals(false, record.success)
        assertEquals("BEGINNER", record.difficulty)
        assertEquals("ENDLESS", record.mode)
        assertNull(record.missionFormula)
        assertNull(record.missionTargetCount)
        assertEquals(emptyList<String>(), record.moleculesMade)
    }

    @Test
    fun restartKeepsSelectedDifficultyForNextGame() = runBlocking {
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.ADVANCED))
        viewModel.onEvent(GameEvent.StartGame)

        viewModel.onEvent(GameEvent.Restart)

        assertEquals(Difficulty.ADVANCED, viewModel.uiState.value.difficulty)
        assertEquals(36, viewModel.uiState.value.movesLeft)
    }

    @Test
    fun swipeIntoResult_recordsGameResultOnceWithResultFields() = runBlocking {
        // Given
        val before = System.currentTimeMillis()
        forceSetH2oMissionBoard(score = 12)

        // When
        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))
        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))
        val after = System.currentTimeMillis()

        // Then
        assertEquals(GamePhase.RESULT, viewModel.uiState.value.phase)
        assertEquals(1, gameStatsRepository.records.size)
        val record = gameStatsRepository.records.single()
        assertEquals(68, record.score)
        assertEquals(true, record.success)
        assertEquals("BEGINNER", record.difficulty)
        assertEquals("MISSION", record.mode)
        assertEquals("H2O", record.missionFormula)
        assertEquals(2, record.missionTargetCount)
        assertEquals(listOf("H2O", "H2O"), record.moleculesMade)
        assertTrue(record.playedAt in before..after)
    }

    @Test
    fun restart_resetsResultRecordGuardForNextSession() = runBlocking {
        // Given
        forceSetH2oMissionBoard()
        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))
        assertEquals(1, gameStatsRepository.records.size)

        // When
        viewModel.onEvent(GameEvent.Restart)
        forceSetH2oMissionBoard()
        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        // Then
        assertEquals(2, gameStatsRepository.records.size)
    }

    @Test
    fun recordFailure_doesNotCrashResultTransition() = runBlocking {
        // Given
        gameStatsRepository.throwOnRecord = true
        forceSetH2oMissionBoard()

        // When
        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        // Then
        assertEquals(GamePhase.RESULT, viewModel.uiState.value.phase)
        assertEquals(1, gameStatsRepository.recordAttempts)
        assertEquals(emptyList<GameResultRecord>(), gameStatsRepository.records)
    }

    @Test
    fun swipeIntoGameOver_recordsFailedGameResult() = runBlocking {
        // Given
        forceSetEngine(noRecipeGameOverEngine())
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                board = gameOverAfterMoveBoard(),
                score = 77,
                combo = 0,
                missionTarget = MissionTarget(formula = "H2O", count = 2, progress = 0),
                discoveredMolecules = emptyList(),
                resultSuccess = false,
                selectedMoleculeSheet = null,
            )
        }

        // When
        viewModel.onEvent(GameEvent.Swipe(Direction.LEFT))

        // Then
        assertEquals(GamePhase.RESULT, viewModel.uiState.value.phase)
        assertEquals(1, gameStatsRepository.records.size)
        val record = gameStatsRepository.records.single()
        assertEquals(77, record.score)
        assertEquals(false, record.success)
        assertEquals("BEGINNER", record.difficulty)
        assertEquals("MISSION", record.mode)
        assertEquals("H2O", record.missionFormula)
        assertEquals(2, record.missionTargetCount)
        assertEquals(emptyList<String>(), record.moleculesMade)
    }

    // --- Result-overlay molecule actions: GameEvent.Open* -> navigation GameEffect mapping (M7) ---

    @Test
    fun openCalculator_emitsNavigateToCalculatorWithFormula() = runBlocking {
        viewModel.onEvent(GameEvent.OpenCalculator("H2O"))
        val effect = withTimeoutOrNull(1000) { viewModel.effects.first() }
        assertEquals(GameEffect.NavigateToCalculator("H2O"), effect)
    }

    @Test
    fun openElement_withPositiveAtomicNumber_emitsNavigateToElement() = runBlocking {
        viewModel.onEvent(GameEvent.OpenElement(8))
        val effect = withTimeoutOrNull(1000) { viewModel.effects.first() }
        assertEquals(GameEffect.NavigateToElement(8), effect)
    }

    @Test
    fun openElement_withNonPositiveAtomicNumber_emitsNoEffect() = runBlocking {
        viewModel.onEvent(GameEvent.OpenElement(0))
        val effect = withTimeoutOrNull(200) { viewModel.effects.first() }
        assertNull(effect)
    }

    @Test
    fun openGlossary_withKnownTerm_emitsNavigateToGlossary() = runBlocking {
        // "molecule" is one of the seeded glossary terms (see `terms`).
        viewModel.onEvent(GameEvent.OpenGlossary("molecule"))
        val effect = withTimeoutOrNull(1000) { viewModel.effects.first() }
        assertEquals(GameEffect.NavigateToGlossary("molecule"), effect)
    }

    @Test
    fun openGlossary_withUnknownTerm_emitsNoEffect() = runBlocking {
        viewModel.onEvent(GameEvent.OpenGlossary("does_not_exist"))
        val effect = withTimeoutOrNull(200) { viewModel.effects.first() }
        assertNull(effect)
    }

    @Test
    fun boardSizeDefaultsToFourAndRestoresPreferredSizeFromSettings() = runBlocking {
        assertEquals(ClassicBoardSize.FOUR_BY_FOUR, viewModel.uiState.value.boardSize)

        val restored = createViewModel(
            FakeSettingsRepository(ClassicBoardSize.SIX_BY_SIX)
        )
        val restoredState = withTimeoutOrNull(1_000) {
            restored.uiState.first { it.boardSize == ClassicBoardSize.SIX_BY_SIX }
        }

        assertEquals(ClassicBoardSize.SIX_BY_SIX, restoredState?.boardSize)
        assertEquals(GamePhase.INTRO, restoredState?.phase)
    }

    @Test
    fun selectingBoardSizeInIntroUpdatesImmediatelyPersistsAndSurvivesOtherSelectors() = runBlocking {
        viewModel.onEvent(GameEvent.SelectBoardSize(ClassicBoardSize.SIX_BY_SIX))

        assertEquals(ClassicBoardSize.SIX_BY_SIX, viewModel.uiState.value.boardSize)
        assertEquals(ClassicBoardSize.SIX_BY_SIX, settingsRepository.savedSizes.single())

        viewModel.onEvent(GameEvent.SelectMode(GameMode.ENDLESS))
        viewModel.onEvent(GameEvent.SelectDifficulty(Difficulty.ADVANCED))

        assertEquals(ClassicBoardSize.SIX_BY_SIX, viewModel.uiState.value.boardSize)
    }

    @Test
    fun staleSettingsEmissionDoesNotRollbackExplicitIntroSelection() = runBlocking {
        viewModel.onEvent(GameEvent.SelectBoardSize(ClassicBoardSize.SIX_BY_SIX))

        settingsRepository.emitBoardSize(ClassicBoardSize.FOUR_BY_FOUR)

        val rolledBack = withTimeoutOrNull(200) {
            viewModel.uiState.first { it.boardSize == ClassicBoardSize.FOUR_BY_FOUR }
        }
        assertNull(rolledBack)
        assertEquals(ClassicBoardSize.SIX_BY_SIX, viewModel.uiState.value.boardSize)
    }

    @Test
    fun boardSizeSelectionIsIgnoredOutsideIntro() {
        listOf(GamePhase.PLAYING, GamePhase.PAUSED, GamePhase.RESULT).forEach { phase ->
            forceSetState {
                it.copy(
                    phase = phase,
                    boardSize = ClassicBoardSize.FOUR_BY_FOUR,
                    board = BoardState.empty(ClassicBoardSize.FOUR_BY_FOUR),
                )
            }

            viewModel.onEvent(GameEvent.SelectBoardSize(ClassicBoardSize.FIVE_BY_FIVE))

            assertEquals(ClassicBoardSize.FOUR_BY_FOUR, viewModel.uiState.value.boardSize)
        }
        assertTrue(settingsRepository.savedSizes.isEmpty())
    }

    @Test
    fun startAndRestartUseTheSelectedBoardSize() {
        viewModel.onEvent(GameEvent.SelectBoardSize(ClassicBoardSize.FIVE_BY_FIVE))
        viewModel.onEvent(GameEvent.StartGame)

        assertEquals(5, viewModel.uiState.value.board.size)
        assertEquals(ClassicBoardSize.FIVE_BY_FIVE, viewModel.uiState.value.board.boardSize)

        viewModel.onEvent(GameEvent.Restart)

        assertEquals(5, viewModel.uiState.value.board.size)
        assertEquals(ClassicBoardSize.FIVE_BY_FIVE, viewModel.uiState.value.board.boardSize)
    }

    @Test
    fun boardSizePersistenceFailureDoesNotCrashOrRollbackImmediateSelection() {
        settingsRepository.throwOnSave = true

        viewModel.onEvent(GameEvent.SelectBoardSize(ClassicBoardSize.SIX_BY_SIX))

        assertEquals(ClassicBoardSize.SIX_BY_SIX, viewModel.uiState.value.boardSize)
        assertEquals(1, settingsRepository.saveAttempts)
    }

    @Test
    fun recordedResultContainsTheActiveBoardSize() = runBlocking {
        viewModel.onEvent(GameEvent.SelectBoardSize(ClassicBoardSize.SIX_BY_SIX))
        viewModel.onEvent(GameEvent.SelectMode(GameMode.TIME_ATTACK))
        viewModel.onEvent(GameEvent.StartGame)
        forceSetState {
            it.copy(
                phase = GamePhase.PLAYING,
                showTutorial = false,
                timeLeftMillis = 1L,
                lastTimerTickAtMillis = 100L,
            )
        }

        viewModel.onEvent(GameEvent.TimerTick(101L))

        assertEquals(6, gameStatsRepository.records.single().boardSize)
    }

    private class FakeGameStatsRepository : GameStatsRepository {
        val records = mutableListOf<GameResultRecord>()
        var recordAttempts = 0
        var throwOnRecord = false

        override fun observeDiscoveredMolecules(): Flow<List<GameMoleculeDiscovery>> = flowOf(emptyList())

        override fun observeRecentSessions(limit: Int): Flow<List<GameSession>> = flowOf(emptyList())

        override fun observeHighScore(boardSize: ClassicBoardSize): Flow<Int?> = flowOf(null)

        override fun observeHighScoreByDifficulty(difficulty: String, boardSize: ClassicBoardSize): Flow<Int?> = flowOf(null)

        override fun observeHighScoreByMode(mode: String, boardSize: ClassicBoardSize): Flow<Int?> = flowOf(null)

        override fun observeHighScoreByDifficultyAndMode(difficulty: String, mode: String, boardSize: ClassicBoardSize): Flow<Int?> =
            flowOf(null)

        override suspend fun getHighScore(boardSize: ClassicBoardSize): Int? = null

        override suspend fun getHighScoreByDifficulty(difficulty: String, boardSize: ClassicBoardSize): Int? = null

        override suspend fun getHighScoreByMode(mode: String, boardSize: ClassicBoardSize): Int? = null

        override suspend fun getHighScoreByDifficultyAndMode(difficulty: String, mode: String, boardSize: ClassicBoardSize): Int? = null

        override suspend fun recordGameResult(record: GameResultRecord) {
            recordAttempts++
            if (throwOnRecord) error("record failed")
            records += record
        }
    }

    private class FakeSettingsRepository(
        initialBoardSize: ClassicBoardSize = ClassicBoardSize.FOUR_BY_FOUR,
    ) : SettingsRepository {
        private val state = MutableStateFlow(
            AppSettings.DEFAULT.copy(preferredClassicBoardSize = initialBoardSize)
        )
        override val settings: Flow<AppSettings> = state
        val savedSizes = mutableListOf<ClassicBoardSize>()
        var saveAttempts = 0
        var throwOnSave = false

        override suspend fun setThemeMode(mode: com.chemtable.interactive.core.model.ThemeMode) = Unit
        override suspend fun setFontScale(scale: Float) = Unit
        override suspend fun setTableViewMode(mode: com.chemtable.interactive.core.model.TableViewMode) = Unit

        override suspend fun setPreferredClassicBoardSize(size: ClassicBoardSize) {
            saveAttempts++
            if (throwOnSave) error("preference write failed")
            savedSizes += size
            state.value = state.value.copy(preferredClassicBoardSize = size)
        }

        fun emitBoardSize(size: ClassicBoardSize) {
            state.value = state.value.copy(preferredClassicBoardSize = size)
        }
    }
}
