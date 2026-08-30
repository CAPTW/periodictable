package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.adapter.ClassicRecipeBookReactorAdapter
import com.chemtable.interactive.feature.minigame.reactor.engine.DeterministicReactorEntityIdFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorBoardEngine
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorEventReplayer
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorFeedSchedule
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorFeedSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorP3EventReplayer
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorP3Orchestrator
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureBand
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureBreakdown
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureEvaluator
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorRecoveryResolver
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSampleBoardFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId

data class ReactorFoundationSessionState(
    val board: ReactorBoardState,
    val latestEvents: List<ReactorTurnEvent> = emptyList(),
    val selectedEntityId: ReactorEntityId? = null,
    val lastReplayVerified: Boolean = false,
    val errorMessage: String? = null,
    val feedCursor: Int = 0,
    val successfulFeedSerial: Int = 0,
    val pendingFeed: ReactorFeedSpecification = ReactorFeedSchedule.state(0, 0).pending,
    val feedPreview: List<ReactorFeedSpecification> = ReactorFeedSchedule.state(0, 0).preview,
    val pressure: Int = 0,
    val pressureBand: ReactorPressureBand = ReactorPressureBand.NORMAL,
    val pressureBreakdown: ReactorPressureBreakdown? = null,
    val operationalState: ReactorOperationalState = ReactorOperationalState.ACTIVE,
    val failureCount: Int = 0,
    val recoveryCount: Int = 0,
)

class ReactorFoundationSession(
    elementCatalog: ReactorElementCatalog,
    massAuthority: ReactorMassAuthority,
    settlingProfile: ReactorSettlingProfile,
) {
    private val sampleFactory = ReactorSampleBoardFactory(elementCatalog, settlingProfile)
    private val engine = ReactorBoardEngine(
        reactionCatalog = ClassicRecipeBookReactorAdapter(),
        massAuthority = massAuthority,
        settlingProfile = settlingProfile,
        idFactory = DeterministicReactorEntityIdFactory(),
    )
    private val orchestrator = ReactorP3Orchestrator(
        p2Engine = engine,
        elementCatalog = elementCatalog,
        settlingProfile = settlingProfile,
    )
    private val eventReplayer = ReactorEventReplayer()
    private val p3Replayer = ReactorP3EventReplayer()
    private val recoveryResolver = ReactorRecoveryResolver()

    var state: ReactorFoundationSessionState = initialState()
        private set

    fun swipe(direction: ReactorDirection) {
        if (state.operationalState == ReactorOperationalState.OVERFLOW) {
            state = state.copy(
                lastReplayVerified = false,
                errorMessage = "오버플로 상태에서는 스와이프가 잠깁니다. 긴급 배출을 사용하세요.",
            )
            return
        }
        val initial = state.board
        val result = runCatching {
            orchestrator.resolveTurn(
                board = initial,
                direction = direction,
                operationalState = state.operationalState,
                feedCursor = state.feedCursor,
                successfulFeedSerial = state.successfulFeedSerial,
                failureCount = state.failureCount,
                recoveryCount = state.recoveryCount,
            )
        }.getOrElse { error ->
            state = state.copy(
                lastReplayVerified = false,
                errorMessage = error.message ?: "반응조 턴을 계산하지 못했습니다.",
            )
            return
        }
        val p2Replay = eventReplayer.validate(initial, result.p2)
        val p3Replay = p3Replayer.validate(initial, result)
        if (!p2Replay.isValid || !p3Replay.isValid || p3Replay.replayedBoard != result.board) {
            state = state.copy(
                lastReplayVerified = false,
                errorMessage = buildString {
                    append("반응조 이벤트 재생 검증에 실패했습니다.")
                    val errors = p2Replay.errors + p3Replay.errors
                    if (errors.isNotEmpty()) {
                        append(' ')
                        append(errors.joinToString())
                    }
                },
            )
            return
        }
        state = state.copy(
            board = result.board,
            latestEvents = result.events,
            selectedEntityId = state.selectedEntityId?.takeIf { result.board.entityStore[it] != null },
            lastReplayVerified = true,
            errorMessage = null,
            feedCursor = result.feedCursor,
            successfulFeedSerial = result.successfulFeedSerial,
            pendingFeed = result.pending,
            feedPreview = result.preview,
            pressure = result.pressure.pressure,
            pressureBand = result.pressure.band,
            pressureBreakdown = result.pressure,
            operationalState = result.operational.state,
            failureCount = result.operational.failureCount,
            recoveryCount = result.recoveryCount,
        )
    }

    fun emergencyVent() {
        if (state.operationalState != ReactorOperationalState.OVERFLOW) {
            state = state.copy(errorMessage = "긴급 배출은 오버플로 상태에서만 사용할 수 있습니다.")
            return
        }
        val result = runCatching {
            recoveryResolver.recover(
                board = state.board,
                cursor = state.feedCursor,
                successfulFeedSerial = state.successfulFeedSerial,
                pendingSymbol = state.pendingFeed.symbol,
                previousFailureCount = state.failureCount,
                previousRecoveryCount = state.recoveryCount,
            )
        }.getOrElse { error ->
            state = state.copy(
                lastReplayVerified = false,
                errorMessage = error.message ?: "긴급 배출을 적용하지 못했습니다.",
            )
            return
        }
        state = state.copy(
            board = result.board,
            latestEvents = result.events,
            selectedEntityId = state.selectedEntityId?.takeIf { result.board.entityStore[it] != null },
            lastReplayVerified = true,
            errorMessage = null,
            feedCursor = result.cursor,
            successfulFeedSerial = result.successfulFeedSerial,
            pendingFeed = result.pending,
            feedPreview = ReactorFeedSchedule.state(result.cursor, result.successfulFeedSerial).preview,
            pressure = result.pressure.pressure,
            pressureBand = result.pressure.band,
            pressureBreakdown = result.pressure,
            operationalState = result.operational.state,
            failureCount = result.operational.failureCount,
            recoveryCount = result.recoveryCount,
        )
    }

    fun reset() {
        state = initialState()
    }

    fun selectEntity(entityId: ReactorEntityId?) {
        state = state.copy(
            selectedEntityId = entityId?.takeIf { state.board.entityStore[it] != null },
        )
    }

    private fun initialState(): ReactorFoundationSessionState {
        val board = sampleFactory.create()
        val schedule = ReactorFeedSchedule.state(0, 0)
        val pressure = ReactorPressureEvaluator.evaluate(board, feedBlocked = false)
        return ReactorFoundationSessionState(
            board = board,
            feedCursor = 0,
            successfulFeedSerial = 0,
            pendingFeed = schedule.pending,
            feedPreview = schedule.preview,
            pressure = pressure.pressure,
            pressureBand = pressure.band,
            pressureBreakdown = pressure,
            operationalState = ReactorOperationalState.ACTIVE,
            failureCount = 0,
            recoveryCount = 0,
        )
    }
}
