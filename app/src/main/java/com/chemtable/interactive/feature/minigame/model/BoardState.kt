package com.chemtable.interactive.feature.minigame.model

/**
 * 4×4(기본) 보드 상태. grid[row][col], 빈 칸은 null.
 */
data class BoardState(
    val size: Int,
    val grid: List<List<GameBlock?>>,
) {
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
        fun empty(size: Int = 4): BoardState =
            BoardState(size, List(size) { List<GameBlock?>(size) { null } })

        fun of(rows: List<List<GameBlock?>>): BoardState =
            BoardState(rows.size, rows)
    }
}
