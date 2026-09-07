package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.*
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.*
import org.junit.Test

class ReactorP3ReplayIntegrityTest {
    private val fixtures = ReactorP3Fixtures
    private val initial = fixtures.sampleBoard()
    private val engine = ReactorP3Orchestrator(fixtures.p2Engine(), fixtures.catalog, fixtures.profile)
    private val result = engine.resolveTurn(initial, ReactorDirection.LEFT, ReactorOperationalState.ACTIVE, 0, 0, 0, 0)
    private val replayer = ReactorP3EventReplayer()
    private val previous = ReactorP3ReplayContext(0, 0, ReactorOperationalState.ACTIVE, 0, 0)

    private fun valid(candidate: ReactorP3TurnResult): Boolean = replayer.validate(initial, candidate, previous).isValid

    @Test fun validTurnStillReplays() { assertTrue(valid(result)) }

    @Test fun changedCursorIsRejected() { assertFalse(valid(result.copy(feedCursor = 9))) }

    @Test fun changedSerialAndPreviewAreRejected() {
        assertFalse(valid(result.copy(successfulFeedSerial = 99)))
        assertFalse(valid(result.copy(pending = ReactorFeedSchedule.specificationAt(9))))
        assertFalse(valid(result.copy(preview = result.preview.reversed())))
    }

    @Test fun changedOperationalStateAndCountersAreRejected() {
        assertFalse(valid(result.copy(operational = result.operational.copy(state = ReactorOperationalState.OVERFLOW))))
        assertFalse(valid(result.copy(operational = result.operational.copy(failureCount = 7))))
        assertFalse(valid(result.copy(recoveryCount = 7)))
        assertFalse(valid(result.copy(operational = result.operational.copy(playerMovesDisabled = true))))
    }

    @Test fun missingRequiredEventsAreRejected() {
        assertFalse(valid(result.copy(events = result.events.filterNot { it is ReactorTurnEvent.FeedAttempted })))
        assertFalse(valid(result.copy(events = result.events.filterNot { it is ReactorTurnEvent.PressureChanged })))
    }

    @Test fun reorderedDuplicatedOrForeignEventsAreRejected() {
        val events = result.events.toMutableList()
        val index = events.indexOfFirst { it is ReactorTurnEvent.FeedAttempted }
        java.util.Collections.swap(events, index, index + 1)
        assertFalse(valid(result.copy(events = events)))
        assertFalse(valid(result.copy(events = result.events + result.events.last())))
        assertFalse(valid(result.copy(events = result.events + ReactorTurnEvent.RecoveryRequested(0, 0))))
    }

    @Test fun alteredAttemptPressureAndPlacementMetadataAreRejected() {
        assertFalse(valid(result.copy(events = result.events.map {
            if (it is ReactorTurnEvent.FeedAttempted) it.copy(feedCursor = 8) else it
        })))
        assertFalse(valid(result.copy(events = result.events.map {
            if (it is ReactorTurnEvent.PressureChanged) it.copy(newPressure = 1) else it
        })))
        assertFalse(valid(result.copy(events = result.events.map {
            if (it is ReactorTurnEvent.FeedPlaced) it.copy(scannedColumns = emptyList()) else it
        })))
    }

    @Test fun changedPressureBreakdownIsRejected() {
        assertFalse(valid(result.copy(pressure = result.pressure.copy(topRowOccupied = 0))))
    }

    @Test fun invalidPlacementReturnsInvalidRatherThanThrowing() {
        val malformed = result.copy(events = result.events.map {
            if (it is ReactorTurnEvent.FeedPlaced) it.copy(position = ReactorPosition(99, 99)) else it
        })
        assertFalse(valid(malformed))
    }

    @Test fun blockedFeedRequiresMatchingOverflowEvent() {
        val board = fixtures.board(*(0..4).map { c ->
            ReactorPosition(0, c) to fixtures.element("blocked-$c", "C")
        }.toTypedArray())
        val blocked = engine.resolveTurn(board, ReactorDirection.LEFT, ReactorOperationalState.ACTIVE, 0, 0, 0, 0)
        assertEquals(100, blocked.pressure.pressure)
        assertTrue(replayer.validate(board, blocked, previous).isValid)
        assertFalse(replayer.validate(board, blocked.copy(events = blocked.events.filterNot {
            it is ReactorTurnEvent.OverflowTriggered
        }), previous).isValid)
    }

    @Test fun p3EventsCannotBeHiddenInsideTheP2Prefix() {
        val forgedP2 = result.p2.copy(events = result.p2.events + result.events.last())
        val forged = result.copy(p2 = forgedP2,
            events = forgedP2.events + result.events.drop(result.p2.events.size))
        assertFalse(valid(forged))
    }

    @Test fun rejectedTurnMustPreserveTrustedState() {
        val board = fixtures.fullTopRowBoard(fillRest = true)
        val context = ReactorP3ReplayContext(2, 12, ReactorOperationalState.OVERFLOW, 3, 2)
        val rejected = engine.resolveTurn(board, ReactorDirection.LEFT, context.operationalState,
            context.feedCursor, context.successfulFeedSerial, context.failureCount, context.recoveryCount)
        assertTrue(replayer.validate(board, rejected, context).isValid)
        assertFalse(replayer.validate(board, rejected.copy(feedCursor = 3), context).isValid)
        assertFalse(replayer.validate(board, rejected.copy(recoveryCount = 3), context).isValid)
        assertFalse(replayer.validate(board, rejected.copy(rejected = false), context).isValid)
        assertFalse(valid(result.copy(rejected = true)))
    }

    @Test fun scheduleWrapAndNonzeroCountersReplayFromPreviousState() {
        val context = ReactorP3ReplayContext(9, 19, ReactorOperationalState.ACTIVE, 3, 2)
        val wrapped = engine.resolveTurn(initial, ReactorDirection.LEFT, context.operationalState,
            context.feedCursor, context.successfulFeedSerial, context.failureCount, context.recoveryCount)
        assertTrue(replayer.validate(initial, wrapped, context).isValid)
        assertEquals(0, wrapped.feedCursor)
        assertEquals(20, wrapped.successfulFeedSerial)
        assertFalse(replayer.validate(initial, wrapped, context.copy(successfulFeedSerial = 9)).isValid)
    }

    @Test fun recoveryDoesNotClaimAnUnperformedReplayAndNextTurnCanVerify() {
        val session = ReactorFoundationSession(fixtures.catalog, fixtures.massAuthority, fixtures.profile)
        listOf(ReactorDirection.LEFT, ReactorDirection.RIGHT, ReactorDirection.UP, ReactorDirection.DOWN,
            ReactorDirection.LEFT, ReactorDirection.RIGHT, ReactorDirection.UP).forEach(session::swipe)
        assertEquals(ReactorOperationalState.OVERFLOW, session.state.operationalState)
        assertTrue(session.state.lastReplayVerified)
        session.emergencyVent()
        assertEquals(46, session.state.pressure)
        assertFalse("Recovery events were not replayed", session.state.lastReplayVerified)
        session.swipe(ReactorDirection.LEFT)
        assertEquals(8, session.state.board.turnIndex)
        assertEquals(55, session.state.pressure)
        assertTrue(session.state.lastReplayVerified)
    }
}
