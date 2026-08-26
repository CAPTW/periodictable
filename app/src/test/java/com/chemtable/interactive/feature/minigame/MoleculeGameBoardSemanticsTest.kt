package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameBlock
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoleculeGameBoardSemanticsTest {

    @Test
    fun boardAndCellTagsAreStableAcrossEverySupportedDimension() {
        ClassicBoardSize.entries.forEach { boardSize ->
            assertEquals(
                "${boardSize.displayLabel} 분자 게임 보드, ${boardSize.dimension * boardSize.dimension}칸",
                boardRootContentDescription(boardSize),
            )
            assertEquals("molecule_game_cell_0_0", boardCellTestTag(0, 0))
            assertEquals(
                "molecule_game_cell_${boardSize.dimension - 1}_${boardSize.dimension - 1}",
                boardCellTestTag(boardSize.dimension - 1, boardSize.dimension - 1),
            )
        }
    }

    @Test
    fun compactCellDescriptionsRetainCoordinatesTypeNameFormulaAndMass() {
        val rows = MutableList(6) { MutableList<GameBlock?>(6) { null } }
        rows[0][0] = ElementBlock(1L, 1, "H", "수소", 1.008)
        rows[0][1] = MoleculeBlock(2L, "H2O", 18.015, mapOf("H" to 2, "O" to 1), "물")
        val board = BoardState(ClassicBoardSize.SIX_BY_SIX, rows.map { it.toList() })

        assertEquals("1행 1열, 수소 원소 블록, H, 1.0 g/mol", boardCellContentDescription(board, 0, 0))
        assertEquals("1행 2열, 물 분자 블록, H2O, 18.0 g/mol", boardCellContentDescription(board, 0, 1))
        assertEquals("6행 6열, 빈 칸", boardCellContentDescription(board, 5, 5))
    }

    @Test
    fun visualProfilesKeepFourByFourDetailAndCompactLargerBoardsDeterministically() {
        val four = boardCellVisualProfile(ClassicBoardSize.FOUR_BY_FOUR)
        val five = boardCellVisualProfile(ClassicBoardSize.FIVE_BY_FIVE)
        val six = boardCellVisualProfile(ClassicBoardSize.SIX_BY_SIX)

        assertTrue(four.showSecondaryMass)
        assertTrue(five.showSecondaryMass)
        assertFalse(six.showSecondaryMass)
        assertTrue(four.outerPaddingDp > five.outerPaddingDp)
        assertTrue(five.outerPaddingDp > six.outerPaddingDp)
    }
}
