package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.feature.minigame.engine.BoardEngine
import com.chemtable.interactive.feature.minigame.engine.FormulaMassResolver
import com.chemtable.interactive.feature.minigame.engine.IdGenerator
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.Direction
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameBlock
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import com.chemtable.interactive.feature.minigame.model.SpawnableElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VariableBoardEngineTest {

    private val pool = listOf(SpawnableElement(1, "H", "수소", 1.008))
    private val resolver = FormulaMassResolver { formula -> if (formula == "H2") 2.016 else 0.0 }

    private fun engine(seed: Long = 7L) = BoardEngine(
        resolver = resolver,
        spawnPool = pool,
        random = Random(seed),
        idGenerator = IdGenerator(),
    )

    private fun element(id: Long, symbol: String = "H", mass: Double = 1.008) =
        ElementBlock(id = id, atomicNumber = 1, symbol = symbol, nameKo = symbol, molarMass = mass)

    @Test
    fun emptyBoardsExposeExactCellAndCoordinateCountsForEverySupportedSize() {
        ClassicBoardSize.entries.forEach { boardSize ->
            val board = BoardState.empty(boardSize)

            assertEquals(boardSize, board.boardSize)
            assertEquals(boardSize.dimension, board.size)
            assertEquals(boardSize.dimension, board.grid.size)
            assertTrue(board.grid.all { it.size == boardSize.dimension })
            assertEquals(boardSize.dimension * boardSize.dimension, board.grid.flatten().size)
            assertEquals(boardSize.dimension * boardSize.dimension, board.emptyPositions().size)
            assertNull(board.blockAt(-1, 0))
            assertNull(board.blockAt(0, boardSize.dimension))
        }
    }

    @Test
    fun seedingAndMovementStayWithinEverySupportedBoard() {
        ClassicBoardSize.entries.forEach { boardSize ->
            val board = engine().seedBoard(count = 4, boardSize = boardSize)

            assertEquals(boardSize, board.boardSize)
            assertEquals(4, board.blocks().size)
            assertEquals(4, board.blocks().map { it.id }.distinct().size)

            val moved = engine().move(board, Direction.LEFT).board
            assertEquals(boardSize, moved.boardSize)
            assertTrue(moved.emptyPositions().all { it.row in 0 until boardSize.dimension })
            assertTrue(moved.emptyPositions().all { it.col in 0 until boardSize.dimension })
            assertEquals(moved.blocks().size, moved.blocks().map { it.id }.distinct().size)
        }
    }

    @Test
    fun mergeGravitySpawnAndInvalidMovePreserveClassicRulesForEverySize() {
        ClassicBoardSize.entries.forEach { boardSize ->
            val size = boardSize.dimension
            val rows = MutableList(size) { MutableList<GameBlock?>(size) { null } }
            rows[0][0] = element(1)
            rows[0][1] = element(2)
            rows[0][2] = element(3, symbol = "Na", mass = 22.99)
            val board = BoardState(boardSize, rows.map { it.toList() })

            val slide = engine().compressAndMerge(board, Direction.LEFT)
            assertEquals(listOf("H2"), slide.mergedBlocks.map { it.formula })
            assertTrue(slide.board.blockAt(0, 0) is MoleculeBlock)

            val gravity = engine().applyGravity(slide.board)
            assertEquals("Na", (gravity.blockAt(size - 1, 1) as ElementBlock).symbol)
            assertEquals("H2", (gravity.blockAt(size - 1, 0) as MoleculeBlock).formula)

            val result = engine().move(board, Direction.LEFT)
            assertTrue(result.moved)
            assertTrue(result.spawned)
            assertEquals(3, result.board.blocks().size)

            val alreadyAtWall = BoardState(
                boardSize,
                List(size) { row ->
                    List<GameBlock?>(size) { col ->
                        if (row == 0 && col == 0) element(100) else null
                    }
                },
            )
            val invalid = engine().move(alreadyAtWall, Direction.LEFT)
            assertFalse(invalid.moved)
            assertFalse(invalid.spawned)
            assertEquals(1, invalid.board.blocks().size)
        }
    }

    @Test
    fun fullUnmergeableBoardsAreGameOverForEverySupportedSize() {
        ClassicBoardSize.entries.forEach { boardSize ->
            val dimension = boardSize.dimension
            val rows = List(dimension) { row ->
                List<GameBlock?>(dimension) { col ->
                    if ((row + col) % 2 == 0) {
                        element((row * dimension + col + 1).toLong(), symbol = "H")
                    } else {
                        element((row * dimension + col + 1).toLong(), symbol = "Na", mass = 22.99)
                    }
                }
            }

            assertTrue(engine().isGameOver(BoardState(boardSize, rows)))
        }
    }

    @Test
    fun newlyMergedMoleculeNeverRemergesInTheSamePassForEverySupportedSize() {
        ClassicBoardSize.entries.forEach { boardSize ->
            val dimension = boardSize.dimension
            val rows = MutableList(dimension) { MutableList<GameBlock?>(dimension) { null } }
            rows[0][0] = element(1)
            rows[0][1] = element(2)
            rows[0][2] = element(3)

            val result = engine().compressAndMerge(
                BoardState(boardSize, rows.map { it.toList() }),
                Direction.LEFT,
            )

            assertEquals(1, result.mergedBlocks.size)
            assertTrue(result.board.blockAt(0, 0) is MoleculeBlock)
            assertTrue(result.board.blockAt(0, 1) is ElementBlock)
        }
    }
}
