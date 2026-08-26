package com.chemtable.interactive.feature.minigame.dex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.domain.usecase.GetDiscoveredMoleculesUseCase
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import com.chemtable.interactive.domain.usecase.GetGlossaryUseCase
import com.chemtable.interactive.domain.usecase.GetHighScoreUseCase
import com.chemtable.interactive.domain.usecase.GetRecentGameSessionsUseCase
import com.chemtable.interactive.domain.repository.SettingsRepository
import com.chemtable.interactive.feature.minigame.MoleculeElementLinkResolver
import com.chemtable.interactive.feature.minigame.MoleculeGlossaryLinkResolver
import com.chemtable.interactive.feature.minigame.model.Difficulty
import com.chemtable.interactive.feature.minigame.model.GameMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoleculeDexViewModel @Inject constructor(
    getDiscoveredMoleculesUseCase: GetDiscoveredMoleculesUseCase,
    getHighScoreUseCase: GetHighScoreUseCase,
    getRecentGameSessionsUseCase: GetRecentGameSessionsUseCase,
    getElementsUseCase: GetElementsUseCase,
    getGlossaryUseCase: GetGlossaryUseCase,
    moleculeElementLinkResolver: MoleculeElementLinkResolver,
    moleculeGlossaryLinkResolver: MoleculeGlossaryLinkResolver,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val selectedBoardSizeOverride = MutableStateFlow<ClassicBoardSize?>(null)

    private val selectedBoardSize: Flow<ClassicBoardSize> = combine(
        settingsRepository.settings.map { it.preferredClassicBoardSize },
        selectedBoardSizeOverride,
    ) { preferred, selected -> selected ?: preferred }
        .distinctUntilChanged()

    private val baseInput: Flow<MoleculeDexInput> = combine(
        getDiscoveredMoleculesUseCase(),
        getRecentGameSessionsUseCase(RECENT_SESSION_LIMIT),
        getElementsUseCase(),
        getGlossaryUseCase.allTerms(),
    ) { discoveries, recentSessions, elements, glossaryTerms ->
        MoleculeDexInput(
            discoveries = discoveries,
            recentSessions = recentSessions,
            elements = elements,
            glossaryTerms = glossaryTerms,
        )
    }

    private val scopedScores: Flow<MoleculeDexScopedScores> = selectedBoardSize.flatMapLatest { boardSize ->
        val difficultyHighScores = combine(
            getHighScoreUseCase(Difficulty.BEGINNER.name, boardSize = boardSize),
            getHighScoreUseCase(Difficulty.INTERMEDIATE.name, boardSize = boardSize),
            getHighScoreUseCase(Difficulty.ADVANCED.name, boardSize = boardSize),
        ) { beginner, intermediate, advanced ->
            mapOf(
                Difficulty.BEGINNER.name to beginner,
                Difficulty.INTERMEDIATE.name to intermediate,
                Difficulty.ADVANCED.name to advanced,
            )
        }
        val modeHighScores = combine(
            getHighScoreUseCase(mode = GameMode.MISSION.name, boardSize = boardSize),
            getHighScoreUseCase(mode = GameMode.ENDLESS.name, boardSize = boardSize),
            getHighScoreUseCase(mode = GameMode.TIME_ATTACK.name, boardSize = boardSize),
        ) { mission, endless, timeAttack ->
            mapOf(
                GameMode.MISSION.name to mission,
                GameMode.ENDLESS.name to endless,
                GameMode.TIME_ATTACK.name to timeAttack,
            )
        }
        combine(
            getHighScoreUseCase(boardSize = boardSize),
            difficultyHighScores,
            modeHighScores,
        ) { highScore, byDifficulty, byMode ->
            MoleculeDexScopedScores(
                boardSize = boardSize,
                highScore = highScore,
                difficultyHighScores = byDifficulty,
                modeHighScores = byMode,
            )
        }
    }

    val uiState: StateFlow<MoleculeDexUiState> = combine(
        baseInput,
        scopedScores,
    ) { input, scores ->
        buildMoleculeDexUiState(
            discoveries = input.discoveries,
            highScore = scores.highScore,
            modeHighScores = scores.modeHighScores,
            difficultyHighScores = scores.difficultyHighScores,
            recentSessions = input.recentSessions,
            elements = input.elements,
            glossaryTerms = input.glossaryTerms,
            elementLinkResolver = moleculeElementLinkResolver,
            glossaryLinkResolver = moleculeGlossaryLinkResolver,
            selectedBoardSize = scores.boardSize,
        )
    }.catch {
        emit(
            MoleculeDexUiState(
                isLoading = false,
                errorMessage = "분자 도감 정보를 불러오지 못했습니다.",
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoleculeDexUiState(),
    )

    fun selectBoardSize(boardSize: ClassicBoardSize) {
        selectedBoardSizeOverride.value = boardSize
    }

    private companion object {
        const val RECENT_SESSION_LIMIT = 5
    }
}

private data class MoleculeDexInput(
    val discoveries: List<GameMoleculeDiscovery>,
    val recentSessions: List<GameSession>,
    val elements: List<Element>,
    val glossaryTerms: List<GlossaryTerm>,
)

private data class MoleculeDexScopedScores(
    val boardSize: ClassicBoardSize,
    val highScore: Int?,
    val difficultyHighScores: Map<String, Int?>,
    val modeHighScores: Map<String, Int?>,
)
