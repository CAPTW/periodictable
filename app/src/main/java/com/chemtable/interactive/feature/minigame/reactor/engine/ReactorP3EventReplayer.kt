package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorElementEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition

/** Pre-turn authority supplied by the caller, never inferred from the result being checked. */
data class ReactorP3ReplayContext(
    val feedCursor: Int,
    val successfulFeedSerial: Int,
    val operationalState: ReactorOperationalState,
    val failureCount: Int,
    val recoveryCount: Int,
)

data class ReactorP3ReplayValidation(
    val isValid: Boolean,
    val replayedBoard: ReactorBoardState?,
    val feedCursor: Int,
    val pressure: Int,
    val operationalState: ReactorOperationalState,
    val errors: List<String>,
)

class ReactorP3EventReplayer {
    private val p2Replayer = ReactorEventReplayer()

    fun validate(
        initial: ReactorBoardState,
        result: ReactorP3TurnResult,
        previous: ReactorP3ReplayContext,
    ): ReactorP3ReplayValidation = runCatching {
        replay(initial, result, previous)
    }.getOrElse { error ->
        ReactorP3ReplayValidation(
            isValid = false,
            replayedBoard = null,
            feedCursor = -1,
            pressure = -1,
            operationalState = previous.operationalState,
            errors = listOf(error.message ?: "Malformed P3 replay"),
        )
    }

    private fun replay(
        initial: ReactorBoardState,
        result: ReactorP3TurnResult,
        previous: ReactorP3ReplayContext,
    ): ReactorP3ReplayValidation {
        require(previous.feedCursor in ReactorFeedSchedule.SYMBOLS.indices) { "Invalid previous cursor" }
        require(previous.failureCount >= 0 && previous.recoveryCount >= 0) { "Invalid previous counters" }
        val schedule = ReactorFeedSchedule.state(previous.feedCursor, previous.successfulFeedSerial)
        if (result.rejected) {
            require(previous.operationalState == ReactorOperationalState.OVERFLOW) { "Unexpected rejected turn" }
            require(result.board == initial && result.boardBeforeFeed == initial && result.events.isEmpty()) {
                "Rejected turn must not mutate board or events"
            }
            require(result.p2 == ReactorTurnResult(
                initial.turnIndex, initial.turnIndex, initial.settlingPhase, initial.settlingPhase,
                initial, emptyList(), false, false, false, false, emptyList(),
            )) { "Rejected P2 turn must be unchanged" }
            val pressure = ReactorPressureEvaluator.evaluate(initial, feedBlocked = false).copy(
                pressure = 100, band = ReactorPressureBand.OVERFLOW,
            )
            val operational = ReactorOperationalResolver.resolve(previous.operationalState, 100, previous.failureCount)
            return finish(result, previous, initial, schedule, pressure, operational)
        }
        require(previous.operationalState == ReactorOperationalState.ACTIVE) { "Overflow turn must be rejected" }
        require(result.p2.events.all {
            it is ReactorTurnEvent.PlayerMove || it is ReactorTurnEvent.Merge ||
                it is ReactorTurnEvent.SettlingMove || it is ReactorTurnEvent.SettlingSwap
        }) { "P3 events are not allowed inside the P2 prefix" }
        val p2 = p2Replayer.validate(initial, result.p2)
        require(p2.isValid && p2.replayedBoard == result.boardBeforeFeed) {
            "P2 pre-feed replay mismatch: ${p2.errors}"
        }
        require(result.events.take(result.p2.events.size) == result.p2.events) { "P2 event prefix mismatch" }
        val events = result.events.drop(result.p2.events.size)
        val start = ReactorFeedResolver.startColumn(result.boardBeforeFeed.turnIndex, previous.feedCursor)
        val scanned = (0 until initial.dimension).map { (start + it) % initial.dimension }
        val column = scanned.firstOrNull { result.boardBeforeFeed.entityIdAt(ReactorPosition(0, it)) == null }
        val expectedEvents = mutableListOf<ReactorTurnEvent>(
            ReactorTurnEvent.FeedAttempted(
                schedule.pending.scheduleIndex, schedule.pending.symbol, schedule.pending.atomicNumber,
                result.boardBeforeFeed.turnIndex, previous.feedCursor, start,
            ),
        )
        require(events.firstOrNull() == expectedEvents.first()) { "Feed attempt mismatch or missing" }
        var replayed = result.boardBeforeFeed
        val nextSchedule = if (column == null) {
            expectedEvents += ReactorTurnEvent.FeedBlocked(
                schedule.pending.scheduleIndex, schedule.pending.symbol, start, scanned,
            )
            schedule
        } else {
            val placed = events.getOrNull(1) as? ReactorTurnEvent.FeedPlaced
                ?: error("Feed placement missing or out of order")
            val entity = placed.entity as? ReactorElementEntity ?: error("Feed must place an element")
            val id = ReactorEntityId(ReactorFeedSchedule.identity(previous.successfulFeedSerial, schedule.pending.scheduleIndex))
            val position = ReactorPosition(0, column)
            require(entity.id == id && entity.id !in replayed.entityStore.ids &&
                entity.symbol == schedule.pending.symbol && entity.atomicNumber == schedule.pending.atomicNumber
            ) { "Feed entity identity/specification mismatch" }
            expectedEvents += ReactorTurnEvent.FeedPlaced(
                id, entity, entity.visibleLabel, schedule.pending.scheduleIndex, position, start, scanned,
            )
            require(placed == expectedEvents.last()) { "Feed placement metadata mismatch" }
            val cells = replayed.cells.toMutableList()
            cells[replayed.indexOf(position)] = id
            replayed = replayed.with(
                cells = cells,
                entityStore = replayed.entityStore.removeAndAdd(emptySet(), listOf(entity)),
            )
            ReactorFeedSchedule.afterSuccess(previous.feedCursor, previous.successfulFeedSerial)
        }
        val oldPressure = ReactorPressureEvaluator.evaluate(result.boardBeforeFeed, feedBlocked = false)
        val pressure = ReactorPressureEvaluator.evaluate(replayed, feedBlocked = column == null)
        expectedEvents += ReactorTurnEvent.PressureChanged(
            oldPressure.pressure, pressure.pressure, oldPressure.band, pressure.band, pressure,
        )
        val operational = ReactorOperationalResolver.resolve(
            previous.operationalState, pressure.pressure, previous.failureCount,
        )
        if (operational.transitionedToOverflow) {
            expectedEvents += ReactorTurnEvent.OverflowTriggered(
                pressure.pressure, operational.failureCount, previous.operationalState, operational.state,
            )
        }
        require(events == expectedEvents) { "P3 event sequence or metadata mismatch" }
        return finish(result, previous, replayed, nextSchedule, pressure, operational)
    }

    private fun finish(
        result: ReactorP3TurnResult,
        previous: ReactorP3ReplayContext,
        board: ReactorBoardState,
        schedule: ReactorFeedScheduleState,
        pressure: ReactorPressureBreakdown,
        operational: ReactorOperationalSnapshot,
    ): ReactorP3ReplayValidation {
        require(result.board == board) { "P3 feed board mismatch" }
        require(result.feedCursor == schedule.cursor && result.successfulFeedSerial == schedule.successfulFeedSerial &&
            result.pending == schedule.pending && result.preview == schedule.preview
        ) { "Feed cursor/serial/preview mismatch" }
        require(result.pressure == pressure) { "Replay pressure breakdown mismatch" }
        require(result.operational == operational && result.recoveryCount == previous.recoveryCount) {
            "Operational state/counters mismatch"
        }
        return ReactorP3ReplayValidation(
            isValid = true,
            replayedBoard = board,
            feedCursor = schedule.cursor,
            pressure = pressure.pressure,
            operationalState = operational.state,
            errors = emptyList(),
        )
    }
}
