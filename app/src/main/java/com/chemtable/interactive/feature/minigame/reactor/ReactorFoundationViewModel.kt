package com.chemtable.interactive.feature.minigame.reactor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.util.MolarMassCalculator
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import com.chemtable.interactive.feature.minigame.engine.FormulaMassResolver
import com.chemtable.interactive.feature.minigame.reactor.adapter.FormulaMassResolverReactorMassAdapter
import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorFeedSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureBand
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureBreakdown
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReactorFoundationUiState(
    val board: ReactorBoardState? = null,
    val latestEvents: List<ReactorTurnEvent> = emptyList(),
    val selectedEntityId: ReactorEntityId? = null,
    val lastReplayVerified: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
    val feedPreview: List<ReactorFeedSpecification> = emptyList(),
    val pendingFeed: ReactorFeedSpecification? = null,
    val pressure: Int = 0,
    val pressureBand: ReactorPressureBand = ReactorPressureBand.NORMAL,
    val pressureBreakdown: ReactorPressureBreakdown? = null,
    val operationalState: ReactorOperationalState = ReactorOperationalState.ACTIVE,
    val failureCount: Int = 0,
    val recoveryCount: Int = 0,
)

@HiltViewModel
class ReactorFoundationViewModel @Inject constructor(
    getElementsUseCase: GetElementsUseCase,
    private val molarMassCalculator: MolarMassCalculator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReactorFoundationUiState())
    val uiState: StateFlow<ReactorFoundationUiState> = _uiState.asStateFlow()

    private var session: ReactorFoundationSession? = null

    init {
        viewModelScope.launch {
            runCatching {
                val elements = getElementsUseCase().first { it.isNotEmpty() }
                val bySymbol = elements.associateBy { it.symbol }
                val elementCatalog = ReactorElementCatalog { symbol ->
                    bySymbol[symbol]?.let { element ->
                        ReactorElementSpecification(
                            atomicNumber = element.atomicNumber,
                            symbol = element.symbol,
                            displayName = element.nameKo.ifBlank { element.name },
                            molarMass = element.molarMass,
                        )
                    }
                }
                val massResolver = FormulaMassResolver { formula ->
                    molarMassCalculator.calculate(formula, elements).totalMolarMass
                }
                ReactorFoundationSession(
                    elementCatalog = elementCatalog,
                    massAuthority = FormulaMassResolverReactorMassAdapter(massResolver),
                    settlingProfile = MassReferenceSettlingProfile(),
                )
            }.onSuccess { created ->
                session = created
                publish(created.state)
            }.onFailure { error ->
                _uiState.value = ReactorFoundationUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "반응조 샘플을 준비하지 못했습니다.",
                )
            }
        }
    }

    fun onSwipe(direction: ReactorDirection) {
        val active = session ?: return
        active.swipe(direction)
        publish(active.state)
    }

    fun resetSample() {
        val active = session ?: return
        active.reset()
        publish(active.state)
    }

    fun emergencyVent() {
        val active = session ?: return
        active.emergencyVent()
        publish(active.state)
    }

    fun selectEntity(entityId: ReactorEntityId?) {
        val active = session ?: return
        active.selectEntity(entityId)
        publish(active.state)
    }

    private fun publish(state: ReactorFoundationSessionState) {
        _uiState.value = ReactorFoundationUiState(
            board = state.board,
            latestEvents = state.latestEvents,
            selectedEntityId = state.selectedEntityId,
            lastReplayVerified = state.lastReplayVerified,
            errorMessage = state.errorMessage,
            isLoading = false,
            feedPreview = state.feedPreview,
            pendingFeed = state.pendingFeed,
            pressure = state.pressure,
            pressureBand = state.pressureBand,
            pressureBreakdown = state.pressureBreakdown,
            operationalState = state.operationalState,
            failureCount = state.failureCount,
            recoveryCount = state.recoveryCount,
        )
    }
}
