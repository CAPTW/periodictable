package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardSize
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorComposition
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorElementEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityStore
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorMoleculeEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorModelTest {

    @Test
    fun elementAndMoleculeEntitiesRetainStableValueSemantics() {
        val hydrogen = element("h-1", "H", 1.008, -30.992, SettlingBehavior.RISE)
        val hydrogenCopy = hydrogen.copy()
        val water = ReactorMoleculeEntity(
            id = ReactorEntityId("water-1"),
            formula = "H2O",
            displayName = "물",
            composition = ReactorComposition.of(mapOf("H" to 2, "O" to 1)),
            molarMass = 18.015,
            settlingIndex = -13.985,
            settlingBehavior = SettlingBehavior.RISE,
        )

        assertEquals(hydrogen, hydrogenCopy)
        assertNotSame(hydrogen, hydrogenCopy)
        assertEquals(mapOf("H" to 1), hydrogen.composition.counts)
        assertEquals(mapOf("H" to 2, "O" to 1), water.composition.counts)
        assertEquals("H2O", water.visibleLabel)
        assertEquals(1, hydrogen.footprint.cellCount)
        assertEquals(1, water.footprint.cellCount)
    }

    @Test
    fun compositionRejectsBlankSymbolsAndNonPositiveCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            ReactorComposition.of(mapOf("" to 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReactorComposition.of(mapOf("H" to 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReactorComposition.of(emptyMap())
        }
    }

    @Test
    fun entityStoreRejectsDuplicateIds() {
        val first = element("same", "H", 1.008, -30.992, SettlingBehavior.RISE)
        val second = element("same", "O", 15.999, -16.001, SettlingBehavior.RISE)

        assertThrows(IllegalArgumentException::class.java) {
            ReactorEntityStore.of(listOf(first, second))
        }
    }

    @Test
    fun boardRejectsMissingDuplicateAndOrphanEntityReferences() {
        val hydrogen = element("h-1", "H", 1.008, -30.992, SettlingBehavior.RISE)
        val oxygen = element("o-1", "O", 15.999, -16.001, SettlingBehavior.RISE)
        val size = ReactorBoardSize.FOUR_BY_FOUR

        assertThrows(IllegalArgumentException::class.java) {
            ReactorBoardState(
                boardSize = size,
                cells = listOf(ReactorEntityId("missing")) + List(15) { null },
                entityStore = ReactorEntityStore.empty(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReactorBoardState(
                boardSize = size,
                cells = listOf(hydrogen.id, hydrogen.id) + List(14) { null },
                entityStore = ReactorEntityStore.of(listOf(hydrogen)),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReactorBoardState(
                boardSize = size,
                cells = listOf(hydrogen.id) + List(15) { null },
                entityStore = ReactorEntityStore.of(listOf(hydrogen, oxygen)),
            )
        }
    }

    @Test
    fun everySupportedBoardHasExactCellsBoundsAndDeterministicIteration() {
        ReactorBoardSize.entries.forEach { boardSize ->
            val board = ReactorBoardState.empty(boardSize)
            val dimension = boardSize.dimension

            assertEquals(dimension * dimension, board.cells.size)
            assertEquals(
                (0 until dimension).flatMap { row ->
                    (0 until dimension).map { column -> ReactorPosition(row, column) }
                },
                board.positions(),
            )
            assertEquals(dimension * dimension, board.emptyPositions().size)
            assertNull(board.entityIdAt(ReactorPosition(-1, 0)))
            assertNull(board.entityIdAt(ReactorPosition(0, dimension)))
            assertEquals(boardSize, ReactorBoardSize.fromDimension(dimension))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReactorBoardSize.requireDimension(7)
        }
    }

    @Test
    fun boardCopiesInputCellsAndValidatesTurnMetadata() {
        val hydrogen = element("h-1", "H", 1.008, -30.992, SettlingBehavior.RISE)
        val input = MutableList<ReactorEntityId?>(25) { null }
        input[0] = hydrogen.id
        val board = ReactorBoardState(
            boardSize = ReactorBoardSize.FIVE_BY_FIVE,
            cells = input,
            entityStore = ReactorEntityStore.of(listOf(hydrogen)),
            turnIndex = 3,
            settlingPhase = 1,
        )

        input[0] = null

        assertEquals(hydrogen.id, board.entityIdAt(ReactorPosition(0, 0)))
        assertEquals(3, board.turnIndex)
        assertEquals(1, board.settlingPhase)
        assertTrue(board.validate().isValid)
        assertThrows(IllegalArgumentException::class.java) {
            ReactorBoardState.empty(ReactorBoardSize.FIVE_BY_FIVE, turnIndex = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReactorBoardState.empty(ReactorBoardSize.FIVE_BY_FIVE, settlingPhase = 2)
        }
    }

    private fun element(
        id: String,
        symbol: String,
        mass: Double,
        index: Double,
        behavior: SettlingBehavior,
    ) = ReactorElementEntity(
        id = ReactorEntityId(id),
        atomicNumber = 1,
        symbol = symbol,
        displayName = symbol,
        molarMass = mass,
        settlingIndex = index,
        settlingBehavior = behavior,
    )
}
