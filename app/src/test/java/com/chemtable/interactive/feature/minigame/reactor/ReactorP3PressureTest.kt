package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureBand
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureEvaluator
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class ReactorP3PressureTest {

    @Test
    fun formulaMatchesOccupiedPlusWeightedRowsPlusBlockedBonus() {
        val board = ReactorP3Fixtures.board(
            ReactorPosition(0, 0) to ReactorP3Fixtures.element("t0", "H"),
            ReactorPosition(0, 4) to ReactorP3Fixtures.element("t4", "C"),
            ReactorPosition(1, 1) to ReactorP3Fixtures.element("s1", "O"),
            ReactorPosition(2, 2) to ReactorP3Fixtures.element("m2", "N"),
        )
        val result = ReactorPressureEvaluator.evaluate(board, feedBlocked = false)
        assertEquals(4, result.occupiedCount)
        assertEquals(2, result.topRowOccupied)
        assertEquals(1, result.secondRowOccupied)
        assertEquals(0, result.feedBlocked)
        assertEquals(4 + 16 + 3, result.rawPressure)
        assertEquals(23, result.pressure)
        assertEquals(ReactorPressureBand.NORMAL, result.band)
    }

    @Test
    fun bandsAreNormalCautionCriticalAndOverflow() {
        assertEquals(ReactorPressureBand.NORMAL, ReactorPressureEvaluator.band(0))
        assertEquals(ReactorPressureBand.NORMAL, ReactorPressureEvaluator.band(49))
        assertEquals(ReactorPressureBand.CAUTION, ReactorPressureEvaluator.band(50))
        assertEquals(ReactorPressureBand.CAUTION, ReactorPressureEvaluator.band(74))
        assertEquals(ReactorPressureBand.CRITICAL, ReactorPressureEvaluator.band(75))
        assertEquals(ReactorPressureBand.CRITICAL, ReactorPressureEvaluator.band(99))
        assertEquals(ReactorPressureBand.OVERFLOW, ReactorPressureEvaluator.band(100))
    }

    @Test
    fun blockedFullTopRowIsExactlyOneHundredAndClamped() {
        val full = ReactorP3Fixtures.fullTopRowBoard(fillRest = true)
        val blocked = ReactorPressureEvaluator.evaluate(full, feedBlocked = true)
        assertEquals(25, blocked.occupiedCount)
        assertEquals(5, blocked.topRowOccupied)
        assertEquals(5, blocked.secondRowOccupied)
        assertEquals(1, blocked.feedBlocked)
        assertEquals(25 + 40 + 15 + 55, blocked.rawPressure)
        assertEquals(100, blocked.pressure)
        assertEquals(ReactorPressureBand.OVERFLOW, blocked.band)
    }

    @Test
    fun sampleBoardResetPressureIsDeterministicAndUnblocked() {
        val sample = ReactorP3Fixtures.sampleBoard()
        val first = ReactorPressureEvaluator.evaluate(sample, feedBlocked = false)
        val second = ReactorPressureEvaluator.evaluate(sample, feedBlocked = false)
        assertEquals(first, second)
        assertEquals(12, first.occupiedCount)
        assertEquals(3, first.topRowOccupied)
        assertEquals(3, first.secondRowOccupied)
        assertEquals(0, first.feedBlocked)
        assertEquals(45, first.pressure)
        assertEquals(ReactorPressureBand.NORMAL, first.band)
    }
}
