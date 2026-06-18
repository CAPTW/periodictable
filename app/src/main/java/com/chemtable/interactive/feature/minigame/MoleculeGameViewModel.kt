package com.chemtable.interactive.feature.minigame

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.core.util.MolarMassCalculator
import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import com.chemtable.interactive.domain.usecase.GetGlossaryUseCase
import com.chemtable.interactive.domain.usecase.RecordGameResultUseCase
import com.chemtable.interactive.feature.minigame.engine.BoardEngine
import com.chemtable.interactive.feature.minigame.engine.FormulaMassResolver
import com.chemtable.interactive.feature.minigame.model.Difficulty
import com.chemtable.interactive.feature.minigame.model.Direction
import com.chemtable.interactive.feature.minigame.model.GameEffect
import com.chemtable.interactive.feature.minigame.model.GameEvent
import com.chemtable.interactive.feature.minigame.model.GamePhase
import com.chemtable.interactive.feature.minigame.model.GameUiState
import com.chemtable.interactive.feature.minigame.model.MissionDifficultyConfigs
import com.chemtable.interactive.feature.minigame.model.MissionTarget
import com.chemtable.interactive.feature.minigame.model.SpawnableElement
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import com.chemtable.interactive.feature.minigame.model.SelectedMoleculeSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * 미니게임 ViewModel. 검증된 순수 BoardEngine 을 구동한다.
 *
 * - 원소 데이터는 GetElementsUseCase 로 받아 스폰 풀을 구성한다.
 * - 분자량은 기존 MolarMassCalculator 를 감싼 FormulaMassResolver 로 계산한다(엔진은 순수 유지).
 * - Phase 3A: 결과 기록은 Room-backed use case 로 넘긴다. 튜토리얼 노출 여부는 세션 메모리 플래그.
 */
@HiltViewModel
class MoleculeGameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getElementsUseCase: GetElementsUseCase,
    getGlossaryUseCase: GetGlossaryUseCase,
    private val recordGameResultUseCase: RecordGameResultUseCase,
    private val molarMassCalculator: MolarMassCalculator,
    private val moleculeElementLinkResolver: MoleculeElementLinkResolver,
    private val moleculeGlossaryLinkResolver: MoleculeGlossaryLinkResolver,
) : ViewModel() {

    private val startElementAtomicNumber: Int? = savedStateHandle.get<Int>("atomicNumber")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(GameUiState.initial())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _effects = Channel<GameEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var elements: List<Element> = emptyList()
    private var glossaryTerms: List<GlossaryTerm> = emptyList()
    private var engine: BoardEngine? = null
    private var hasSeenTutorial = false
    private val madeCount = mutableMapOf<String, Int>()
    private var resultRecordedForSession = false

    init {
        viewModelScope.launch {
            getElementsUseCase().collect { loaded ->
                elements = loaded
                rebuildEngine()
                _uiState.update { state ->
                    val formulas = state.discoveredMolecules.map { molecule -> molecule.formula }
                    state.copy(
                        isEngineReady = engine != null && loaded.isNotEmpty(),
                        discoveredMolecules = discoveredMoleculesFor(formulas),
                    )
                }
            }
        }
        viewModelScope.launch {
            getGlossaryUseCase.allTerms().collect { loaded ->
                glossaryTerms = loaded
                _uiState.update { state ->
                    val formulas = state.discoveredMolecules.map { molecule -> molecule.formula }
                    state.copy(discoveredMolecules = discoveredMoleculesFor(formulas))
                }
            }
        }
    }

    fun onEvent(event: GameEvent) {
        when (event) {
            GameEvent.StartGame -> startGame()
            GameEvent.Restart -> startGame()
            is GameEvent.SelectDifficulty -> selectDifficulty(event.difficulty)
            is GameEvent.Swipe -> handleSwipe(event.direction)
            GameEvent.Pause -> _uiState.update {
                if (it.phase == GamePhase.PLAYING) it.copy(phase = GamePhase.PAUSED) else it
            }
            GameEvent.Resume -> _uiState.update {
                if (it.phase == GamePhase.PAUSED) it.copy(phase = GamePhase.PLAYING) else it
            }
            GameEvent.Exit -> viewModelScope.launch { _effects.send(GameEffect.NavigateBack) }
            is GameEvent.OpenCalculator -> viewModelScope.launch {
                _uiState.update { it.copy(selectedMoleculeSheet = null) }
                _effects.send(GameEffect.NavigateToCalculator(event.formula))
            }
            is GameEvent.OpenElement -> viewModelScope.launch {
                _uiState.update { it.copy(selectedMoleculeSheet = null) }
                if (event.atomicNumber > 0) {
                    _effects.send(GameEffect.NavigateToElement(event.atomicNumber))
                }
            }
            is GameEvent.OpenGlossary -> viewModelScope.launch {
                _uiState.update { it.copy(selectedMoleculeSheet = null) }
                if (event.termId.isNotBlank() && glossaryTerms.any { it.id == event.termId }) {
                    _effects.send(GameEffect.NavigateToGlossary(event.termId))
                }
            }
            GameEvent.SkipTutorial -> _uiState.update { it.copy(showTutorial = false) }
            GameEvent.ShowTutorial -> _uiState.update { it.copy(showTutorial = true) }
            is GameEvent.BlockTapped -> handleBlockTapped(event.blockId)
            GameEvent.CloseMoleculeSheet -> _uiState.update { it.copy(selectedMoleculeSheet = null) }
        }
    }

    private fun rebuildEngine() {
        engine = createEngineForDifficulty(_uiState.value.difficulty)
    }

    private fun createEngineForDifficulty(difficulty: Difficulty): BoardEngine {
        val config = MissionDifficultyConfigs.forDifficulty(difficulty)
        val resolver = FormulaMassResolver { formula ->
            runCatching { molarMassCalculator.calculate(formula, elements).totalMolarMass }
                .getOrDefault(0.0)
        }
        return BoardEngine(
            resolver = resolver,
            spawnPool = buildSpawnPool(elements, config.spawnSymbols),
            random = Random(System.nanoTime()),
        )
    }

    private fun selectDifficulty(difficulty: Difficulty) {
        var changed = false
        _uiState.update { state ->
            if (state.phase == GamePhase.INTRO && state.difficulty != difficulty) {
                changed = true
                state.copy(difficulty = difficulty)
            } else {
                state
            }
        }
        if (changed) {
            rebuildEngine()
        }
    }

    private fun startGame() {
        if (elements.isEmpty()) return
        val difficulty = _uiState.value.difficulty
        val config = MissionDifficultyConfigs.forDifficulty(difficulty)
        val activeEngine = createEngineForDifficulty(difficulty)
        engine = activeEngine
        madeCount.clear()
        resultRecordedForSession = false
        val startSpec = startElementAtomicNumber?.let { targetId ->
            elements.find { it.atomicNumber == targetId }?.let { elem ->
                SpawnableElement(
                    atomicNumber = elem.atomicNumber,
                    symbol = elem.symbol,
                    nameKo = elem.nameKo,
                    molarMass = elem.molarMass,
                    category = elem.category,
                )
            }
        }
        val mission = config.missionCandidates.first()
        val board = activeEngine.seedBoard(
            count = config.initialBlockCount,
            startElementSpec = startSpec,
            size = config.boardSize,
        )
        val firstTime = !hasSeenTutorial
        hasSeenTutorial = true
        _uiState.update {
            it.copy(
                phase = GamePhase.PLAYING,
                board = board,
                score = 0,
                combo = 0,
                missionTarget = MissionTarget(formula = mission.formula, count = mission.count, progress = 0),
                movesLeft = config.movesLeft,
                discoveredMolecules = emptyList(),
                resultSuccess = false,
                showTutorial = firstTime,
                selectedMoleculeSheet = null,
            )
        }
    }

    private fun handleSwipe(direction: Direction) {
        val state = _uiState.value
        if (state.phase != GamePhase.PLAYING) return
        val activeEngine = engine ?: return

        val result = activeEngine.move(state.board, direction)
        if (!result.moved) {
            viewModelScope.launch { _effects.send(GameEffect.MergeRejected) }
            return
        }

        result.mergedFormulas.forEach { formula ->
            madeCount[formula] = (madeCount[formula] ?: 0) + 1
        }
        val discoveredFormulas = (state.discoveredMolecules.map { it.formula } + result.mergedFormulas).distinct()
        val discovered = discoveredMoleculesFor(discoveredFormulas)
        val target = state.missionTarget?.copy(progress = madeCount[state.missionTarget.formula] ?: 0)
        val success = target?.isComplete == true
        val nextCombo = if (result.mergedFormulas.isNotEmpty()) state.combo + 1 else 0
        val moveScore = calculateMoveScore(
            mergedScore = result.gainedScore,
            combo = nextCombo,
            difficulty = state.difficulty,
        )
        val movesLeft = state.movesLeft?.let { (it - 1).coerceAtLeast(0) }
        val exhaustedMoves = movesLeft == 0
        val missionSuccessScore = if (success && !state.resultSuccess) calculateMissionSuccessBonus(state.difficulty) else 0
        val finalMoveScore = moveScore + missionSuccessScore
        val newPhase = when {
            success -> GamePhase.RESULT
            result.isGameOver -> GamePhase.RESULT
            exhaustedMoves -> GamePhase.RESULT
            else -> GamePhase.PLAYING
        }

        _uiState.update {
            it.copy(
                board = result.board,
                score = it.score + finalMoveScore,
                combo = nextCombo,
                discoveredMolecules = discovered,
                missionTarget = target,
                movesLeft = movesLeft,
                phase = newPhase,
                resultSuccess = if (newPhase == GamePhase.RESULT) success else it.resultSuccess,
            )
        }

        if (result.mergedFormulas.isNotEmpty()) {
            val label = result.mergedFormulas.joinToString(" + ")
            viewModelScope.launch {
                _effects.send(GameEffect.MergeSuccess(label, moveScore))
            }
            if (missionSuccessScore > 0) {
                viewModelScope.launch {
                    _effects.send(GameEffect.MergeSuccess("미션 보너스", missionSuccessScore))
                }
            }
        }
        if (newPhase == GamePhase.RESULT) {
            recordResultIfNeeded(_uiState.value)
        }
    }

    private fun recordResultIfNeeded(state: GameUiState) {
        if (state.phase != GamePhase.RESULT || resultRecordedForSession) return
        resultRecordedForSession = true

        val record = GameResultRecord(
            score = state.score,
            success = state.resultSuccess,
            difficulty = state.difficulty.name,
            missionFormula = state.missionTarget?.formula,
            missionTargetCount = state.missionTarget?.count,
            playedAt = System.currentTimeMillis(),
            moleculesMade = madeMoleculesForRecord(),
        )

        viewModelScope.launch {
            runCatching { recordGameResultUseCase(record) }
        }
    }

    private fun madeMoleculesForRecord(): List<String> =
        madeCount.entries.flatMap { (formula, count) -> List(count) { formula } }

    private fun discoveredMoleculesFor(formulas: List<String>) =
        moleculeElementLinkResolver.discoveredMoleculesFor(formulas, elements).map { molecule ->
            molecule.copy(
                glossaryLinks = moleculeGlossaryLinkResolver.linksForFormula(molecule.formula, glossaryTerms),
            )
        }

    private fun buildSpawnPool(source: List<Element>, spawnSymbols: List<String>): List<SpawnableElement> {
        val bySymbol = source.associateBy { it.symbol }
        return spawnSymbols.mapNotNull { symbol ->
            bySymbol[symbol]?.let {
                SpawnableElement(
                    atomicNumber = it.atomicNumber,
                    symbol = it.symbol,
                    nameKo = it.nameKo,
                    molarMass = it.molarMass,
                    category = it.category,
                )
            }
        }
    }

    private fun handleBlockTapped(blockId: Long) {
        val state = _uiState.value
        if (state.phase != GamePhase.PLAYING) return

        val board = state.board
        val block = board.blocks().find { it.id == blockId }
        if (block is MoleculeBlock) {
            _uiState.update {
                it.copy(
                    selectedMoleculeSheet = SelectedMoleculeSheet(
                        blockId = block.id,
                        formula = block.formula,
                        molarMass = block.massScore,
                        elementLinks = moleculeElementLinkResolver.linksForFormula(block.formula, elements),
                        glossaryLinks = moleculeGlossaryLinkResolver.linksForFormula(block.formula, glossaryTerms),
                    ),
                )
            }
        }
    }
}
