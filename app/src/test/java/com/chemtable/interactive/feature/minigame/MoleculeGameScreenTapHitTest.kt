package com.chemtable.interactive.feature.minigame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoleculeGameScreenTapHitTest {

    @Test
    fun boardCellForTap_returnsCellForVisualCellCenter() {
        val position = boardCellForTap(
            tapX = 150f,
            tapY = 250f,
            viewWidth = 400f,
            viewHeight = 400f,
            boardSize = 4,
            cellPaddingPx = 3f,
        )

        assertEquals(2, position?.row)
        assertEquals(1, position?.col)
    }

    @Test
    fun boardCellForTap_returnsNullForCellGapPadding() {
        assertNull(
            boardCellForTap(
                tapX = 101f,
                tapY = 250f,
                viewWidth = 400f,
                viewHeight = 400f,
                boardSize = 4,
                cellPaddingPx = 3f,
            )
        )
        assertNull(
            boardCellForTap(
                tapX = 99f,
                tapY = 250f,
                viewWidth = 400f,
                viewHeight = 400f,
                boardSize = 4,
                cellPaddingPx = 3f,
            )
        )
    }

    @Test
    fun boardCellForTap_returnsNullForOutsideBoard() {
        assertNull(
            boardCellForTap(
                tapX = 400f,
                tapY = 250f,
                viewWidth = 400f,
                viewHeight = 400f,
                boardSize = 4,
                cellPaddingPx = 3f,
            )
        )
    }
}
