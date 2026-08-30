package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalResolver
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorRecoveryResolver
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorP3OverflowRecoveryTest {

    private val recovery = ReactorRecoveryResolver()

    @Test
    fun activeToOverflowIncrementsFailureOnceAndDisablesPlayerAndFeed() {
        val first = ReactorOperationalResolver.resolve(
            previous = ReactorOperationalState.ACTIVE,
            pressure = 100,
            previousFailureCount = 0,
        )
        assertEquals(ReactorOperationalState.OVERFLOW, first.state)
        assertEquals(1, first.failureCount)
        assertTrue(first.transitionedToOverflow)
        assertTrue(first.playerMovesDisabled)
        assertTrue(first.feedDisabled)
        assertTrue(first.emergencyVentEnabled)
        assertTrue(first.resetEnabled)

        val repeated = ReactorOperationalResolver.resolve(
            previous = ReactorOperationalState.OVERFLOW,
            pressure = 100,
            previousFailureCount = 1,
        )
        assertEquals(1, repeated.failureCount)
        assertFalse(repeated.transitionedToOverflow)
    }

    @Test
    fun emergencyVentOrdersBySettlingIndexThenColumnThenStableIdAndRemovesTwo() {
        val highRight = ReactorP3Fixtures.element("id-b", "Cl", settlingIndex = 4.0)
        val highLeft = ReactorP3Fixtures.element("id-a", "Na", settlingIndex = 4.0)
        val low = ReactorP3Fixtures.element("id-c", "H", settlingIndex = -10.0)
        val board = ReactorP3Fixtures.board(
            ReactorPosition(0, 4) to highRight,
            ReactorPosition(0, 1) to highLeft,
            ReactorPosition(0, 2) to low,
        )
        val result = recovery.recover(
            board = board,
            cursor = 4,
            successfulFeedSerial = 4,
            pendingSymbol = "N",
            previousFailureCount = 1,
            previousRecoveryCount = 0,
        )
        assertEquals(listOf("id-a", "id-b"), result.vented.map { it.first.value })
        assertEquals(2, result.vented.size)
        assertEquals(1, result.recoveryCount)
        assertEquals(4, result.cursor)
        assertEquals(4, result.successfulFeedSerial)
        assertEquals("N", result.pending.symbol)
        assertTrue(result.pressure.pressure < 100)
        assertEquals(ReactorOperationalState.ACTIVE, result.operational.state)
        assertEquals(0, result.pressure.feedBlocked)
        assertTrue(result.events.any { it is ReactorTurnEvent.RecoveryRequested })
        assertTrue(result.events.any { it is ReactorTurnEvent.EmergencyVentApplied })
        assertEquals(2, result.events.filterIsInstance<ReactorTurnEvent.EntityVented>().size)
        assertTrue(result.events.any { it is ReactorTurnEvent.RecoveryCompleted })
        assertFalse(result.events.any { it is ReactorTurnEvent.PlayerMove })
        assertFalse(result.events.any { it is ReactorTurnEvent.Merge })
        assertFalse(result.events.any { it is ReactorTurnEvent.FeedAttempted })
    }

    @Test
    fun removingTwoFromFullyOccupiedTopRowDropsPressureBelowOverflow() {
        val full = ReactorP3Fixtures.fullTopRowBoard(fillRest = true)
        val result = recovery.recover(
            board = full,
            cursor = 0,
            successfulFeedSerial = 0,
            pendingSymbol = "H",
            previousFailureCount = 1,
            previousRecoveryCount = 3,
        )
        assertEquals(23, result.board.occupiedPositions().size)
        assertEquals(4, result.recoveryCount)
        assertTrue(result.pressure.pressure < 100)
        assertEquals(ReactorOperationalState.ACTIVE, result.operational.state)
    }
}
