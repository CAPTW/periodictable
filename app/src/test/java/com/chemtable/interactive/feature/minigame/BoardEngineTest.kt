package com.chemtable.interactive.feature.minigame

import com.chemtable.interactive.feature.minigame.engine.BoardEngine
import com.chemtable.interactive.feature.minigame.engine.FormulaMassResolver
import com.chemtable.interactive.feature.minigame.engine.IdGenerator
import com.chemtable.interactive.feature.minigame.engine.RecipeBook
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.Direction
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameBlock
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import com.chemtable.interactive.feature.minigame.model.SpawnableElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.chemtable.interactive.core.model.ElementCategory
import org.junit.Test
import kotlin.random.Random

class BoardEngineTest {

    private val masses = mapOf(
        "H2" to 2.016,
        "O2" to 31.998,
        "N2" to 28.014,
        "H2O" to 18.015,
        "CO2" to 44.009,
        "NaCl" to 58.44,
    )
    private val resolver = FormulaMassResolver { masses[it] ?: 0.0 }

    private fun engine(
        pool: List<SpawnableElement> = emptyList(),
        seed: Long = 42L,
    ) = BoardEngine(
        resolver = resolver,
        recipeBook = RecipeBook(),
        spawnPool = pool,
        random = Random(seed),
        idGenerator = IdGenerator(),
    )

    private fun el(symbol: String, id: Long = 1L, mass: Double = 0.0) =
        ElementBlock(id = id, atomicNumber = 0, symbol = symbol, nameKo = "", molarMass = mass)

    private fun emptyRow(): List<GameBlock?> = listOf(null, null, null, null)
    private fun row(vararg cells: GameBlock?): List<GameBlock?> = cells.toList()
    private fun board(vararg rows: List<GameBlock?>) = BoardState(rows.size, rows.toList())

    @Test
    fun swipe_compacts_blocks_to_wall() {
        val b = board(
            row(null, el("H", 1), null, el("Na", 2)),
            emptyRow(), emptyRow(), emptyRow(),
        )
        val out = engine().compressAndMerge(b, Direction.LEFT)

        assertTrue(out.moved)
        assertEquals("H", (out.board.blockAt(0, 0) as ElementBlock).symbol)
        assertEquals("Na", (out.board.blockAt(0, 1) as ElementBlock).symbol)
        assertNull(out.board.blockAt(0, 2))
        assertNull(out.board.blockAt(0, 3))
    }

    @Test
    fun gravity_sorts_heavy_to_bottom() {
        val heavy = el("Na", 1, mass = 22.99)
        val light = el("H", 2, mass = 1.008)
        val b = board(
            row(heavy, null, null, null),
            emptyRow(),
            emptyRow(),
            row(light, null, null, null),
        )
        val out = engine().applyGravity(b)

        assertEquals("Na", (out.blockAt(3, 0) as ElementBlock).symbol) // heaviest at bottom
        assertEquals("H", (out.blockAt(2, 0) as ElementBlock).symbol)  // lighter above
        assertNull(out.blockAt(0, 0))
        assertNull(out.blockAt(1, 0))
    }

    @Test
    fun invalid_pair_is_preserved() {
        val b = board(
            row(el("H", 1), el("Na", 2), null, null),
            emptyRow(), emptyRow(), emptyRow(),
        )
        val out = engine().compressAndMerge(b, Direction.LEFT)

        assertTrue(out.mergedBlocks.isEmpty())
        assertEquals(2, out.board.blocks().size)
        assertTrue(out.board.blocks().all { it is ElementBlock })
    }

    @Test
    fun same_block_merges_once_per_move() {
        val b = board(
            row(el("H", 1), el("H", 2), el("H", 3), null),
            emptyRow(), emptyRow(), emptyRow(),
        )
        val out = engine().compressAndMerge(b, Direction.LEFT)

        // [H, H, H] -> [H2, H]
        val first = out.board.blockAt(0, 0)
        val second = out.board.blockAt(0, 1)
        assertTrue(first is MoleculeBlock && first.formula == "H2")
        assertTrue(second is ElementBlock && second.symbol == "H")
        assertNull(out.board.blockAt(0, 2))
        assertEquals(1, out.mergedBlocks.size)
    }

    @Test
    fun valid_move_marks_spawn() {
        val pool = listOf(SpawnableElement(1, "H", "수소", 1.008))
        val b = board(
            row(null, el("H", 1), null, null),
            emptyRow(), emptyRow(), emptyRow(),
        )
        val result = engine(pool).move(b, Direction.LEFT)

        assertTrue(result.moved)
        assertTrue(result.spawned)
        assertNotNull(result.spawnedPosition)
    }

    @Test
    fun no_movement_means_no_spawn() {
        val pool = listOf(SpawnableElement(1, "H", "수소", 1.008))
        val b = board(
            row(el("H", 1), el("Na", 2), null, null),
            emptyRow(), emptyRow(), emptyRow(),
        )
        val result = engine(pool).move(b, Direction.LEFT)

        assertFalse(result.moved)
        assertFalse(result.spawned)
        assertNull(result.spawnedPosition)
    }

    @Test
    fun full_unmergeable_board_is_game_over() {
        // 체커보드 H / Na: 가득 차 있고 인접 동일 원소가 없어 어떤 방향으로도 이동/조합 불가.
        val rows = (0 until 4).map { r ->
            (0 until 4).map<Int, GameBlock?> { c ->
                val id = (r * 4 + c).toLong() + 1
                if ((r + c) % 2 == 0) el("H", id, 1.008) else el("Na", id, 22.99)
            }
        }
        val b = BoardState(4, rows)

        assertTrue(b.isFull())
        assertTrue(engine().isGameOver(b))
    }

    @Test
    fun board_with_empty_cell_is_not_game_over() {
        val b = board(
            row(el("H", 1, 1.008), null, null, null),
            emptyRow(), emptyRow(), emptyRow(),
        )
        assertFalse(engine().isGameOver(b))
    }

    @Test
    fun seedBoard_guarantees_startElement() {
        val startElement = SpawnableElement(
            atomicNumber = 11,
            symbol = "Na",
            nameKo = "소듐",
            molarMass = 22.99,
            category = ElementCategory.ALKALI_METAL
        )
        val pool = listOf(
            SpawnableElement(1, "H", "수소", 1.008, ElementCategory.NONMETAL),
            SpawnableElement(8, "O", "산소", 16.0, ElementCategory.NONMETAL)
        )
        val initialBlocksCount = 4
        val board = engine(pool).seedBoard(
            count = initialBlocksCount,
            startElementSpec = startElement
        )
        val blocks = board.blocks()
        assertEquals(initialBlocksCount, blocks.size)
        val hasNa = blocks.any { it is ElementBlock && it.symbol == "Na" && it.atomicNumber == 11 }
        assertTrue(hasNa)
    }
}
