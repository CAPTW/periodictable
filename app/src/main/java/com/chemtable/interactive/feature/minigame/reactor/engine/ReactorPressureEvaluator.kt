package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition

enum class ReactorPressureBand { NORMAL, CAUTION, CRITICAL, OVERFLOW }

data class ReactorPressureBreakdown(
    val occupiedCount: Int,
    val topRowOccupied: Int,
    val secondRowOccupied: Int,
    val feedBlocked: Int,
    val rawPressure: Int,
    val pressure: Int,
    val band: ReactorPressureBand,
)

object ReactorPressureEvaluator {
    fun band(pressure: Int): ReactorPressureBand = when (pressure) {
        in 0..49 -> ReactorPressureBand.NORMAL
        in 50..74 -> ReactorPressureBand.CAUTION
        in 75..99 -> ReactorPressureBand.CRITICAL
        100 -> ReactorPressureBand.OVERFLOW
        else -> error("Reactor pressure must be in 0..100: $pressure")
    }

    fun evaluate(board: ReactorBoardState, feedBlocked: Boolean): ReactorPressureBreakdown {
        val occupiedCount = board.occupiedPositions().size
        val topRowOccupied = (0 until board.dimension).count { column ->
            board.entityIdAt(ReactorPosition(0, column)) != null
        }
        val secondRowOccupied = (0 until board.dimension).count { column ->
            board.entityIdAt(ReactorPosition(1, column)) != null
        }
        val blocked = if (feedBlocked) 1 else 0
        val rawPressure = occupiedCount + (topRowOccupied * 8) + (secondRowOccupied * 3) + (blocked * 55)
        val pressure = rawPressure.coerceIn(0, 100)
        return ReactorPressureBreakdown(
            occupiedCount = occupiedCount,
            topRowOccupied = topRowOccupied,
            secondRowOccupied = secondRowOccupied,
            feedBlocked = blocked,
            rawPressure = rawPressure,
            pressure = pressure,
            band = band(pressure),
        )
    }
}
