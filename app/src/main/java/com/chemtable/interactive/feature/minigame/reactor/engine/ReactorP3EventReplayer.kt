package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState

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
    ): ReactorP3ReplayValidation {
        val errors = mutableListOf<String>()
        if (result.rejected) {
            if (result.board != initial) errors += "Rejected turn must not mutate the board"
            return finish(errors, result.board, result)
        }
        val p2 = p2Replayer.validate(initial, result.p2)
        if (!p2.isValid || p2.replayedBoard != result.boardBeforeFeed) {
            errors += p2.errors
            errors += "P2 pre-feed replay mismatch"
            return finish(errors, null, result)
        }
        var replayed = result.boardBeforeFeed
        result.events.filterIsInstance<ReactorTurnEvent.FeedPlaced>().forEach { event ->
            if (replayed.entityIdAt(event.position) != null) {
                errors += "Feed overwrite at ${event.position}"
            } else {
                val cells = replayed.cells.toMutableList()
                cells[replayed.indexOf(event.position)] = event.entityId
                replayed = replayed.with(
                    cells = cells,
                    entityStore = replayed.entityStore.removeAndAdd(emptySet(), listOf(event.entity)),
                )
            }
        }
        val blocked = result.events.any { it is ReactorTurnEvent.FeedBlocked }
        val pressure = ReactorPressureEvaluator.evaluate(replayed, feedBlocked = blocked)
        if (replayed != result.board) errors += "P3 feed board mismatch"
        if (pressure.pressure != result.pressure.pressure) errors += "Replay pressure mismatch"
        return finish(errors, replayed, result)
    }

    private fun finish(
        errors: MutableList<String>,
        board: ReactorBoardState?,
        result: ReactorP3TurnResult,
    ): ReactorP3ReplayValidation =
        ReactorP3ReplayValidation(
            isValid = errors.isEmpty(),
            replayedBoard = board.takeIf { errors.isEmpty() },
            feedCursor = result.feedCursor,
            pressure = result.pressure.pressure,
            operationalState = result.operational.state,
            errors = errors.toList(),
        )
}
