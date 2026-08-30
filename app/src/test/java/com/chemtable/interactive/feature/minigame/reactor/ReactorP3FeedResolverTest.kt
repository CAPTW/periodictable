package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorFeedResolver
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorP3FeedResolverTest {

    private val resolver = ReactorFeedResolver(
        elementCatalog = ReactorP3Fixtures.catalog,
        settlingProfile = ReactorP3Fixtures.profile,
    )

    @Test
    fun startColumnUsesResolvedTurnPlusCursorModuloFive() {
        assertEquals(1, ReactorFeedResolver.startColumn(resolvedTurn = 1, feedCursor = 0))
        assertEquals(0, ReactorFeedResolver.startColumn(resolvedTurn = 5, feedCursor = 0))
        assertEquals(4, ReactorFeedResolver.startColumn(resolvedTurn = 2, feedCursor = 2))
    }

    @Test
    fun circularScanSelectsFirstEmptyTopCellAndDoesNotOverwrite() {
        val occupied = ReactorP3Fixtures.board(
            ReactorPosition(0, 1) to ReactorP3Fixtures.element("keep-c", "C"),
            ReactorPosition(0, 2) to ReactorP3Fixtures.element("keep-n", "N"),
        )
        val result = resolver.resolve(
            board = occupied,
            resolvedTurn = 1,
            cursor = 0,
            successfulFeedSerial = 0,
        )
        assertTrue(result.placed)
        assertFalse(result.blocked)
        assertEquals(ReactorPosition(0, 3), result.placedPosition)
        assertEquals("keep-c", occupied.entityIdAt(ReactorPosition(0, 1))?.value)
        assertEquals("H", result.board.entityAt(ReactorPosition(0, 3))?.visibleLabel)
        assertEquals("p3-feed-0-0", result.placedEntityId?.value)
        assertEquals(1, result.cursor)
        assertTrue(result.events.filterIsInstance<ReactorTurnEvent.FeedPlaced>().size == 1)
        assertTrue(result.events.filterIsInstance<ReactorTurnEvent.FeedAttempted>().size == 1)
    }

    @Test
    fun fullTopRowProducesFeedBlockedAndRetainsPending() {
        val fullTop = ReactorP3Fixtures.fullTopRowBoard()
        val result = resolver.resolve(
            board = fullTop,
            resolvedTurn = 3,
            cursor = 2,
            successfulFeedSerial = 2,
        )
        assertFalse(result.placed)
        assertTrue(result.blocked)
        assertNull(result.placedPosition)
        assertEquals(fullTop.cells, result.board.cells)
        assertEquals(2, result.cursor)
        assertEquals(2, result.successfulFeedSerial)
        assertEquals("H", result.pending.symbol)
        assertEquals(1, result.events.filterIsInstance<ReactorTurnEvent.FeedBlocked>().size)
    }

    @Test
    fun placedEntityIsAbsentFromSameTurnSettlingAndUsesExactPendingSymbol() {
        val emptyTop = ReactorP3Fixtures.board(
            ReactorPosition(2, 2) to ReactorP3Fixtures.element("mid-o", "O"),
        )
        val result = resolver.resolve(
            board = emptyTop,
            resolvedTurn = 1,
            cursor = 0,
            successfulFeedSerial = 0,
        )
        val placed = result.board.entityAt(result.placedPosition!!)
        assertEquals("H", placed?.visibleLabel)
        assertEquals(result.placedEntityId, placed?.id)
        assertFalse(result.events.any { it is ReactorTurnEvent.SettlingMove || it is ReactorTurnEvent.SettlingSwap })
    }
}
