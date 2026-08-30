package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorFeedSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReactorP3FeedScheduleTest {

    @Test
    fun cycleIsExactTenEntryHOHONNNaClCOSequence() {
        assertEquals(listOf("H", "O", "H", "O", "N", "N", "Na", "Cl", "C", "O"), ReactorFeedSchedule.SYMBOLS)
        assertEquals(listOf(1, 8, 1, 8, 7, 7, 11, 17, 6, 8), ReactorFeedSchedule.ATOMIC_NUMBERS)
        assertEquals(10, ReactorFeedSchedule.SYMBOLS.size)
    }

    @Test
    fun previewShowsNextThreeAndCursorStartsAtZero() {
        val state = ReactorFeedSchedule.state(cursor = 0, successfulFeedSerial = 0)
        assertEquals(0, state.cursor)
        assertEquals(0, state.successfulFeedSerial)
        assertEquals("H", state.pending.symbol)
        assertEquals(0, state.pending.scheduleIndex)
        assertEquals(listOf("H", "O", "H"), state.preview.map { it.symbol })
        assertEquals(3, state.preview.size)
    }

    @Test
    fun cursorAdvancesOnlyOnSuccessfulPlacementAndWrapsPreview() {
        val afterFirst = ReactorFeedSchedule.afterSuccess(cursor = 0, successfulFeedSerial = 0)
        assertEquals(1, afterFirst.cursor)
        assertEquals(1, afterFirst.successfulFeedSerial)
        assertEquals("O", afterFirst.pending.symbol)
        assertEquals(listOf("O", "H", "O"), afterFirst.preview.map { it.symbol })

        val afterWrap = ReactorFeedSchedule.afterSuccess(cursor = 9, successfulFeedSerial = 9)
        assertEquals(0, afterWrap.cursor)
        assertEquals(10, afterWrap.successfulFeedSerial)
        assertEquals("H", afterWrap.pending.symbol)
        assertEquals(listOf("H", "O", "H"), afterWrap.preview.map { it.symbol })
    }

    @Test
    fun blockedPlacementRetainsPendingEntryAndCursor() {
        val blocked = ReactorFeedSchedule.afterBlocked(cursor = 3, successfulFeedSerial = 3)
        val original = ReactorFeedSchedule.state(cursor = 3, successfulFeedSerial = 3)
        assertEquals(original.cursor, blocked.cursor)
        assertEquals(original.successfulFeedSerial, blocked.successfulFeedSerial)
        assertEquals(original.pending, blocked.pending)
        assertEquals(original.preview, blocked.preview)
        assertEquals("O", blocked.pending.symbol)
    }

    @Test
    fun feedIdentitiesAreDeterministicAndUniqueAcrossSuccesses() {
        val first = ReactorFeedSchedule.identity(successfulFeedSerial = 0, scheduleIndex = 0)
        val second = ReactorFeedSchedule.identity(successfulFeedSerial = 1, scheduleIndex = 1)
        assertEquals("p3-feed-0-0", first)
        assertEquals("p3-feed-1-1", second)
        assertNotEquals(first, second)
        assertEquals(first, ReactorFeedSchedule.identity(0, 0))
    }
}
