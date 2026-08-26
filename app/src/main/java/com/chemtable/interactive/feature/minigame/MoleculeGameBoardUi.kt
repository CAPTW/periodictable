package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import java.util.Locale

internal const val MoleculeGameBoardTestTag = "molecule_game_board"

internal data class BoardCellVisualProfile(
    val outerPaddingDp: Float,
    val contentPaddingDp: Float,
    val showSecondaryMass: Boolean,
)

internal fun boardCellVisualProfile(boardSize: ClassicBoardSize): BoardCellVisualProfile =
    when (boardSize) {
        ClassicBoardSize.FOUR_BY_FOUR -> BoardCellVisualProfile(3f, 4f, true)
        ClassicBoardSize.FIVE_BY_FIVE -> BoardCellVisualProfile(2f, 3f, true)
        ClassicBoardSize.SIX_BY_SIX -> BoardCellVisualProfile(1f, 2f, false)
    }

internal fun boardRootContentDescription(boardSize: ClassicBoardSize): String =
    "${boardSize.displayLabel} 분자 게임 보드, ${boardSize.dimension * boardSize.dimension}칸"

internal fun boardCellTestTag(row: Int, col: Int): String = "molecule_game_cell_${row}_$col"

internal fun boardBlockTestTag(row: Int, col: Int): String = "molecule_game_block_${row}_$col"

internal fun boardCellContentDescription(board: BoardState, row: Int, col: Int): String {
    val coordinate = "${row + 1}행 ${col + 1}열"
    return when (val block = board.blockAt(row, col)) {
        null -> "$coordinate, 빈 칸"
        is ElementBlock -> {
            val name = block.nameKo.ifBlank { block.symbol }
            "$coordinate, $name 원소 블록, ${block.symbol}, ${boardMassDescription(block.molarMass)}"
        }
        is MoleculeBlock -> {
            val name = block.displayKo.ifBlank { block.formula }
            "$coordinate, $name 분자 블록, ${block.formula}, ${boardMassDescription(block.massScore)}"
        }
    }
}

private fun boardMassDescription(value: Double): String =
    if (value <= 0.0) {
        "몰 질량 정보 없음"
    } else {
        "${String.format(Locale.ROOT, "%.1f", value)} g/mol"
    }
