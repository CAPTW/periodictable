package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState

data class ReactorP3TurnResult(
    val p2: ReactorTurnResult,
    val boardBeforeFeed: ReactorBoardState,
    val board: ReactorBoardState,
    val events: List<ReactorTurnEvent>,
    val feedCursor: Int,
    val successfulFeedSerial: Int,
    val pending: ReactorFeedSpecification,
    val preview: List<ReactorFeedSpecification>,
    val pressure: ReactorPressureBreakdown,
    val operational: ReactorOperationalSnapshot,
    val rejected: Boolean,
    val recoveryCount: Int,
)

class ReactorP3Orchestrator(
    private val p2Engine: ReactorBoardEngine,
    elementCatalog: ReactorElementCatalog,
    settlingProfile: ReactorSettlingProfile,
) {
    private val feedResolver = ReactorFeedResolver(elementCatalog, settlingProfile)

    fun resolveTurn(
        board: ReactorBoardState,
        direction: ReactorDirection,
        operationalState: ReactorOperationalState,
        feedCursor: Int,
        successfulFeedSerial: Int,
        failureCount: Int,
        recoveryCount: Int,
    ): ReactorP3TurnResult {
        if (operationalState == ReactorOperationalState.OVERFLOW) {
            val pressure = ReactorPressureEvaluator.evaluate(board, feedBlocked = false)
            val operational = ReactorOperationalResolver.resolve(
                previous = operationalState,
                pressure = 100,
                previousFailureCount = failureCount,
            )
            val schedule = ReactorFeedSchedule.state(feedCursor, successfulFeedSerial)
            return ReactorP3TurnResult(
                p2 = emptyRejectedP2(board),
                boardBeforeFeed = board,
                board = board,
                events = emptyList(),
                feedCursor = feedCursor,
                successfulFeedSerial = successfulFeedSerial,
                pending = schedule.pending,
                preview = schedule.preview,
                pressure = pressure.copy(
                    feedBlocked = 0,
                    pressure = 100,
                    band = ReactorPressureBand.OVERFLOW,
                ),
                operational = operational.copy(
                    state = ReactorOperationalState.OVERFLOW,
                    playerMovesDisabled = true,
                    feedDisabled = true,
                    emergencyVentEnabled = true,
                ),
                rejected = true,
                recoveryCount = recoveryCount,
            )
        }
        val p2 = p2Engine.resolveTurn(board, direction)
        return continueAfterAction(p2, operationalState, feedCursor, successfulFeedSerial, failureCount, recoveryCount)
    }

    /** Shared once-per-turn feed and pressure tail for swipe and validated item actions. */
    internal fun continueAfterAction(
        p2: ReactorTurnResult,
        operationalState: ReactorOperationalState,
        feedCursor: Int,
        successfulFeedSerial: Int,
        failureCount: Int,
        recoveryCount: Int,
    ): ReactorP3TurnResult {
        require(operationalState == ReactorOperationalState.ACTIVE)
        val previousPressure = ReactorPressureEvaluator.evaluate(p2.board, feedBlocked = false)
        val feed = feedResolver.resolve(
            board = p2.board,
            resolvedTurn = p2.resultingTurnIndex,
            cursor = feedCursor,
            successfulFeedSerial = successfulFeedSerial,
        )
        val pressure = ReactorPressureEvaluator.evaluate(feed.board, feedBlocked = feed.blocked)
        val operational = ReactorOperationalResolver.resolve(
            previous = operationalState,
            pressure = pressure.pressure,
            previousFailureCount = failureCount,
        )
        val events = buildList {
            addAll(p2.events)
            addAll(feed.events)
            add(
                ReactorTurnEvent.PressureChanged(
                    oldPressure = previousPressure.pressure,
                    newPressure = pressure.pressure,
                    oldBand = previousPressure.band,
                    newBand = pressure.band,
                    breakdown = pressure,
                ),
            )
            if (operational.transitionedToOverflow) {
                add(
                    ReactorTurnEvent.OverflowTriggered(
                        pressure = pressure.pressure,
                        failureCount = operational.failureCount,
                        oldState = operationalState,
                        newState = operational.state,
                    ),
                )
            }
        }
        return ReactorP3TurnResult(
            p2 = p2,
            boardBeforeFeed = p2.board,
            board = feed.board,
            events = events,
            feedCursor = feed.cursor,
            successfulFeedSerial = feed.successfulFeedSerial,
            pending = feed.pending,
            preview = feed.preview,
            pressure = pressure,
            operational = operational,
            rejected = false,
            recoveryCount = recoveryCount,
        )
    }

    private fun emptyRejectedP2(board: ReactorBoardState): ReactorTurnResult =
        ReactorTurnResult(
            previousTurnIndex = board.turnIndex,
            resultingTurnIndex = board.turnIndex,
            phaseUsed = board.settlingPhase,
            nextPhase = board.settlingPhase,
            board = board,
            events = emptyList(),
            playerCompressionChanged = false,
            mergeOccurred = false,
            settlingChanged = false,
            anyEntityMoved = false,
            formulasCreated = emptyList(),
        )
}
