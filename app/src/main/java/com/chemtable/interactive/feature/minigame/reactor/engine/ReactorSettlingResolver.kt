package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior

data class ReactorSettlingOutcome(
    val board: ReactorBoardState,
    val events: List<ReactorTurnEvent>,
    val changed: Boolean,
)

/** Resolves one non-overlapping odd/even vertical-pair tick from a single snapshot. */
class ReactorSettlingResolver(
    private val profile: ReactorSettlingProfile,
) {
    fun resolve(board: ReactorBoardState): ReactorSettlingOutcome {
        val snapshot = board.cells
        val resultCells = snapshot.toMutableList()
        val events = mutableListOf<ReactorTurnEvent>()
        val firstRow = board.settlingPhase
        var upperRow = firstRow

        while (upperRow + 1 < board.dimension) {
            val lowerRow = upperRow + 1
            for (column in 0 until board.dimension) {
                val upperPosition = ReactorPosition(upperRow, column)
                val lowerPosition = ReactorPosition(lowerRow, column)
                val upperIndex = board.indexOf(upperPosition)
                val lowerIndex = board.indexOf(lowerPosition)
                val upperId = snapshot[upperIndex]
                val lowerId = snapshot[lowerIndex]
                val upper = upperId?.let(board.entityStore::get)
                val lower = lowerId?.let(board.entityStore::get)

                when {
                    upper != null && lower == null &&
                        upper.settlingBehavior == SettlingBehavior.SINK -> {
                        resultCells[upperIndex] = null
                        resultCells[lowerIndex] = upper.id
                        events += ReactorTurnEvent.SettlingMove(
                            entityId = upper.id,
                            from = upperPosition,
                            to = lowerPosition,
                            behavior = upper.settlingBehavior,
                            settlingIndex = upper.settlingIndex,
                            phase = board.settlingPhase,
                        )
                    }

                    upper == null && lower != null &&
                        lower.settlingBehavior == SettlingBehavior.RISE -> {
                        resultCells[upperIndex] = lower.id
                        resultCells[lowerIndex] = null
                        events += ReactorTurnEvent.SettlingMove(
                            entityId = lower.id,
                            from = lowerPosition,
                            to = upperPosition,
                            behavior = lower.settlingBehavior,
                            settlingIndex = lower.settlingIndex,
                            phase = board.settlingPhase,
                        )
                    }

                    upper != null && lower != null &&
                        upper.settlingIndex - lower.settlingIndex > profile.epsilon -> {
                        resultCells[upperIndex] = lower.id
                        resultCells[lowerIndex] = upper.id
                        events += ReactorTurnEvent.SettlingSwap(
                            upperEntityId = upper.id,
                            lowerEntityId = lower.id,
                            upperFrom = upperPosition,
                            upperTo = lowerPosition,
                            lowerFrom = lowerPosition,
                            lowerTo = upperPosition,
                            upperSettlingIndex = upper.settlingIndex,
                            lowerSettlingIndex = lower.settlingIndex,
                            phase = board.settlingPhase,
                        )
                    }
                }
            }
            upperRow += 2
        }

        val result = board.with(cells = resultCells)
        return ReactorSettlingOutcome(
            board = result,
            events = events.toList(),
            changed = result.cells != board.cells,
        )
    }
}
