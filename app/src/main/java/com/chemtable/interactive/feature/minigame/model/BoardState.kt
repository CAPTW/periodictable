package com.chemtable.interactive.feature.minigame.model

import com.chemtable.interactive.core.model.ClassicBoardSize

/**
 * Classic 보드 상태. grid[row][col], 빈 칸은 null.
 *
 * [boardSize] is strongly typed so unsupported dimensions cannot reach the engine. The validated
 * integer constructor remains internal for legacy call sites while they migrate to the typed API.
 */
data class BoardState(
    val boardSize: ClassicBoardSize,
    val grid: List<List<GameBlock?>>,
) {
    val size: Int get() = boardSize.dimension

    init {
        require(grid.size == size && grid.all { it.size == size }) {
            "Classic board grid must be exactly ${size}×$size"
        }
    }

    internal constructor(size: Int, grid: List<List<GameBlock?>>) : this(
        boardSize = requireNotNull(ClassicBoardSize.fromDimension(size)) {
            "Unsupported Classic board dimension: $size"
        },
        grid = grid,
    )

    fun blockAt(row: Int, col: Int): GameBlock? = grid.getOrNull(row)?.getOrNull(col)

    fun isFull(): Boolean = grid.all { line -> line.all { it != null } }

    fun emptyPositions(): List<Position> {
        val result = ArrayList<Position>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] == null) result.add(Position(r, c))
            }
        }
        return result
    }

    fun blocks(): List<GameBlock> = grid.flatten().filterNotNull()

    companion object {
        fun empty(boardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT): BoardState =
            BoardState(
                boardSize = boardSize,
                grid = List(boardSize.dimension) {
                    List<GameBlock?>(boardSize.dimension) { null }
                },
            )

        internal fun empty(size: Int): BoardState {
            val boardSize = requireNotNull(ClassicBoardSize.fromDimension(size)) {
                "Unsupported Classic board dimension: $size"
            }
            return empty(boardSize)
        }

        fun of(rows: List<List<GameBlock?>>): BoardState {
            val boardSize = requireNotNull(ClassicBoardSize.fromDimension(rows.size)) {
                "Unsupported Classic board dimension: ${rows.size}"
            }
            return BoardState(boardSize, rows)
        }
    }
}
