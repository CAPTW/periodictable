package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorP3EventReplayer
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorP3Orchestrator
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorP3OrchestratorReplayTest {

    private val orchestrator = ReactorP3Orchestrator(
        p2Engine = ReactorP3Fixtures.p2Engine(),
        elementCatalog = ReactorP3Fixtures.catalog,
        settlingProfile = ReactorP3Fixtures.profile,
    )
    private val replayer = ReactorP3EventReplayer()

    @Test
    fun normalTurnPreservesP2ResultThenAppendsFeedAndPressureEvents() {
        val initial = ReactorP3Fixtures.sampleBoard()
        val p2Only = ReactorP3Fixtures.p2Engine().resolveTurn(initial, ReactorDirection.LEFT)
        val result = orchestrator.resolveTurn(
            board = initial,
            direction = ReactorDirection.LEFT,
            operationalState = ReactorOperationalState.ACTIVE,
            feedCursor = 0,
            successfulFeedSerial = 0,
            failureCount = 0,
            recoveryCount = 0,
        )
        assertEquals(p2Only.board.turnIndex, result.p2.board.turnIndex)
        assertEquals(p2Only.board.settlingPhase, result.p2.board.settlingPhase)
        assertEquals(p2Only.events, result.p2.events)
        assertEquals(p2Only.board, result.boardBeforeFeed)
        assertTrue(result.events.take(p2Only.events.size) == p2Only.events)
        val p3Events = result.events.drop(p2Only.events.size)
        assertTrue(p3Events[0] is ReactorTurnEvent.FeedAttempted)
        assertTrue(p3Events[1] is ReactorTurnEvent.FeedPlaced || p3Events[1] is ReactorTurnEvent.FeedBlocked)
        assertTrue(p3Events[2] is ReactorTurnEvent.PressureChanged)
        assertFalse(result.rejected)
    }

    @Test
    fun overflowRejectsPlayerMovesAndDisablesFeed() {
        val overflowBoard = ReactorP3Fixtures.fullTopRowBoard(fillRest = true)
        val result = orchestrator.resolveTurn(
            board = overflowBoard,
            direction = ReactorDirection.LEFT,
            operationalState = ReactorOperationalState.OVERFLOW,
            feedCursor = 2,
            successfulFeedSerial = 2,
            failureCount = 1,
            recoveryCount = 0,
        )
        assertTrue(result.rejected)
        assertEquals(overflowBoard, result.board)
        assertEquals(2, result.feedCursor)
        assertTrue(result.events.none { it is ReactorTurnEvent.FeedAttempted })
    }

    @Test
    fun blockedFullTopRowEmitsFeedBlockedThenPressureThenOverflow() {
        val orchestrator = ReactorP3Orchestrator(
            p2Engine = ReactorP3Fixtures.p2Engine(),
            elementCatalog = ReactorP3Fixtures.catalog,
            settlingProfile = ReactorP3Fixtures.profile,
        )
        val fullTop = ReactorP3Fixtures.board(
            *(0..4).map { column ->
                ReactorPosition(0, column) to ReactorP3Fixtures.element("top-c-$column", "C")
            }.toTypedArray(),
        )
        val result = orchestrator.resolveTurn(
            board = fullTop,
            direction = ReactorDirection.LEFT,
            operationalState = ReactorOperationalState.ACTIVE,
            feedCursor = 0,
            successfulFeedSerial = 0,
            failureCount = 0,
            recoveryCount = 0,
        )
        val p3Events = result.events.drop(result.p2.events.size)
        assertTrue(p3Events[0] is ReactorTurnEvent.FeedAttempted)
        assertTrue(p3Events[1] is ReactorTurnEvent.FeedBlocked)
        assertTrue(p3Events[2] is ReactorTurnEvent.PressureChanged)
        assertTrue(p3Events[3] is ReactorTurnEvent.OverflowTriggered)
        assertEquals(100, result.pressure.pressure)
        assertEquals(ReactorOperationalState.OVERFLOW, result.operational.state)
        assertEquals(0, result.feedCursor)
        assertEquals("H", result.pending.symbol)
    }

    @Test
    fun replayReconstructsFeedPressureAndCounters() {
        val initial = ReactorP3Fixtures.sampleBoard()
        val result = orchestrator.resolveTurn(
            board = initial,
            direction = ReactorDirection.LEFT,
            operationalState = ReactorOperationalState.ACTIVE,
            feedCursor = 0,
            successfulFeedSerial = 0,
            failureCount = 0,
            recoveryCount = 0,
        )
        val replay = replayer.validate(initial, result)
        assertTrue(replay.errors.toString(), replay.isValid)
        assertEquals(result.board, replay.replayedBoard)
        assertEquals(result.feedCursor, replay.feedCursor)
        assertEquals(result.pressure.pressure, replay.pressure)
        assertEquals(result.operational.state, replay.operationalState)
    }

    @Test
    fun sessionSwipeFeedsThenResetClearsP3StateIdempotentlyAndDoesNotUseRoomOrClock() {
        val session = ReactorFoundationSession(
            elementCatalog = ReactorP3Fixtures.catalog,
            massAuthority = ReactorP3Fixtures.massAuthority,
            settlingProfile = ReactorP3Fixtures.profile,
        )
        val initialBoard = session.state.board
        assertEquals(0, session.state.feedCursor)
        assertEquals(listOf("H", "O", "H"), session.state.feedPreview.map { it.symbol })
        assertEquals(ReactorOperationalState.ACTIVE, session.state.operationalState)

        session.swipe(ReactorDirection.LEFT)
        assertTrue(session.state.lastReplayVerified)
        assertTrue(session.state.latestEvents.any { it is ReactorTurnEvent.FeedAttempted })
        assertTrue(session.state.latestEvents.any { it is ReactorTurnEvent.PressureChanged })

        session.reset()
        assertEquals(initialBoard, session.state.board)
        assertEquals(0, session.state.feedCursor)
        assertEquals(0, session.state.successfulFeedSerial)
        assertEquals(0, session.state.failureCount)
        assertEquals(0, session.state.recoveryCount)
        assertEquals(ReactorOperationalState.ACTIVE, session.state.operationalState)
        assertTrue(session.state.latestEvents.isEmpty())
        val afterFirstReset = session.state
        session.reset()
        assertEquals(afterFirstReset, session.state)
        assertFalse(session.javaClass.declaredFields.any { field ->
            field.type.name.contains("Room") || field.type.name.contains("DataStore")
        })
    }

    @Test
    fun overflowIsReachableFromSampleWithinThirtyAcceptedTurns() {
        val session = ReactorFoundationSession(
            elementCatalog = ReactorP3Fixtures.catalog,
            massAuthority = ReactorP3Fixtures.massAuthority,
            settlingProfile = ReactorP3Fixtures.profile,
        )
        var turns = 0
        val directions = listOf(
            ReactorDirection.LEFT,
            ReactorDirection.RIGHT,
            ReactorDirection.UP,
            ReactorDirection.DOWN,
        )
        while (turns < 30 && session.state.operationalState != ReactorOperationalState.OVERFLOW) {
            session.swipe(directions[turns % directions.size])
            turns += 1
        }
        assertEquals(ReactorOperationalState.OVERFLOW, session.state.operationalState)
        assertEquals(100, session.state.pressure)
        assertEquals(1, session.state.failureCount)
        assertTrue(turns <= 30)
    }
}
