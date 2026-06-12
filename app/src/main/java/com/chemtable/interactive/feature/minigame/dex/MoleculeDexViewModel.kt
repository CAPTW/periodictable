package com.chemtable.interactive.feature.minigame.dex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.domain.usecase.GetDiscoveredMoleculesUseCase
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import com.chemtable.interactive.domain.usecase.GetGlossaryUseCase
import com.chemtable.interactive.domain.usecase.GetHighScoreUseCase
import com.chemtable.interactive.domain.usecase.GetRecentGameSessionsUseCase
import com.chemtable.interactive.feature.minigame.MoleculeElementLinkResolver
import com.chemtable.interactive.feature.minigame.MoleculeGlossaryLinkResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MoleculeDexViewModel @Inject constructor(
    getDiscoveredMoleculesUseCase: GetDiscoveredMoleculesUseCase,
    getHighScoreUseCase: GetHighScoreUseCase,
    getRecentGameSessionsUseCase: GetRecentGameSessionsUseCase,
    getElementsUseCase: GetElementsUseCase,
    getGlossaryUseCase: GetGlossaryUseCase,
    moleculeElementLinkResolver: MoleculeElementLinkResolver,
    moleculeGlossaryLinkResolver: MoleculeGlossaryLinkResolver,
) : ViewModel() {

    val uiState: StateFlow<MoleculeDexUiState> = combine(
        getDiscoveredMoleculesUseCase(),
        getHighScoreUseCase(),
        getRecentGameSessionsUseCase(RECENT_SESSION_LIMIT),
        getElementsUseCase(),
        getGlossaryUseCase.allTerms(),
    ) { discoveries, highScore, recentSessions, elements, glossaryTerms ->
        buildMoleculeDexUiState(
            discoveries = discoveries,
            highScore = highScore,
            recentSessions = recentSessions,
            elements = elements,
            glossaryTerms = glossaryTerms,
            elementLinkResolver = moleculeElementLinkResolver,
            glossaryLinkResolver = moleculeGlossaryLinkResolver,
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

    private companion object {
        const val RECENT_SESSION_LIMIT = 5
    }
}
