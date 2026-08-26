package com.chemtable.interactive.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicBoardSizeTest {

    @Test
    fun entriesExposeExactlyFourFiveAndSixInDeterministicOrder() {
        assertEquals(listOf(4, 5, 6), ClassicBoardSize.entries.map { it.dimension })
        assertEquals(listOf(4, 5, 6), ClassicBoardSize.entries.map { it.persistenceValue })
    }

    @Test
    fun labelsExposeFullDimensionsForDisplayAndAccessibility() {
        assertEquals("4×4", ClassicBoardSize.FOUR_BY_FOUR.displayLabel)
        assertEquals("5×5 보드", ClassicBoardSize.FIVE_BY_FIVE.accessibilityLabel)
        assertEquals("6×6", ClassicBoardSize.SIX_BY_SIX.displayLabel)
    }

    @Test
    fun persistedValuesRoundTripAndUnknownValuesFallBackToFourByFour() {
        ClassicBoardSize.entries.forEach { size ->
            assertEquals(size, ClassicBoardSize.fromPersistenceValue(size.persistenceValue))
        }
        assertEquals(ClassicBoardSize.FOUR_BY_FOUR, ClassicBoardSize.fromPersistenceValue(null))
        assertEquals(ClassicBoardSize.FOUR_BY_FOUR, ClassicBoardSize.fromPersistenceValue(3))
        assertEquals(ClassicBoardSize.FOUR_BY_FOUR, ClassicBoardSize.fromPersistenceValue(7))
    }
}
