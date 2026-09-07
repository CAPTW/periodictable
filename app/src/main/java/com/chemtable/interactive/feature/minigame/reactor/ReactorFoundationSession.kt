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
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorP3ReplayContext
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
    val itemActionsRemaining: Int = 6,
    val itemRechargeProgress: Int = 0,
    val itemLearningMessage: String? = null,
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

    private val itemResolver = com.chemtable.interactive.feature.minigame.reactor.engine.ReactorItemTurnResolver(orchestrator, settlingProfile)

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
        val p3Replay = p3Replayer.validate(
            initial, result,
            ReactorP3ReplayContext(
                state.feedCursor, state.successfulFeedSerial, state.operationalState,
                state.failureCount, state.recoveryCount,
            ),
        )
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
            itemRechargeProgress = if (!result.rejected && state.itemActionsRemaining < 6) (state.itemRechargeProgress + 1).coerceAtMost(3) else state.itemRechargeProgress,
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
            // Recovery events have not been replayed.
            lastReplayVerified = false,
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

    /** Session-only reward; no board turn, event receipt or persistent inventory is changed. */
    fun claimItemRecharge() {
        if (state.itemRechargeProgress < 3 || state.itemActionsRemaining >= 6) return
        state = state.copy(itemActionsRemaining = state.itemActionsRemaining + 1, itemRechargeProgress = 0)
    }

    fun loadItemSample() {
        state = initialState(com.chemtable.interactive.feature.minigame.reactor.engine.ReactorItemTurnResolver.sampleBoard())
    }

    fun useItem(command: com.chemtable.interactive.feature.minigame.reactor.engine.ReactorItemCommand) {
        val initial = state
        val before = ReactorP3ReplayContext(initial.feedCursor,initial.successfulFeedSerial,
            initial.operationalState,initial.failureCount,initial.recoveryCount)
        val result = runCatching {
            itemResolver.resolve(initial.board,command,before,initial.itemActionsRemaining).also {
                check(itemResolver.validate(initial.board,command,before,initial.itemActionsRemaining,it)) {
                    "아이템 이벤트 재생 검증에 실패했습니다."
                }
            }
        }.getOrElse { error ->
            state = initial.copy(lastReplayVerified = false, errorMessage = error.message ?: "아이템을 적용하지 못했습니다.")
            return
        }
        val tail = result.continuation
        state = initial.copy(
            board = tail.board, latestEvents = result.events, itemActionsRemaining = result.remainingActions,
            itemLearningMessage = when (command) {
                is com.chemtable.interactive.feature.minigame.reactor.engine.ReactorItemCommand.Cleave -> "맞는 효소로 조각 1개를 분리했습니다. 남은 묶음과 조각의 총량은 같습니다. 빈 공간을 살펴 재연결해 보세요."
                is com.chemtable.interactive.feature.minigame.reactor.engine.ReactorItemCommand.Link -> "같은 기질의 조각을 한 묶음으로 연결했습니다. 총량은 그대로이며, 공급이 빈 칸을 채울 수 있습니다."
            },
            selectedEntityId = initial.selectedEntityId?.takeIf { tail.board.entityStore[it] != null },
            lastReplayVerified = true, errorMessage = null,
            feedCursor = tail.feedCursor, successfulFeedSerial = tail.successfulFeedSerial,
            pendingFeed = tail.pending, feedPreview = tail.preview,
            pressure = tail.pressure.pressure, pressureBand = tail.pressure.band,
            pressureBreakdown = tail.pressure, operationalState = tail.operational.state,
            failureCount = tail.operational.failureCount, recoveryCount = tail.recoveryCount,
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

    private fun initialState(board: ReactorBoardState = sampleFactory.create()): ReactorFoundationSessionState {
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
