package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorBoardEngine
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorEntityIdFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorProductSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorReactionCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardSize
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorComposition
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorElementEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorMoleculeEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorBoardEngineTest {

    private val profile = MassReferenceSettlingProfile()
    private val products = listOf(
        product("H2", "수소 분자", mapOf("H" to 2)),
        product("O2", "산소 분자", mapOf("O" to 2)),
        product("N2", "질소 분자", mapOf("N" to 2)),
        product("H2O", "물", mapOf("H" to 2, "O" to 1)),
        product("CO2", "이산화탄소", mapOf("C" to 1, "O" to 2)),
        product("NaCl", "염화 소듐", mapOf("Na" to 1, "Cl" to 1)),
    )
    private val masses = mapOf(
        "H2" to 2.016,
        "O2" to 31.998,
        "N2" to 28.014,
        "H2O" to 18.015,
        "CO2" to 44.009,
        "NaCl" to 58.44,
    )
    private val engine = ReactorBoardEngine(
        reactionCatalog = ReactorReactionCatalog { composition ->
            products.firstOrNull { it.composition == composition }
        },
        massAuthority = ReactorMassAuthority { specification ->
            requireNotNull(masses[specification.formula])
        },
        settlingProfile = profile,
        idFactory = ReactorEntityIdFactory { turnIndex, mergeOrdinal, consumed, specification ->
            ReactorEntityId(
                "turn-$turnIndex-merge-$mergeOrdinal-${consumed.joinToString("+") { it.value }}-${specification.formula}",
            )
        },
    )

    @Test
    fun playerCompressionRunsBeforeSettlingAndMaySpanMultipleCells() {
        val hydrogen = element("h", "H", 1.008)
        val result = engine.resolveTurn(
            board(
                ReactorBoardSize.FOUR_BY_FOUR,
                ReactorPosition(0, 3) to hydrogen,
            ),
            ReactorDirection.LEFT,
        )

        assertEquals(hydrogen.id, result.board.entityIdAt(ReactorPosition(0, 0)))
        val movement = result.events.single() as ReactorTurnEvent.PlayerMove
        assertEquals(ReactorPosition(0, 3), movement.from)
        assertEquals(ReactorPosition(0, 0), movement.to)
        assertEquals(0, result.previousTurnIndex)
        assertEquals(1, result.resultingTurnIndex)
        assertEquals(0, result.phaseUsed)
        assertEquals(1, result.nextPhase)
        assertTrue(result.playerCompressionChanged)
        assertFalse(result.settlingChanged)
    }

    @Test
    fun exactSixWhitelistedProductsCanBeCreated() {
        val cases = listOf(
            listOf(element("h1", "H", 1.008), element("h2", "H", 1.008)) to "H2",
            listOf(element("o1", "O", 15.999), element("o2", "O", 15.999)) to "O2",
            listOf(element("n1", "N", 14.007), element("n2", "N", 14.007)) to "N2",
            listOf(molecule("h2m", "H2", mapOf("H" to 2), 2.016), element("o3", "O", 15.999)) to "H2O",
            listOf(element("c1", "C", 12.011), molecule("o2m", "O2", mapOf("O" to 2), 31.998)) to "CO2",
            listOf(element("na1", "Na", 22.99), element("cl1", "Cl", 35.45)) to "NaCl",
        )

        cases.forEachIndexed { index, (inputs, expectedFormula) ->
            val result = engine.resolveTurn(
                board(
                    ReactorBoardSize.FIVE_BY_FIVE,
                    ReactorPosition(0, 0) to inputs[0],
                    ReactorPosition(0, 1) to inputs[1],
                ),
                ReactorDirection.LEFT,
            )

            assertEquals(listOf(expectedFormula), result.formulasCreated)
            assertTrue(result.mergeOccurred)
            val created = result.board.entityStore.entities.values
                .filterIsInstance<ReactorMoleculeEntity>()
                .single()
            assertEquals(expectedFormula, created.formula)
            assertTrue(result.board.validate().isValid)
            assertEquals("case $index", 1, result.resultingTurnIndex)
        }
    }

    @Test
    fun undefinedCombinationStaysSeparateAndNewProductCannotRemergeInSamePass() {
        val undefined = engine.resolveTurn(
            board(
                ReactorBoardSize.FIVE_BY_FIVE,
                ReactorPosition(0, 0) to element("h", "H", 1.008),
                ReactorPosition(0, 1) to element("na", "Na", 22.99),
            ),
            ReactorDirection.LEFT,
        )
        assertFalse(undefined.mergeOccurred)
        assertEquals(2, undefined.board.entityStore.size)

        val oneMergeOnly = engine.resolveTurn(
            board(
                ReactorBoardSize.FIVE_BY_FIVE,
                ReactorPosition(0, 0) to element("h1", "H", 1.008),
                ReactorPosition(0, 1) to element("h2", "H", 1.008),
                ReactorPosition(0, 2) to element("o", "O", 15.999),
            ),
            ReactorDirection.LEFT,
        )

        assertEquals(listOf("H2"), oneMergeOnly.formulasCreated)
        assertEquals(2, oneMergeOnly.board.entityStore.size)
        assertTrue(oneMergeOnly.board.entityStore.entities.values.any { it.visibleLabel == "O" })
    }

    @Test
    fun mergedSinkParticipatesInExactlyOneSettlingCell() {
        val result = engine.resolveTurn(
            board(
                ReactorBoardSize.FIVE_BY_FIVE,
                ReactorPosition(0, 0) to element("c", "C", 12.011),
                ReactorPosition(0, 1) to molecule("o2", "O2", mapOf("O" to 2), 31.998),
            ),
            ReactorDirection.LEFT,
        )

        val product = result.board.entityStore.entities.values
            .filterIsInstance<ReactorMoleculeEntity>()
            .single()
        assertEquals("CO2", product.formula)
        assertEquals(product.id, result.board.entityIdAt(ReactorPosition(1, 0)))
        assertEquals(null, result.board.entityIdAt(ReactorPosition(0, 0)))
        assertEquals(
            listOf(ReactorTurnEvent.Merge::class, ReactorTurnEvent.SettlingMove::class),
            result.events.map { it::class },
        )
    }

    @Test
    fun noOpSwipeStillAdvancesTurnTogglesPhaseAndMaySettle() {
        val sink = element("sink", "Cl", 44.0)
        val settlingTurn = engine.resolveTurn(
            board(ReactorBoardSize.FIVE_BY_FIVE, ReactorPosition(0, 0) to sink),
            ReactorDirection.LEFT,
        )

        assertFalse(settlingTurn.playerCompressionChanged)
        assertTrue(settlingTurn.settlingChanged)
        assertTrue(settlingTurn.anyEntityMoved)
        assertEquals(1, settlingTurn.resultingTurnIndex)
        assertEquals(1, settlingTurn.nextPhase)

        val rise = element("rise", "H", 1.008)
        val fullyStable = engine.resolveTurn(
            board(ReactorBoardSize.FIVE_BY_FIVE, ReactorPosition(0, 0) to rise),
            ReactorDirection.LEFT,
        )

        assertFalse(fullyStable.playerCompressionChanged)
        assertFalse(fullyStable.mergeOccurred)
        assertFalse(fullyStable.settlingChanged)
        assertFalse(fullyStable.anyEntityMoved)
        assertTrue(fullyStable.events.isEmpty())
        assertEquals(1, fullyStable.resultingTurnIndex)
        assertEquals(1, fullyStable.nextPhase)
    }

    @Test
    fun eventStagesIdsAndResultsAreDeterministicAcrossRepeatedRuns() {
        val initial = board(
            ReactorBoardSize.FIVE_BY_FIVE,
            ReactorPosition(0, 3) to element("h1", "H", 1.008),
            ReactorPosition(0, 4) to element("h2", "H", 1.008),
            ReactorPosition(2, 4) to element("sink", "Cl", 44.0),
        )

        val first = engine.resolveTurn(initial, ReactorDirection.LEFT)
        val second = engine.resolveTurn(initial, ReactorDirection.LEFT)

        assertEquals(first, second)
        assertEquals(
            listOf(
                ReactorTurnEvent.Merge::class,
                ReactorTurnEvent.PlayerMove::class,
                ReactorTurnEvent.SettlingMove::class,
            ).sortedBy { expectedStage(it) },
            first.events.map { it::class },
        )
        assertEquals(first.events.sortedBy { actualStage(it) }, first.events)
    }

    @Test
    fun allSupportedDimensionsRemainValidAfterEveryDirection() {
        ReactorBoardSize.entries.forEach { size ->
            ReactorDirection.entries.forEach { direction ->
                val dimension = size.dimension
                val result = engine.resolveTurn(
                    board(
                        size,
                        ReactorPosition(dimension - 1, dimension - 1) to element("${size.name}-${direction.name}", "H", 1.008),
                    ),
                    direction,
                )

                assertEquals(dimension * dimension, result.board.cells.size)
                assertTrue(result.board.validate().isValid)
                assertNotNull(result.board.entityStore.entities.values.single())
            }
        }
    }

    private fun product(
        formula: String,
        displayName: String,
        composition: Map<String, Int>,
    ) = ReactorProductSpecification(
        formula = formula,
        displayName = displayName,
        composition = ReactorComposition.of(composition),
    )

    private fun element(id: String, symbol: String, mass: Double): ReactorElementEntity {
        val settling = profile.evaluate(mass)
        return ReactorElementEntity(
            id = ReactorEntityId(id),
            atomicNumber = 1,
            symbol = symbol,
            displayName = symbol,
            molarMass = mass,
            settlingIndex = settling.settlingIndex,
            settlingBehavior = settling.behavior,
        )
    }

    private fun molecule(
        id: String,
        formula: String,
        composition: Map<String, Int>,
        mass: Double,
    ): ReactorMoleculeEntity {
        val settling = profile.evaluate(mass)
        return ReactorMoleculeEntity(
            id = ReactorEntityId(id),
            formula = formula,
            displayName = formula,
            composition = ReactorComposition.of(composition),
            molarMass = mass,
            settlingIndex = settling.settlingIndex,
            settlingBehavior = settling.behavior,
        )
    }

    private fun board(
        size: ReactorBoardSize,
        vararg placements: Pair<ReactorPosition, ReactorEntity>,
    ): ReactorBoardState = ReactorBoardState.fromPlacements(size, placements.toMap())

    private fun expectedStage(type: kotlin.reflect.KClass<out ReactorTurnEvent>): Int = when (type) {
        ReactorTurnEvent.PlayerMove::class -> 0
        ReactorTurnEvent.Merge::class -> 1
        else -> 2
    }

    private fun actualStage(event: ReactorTurnEvent): Int = when (event) {
        is ReactorTurnEvent.ItemApplied -> error("Item receipts cannot enter a P2-only trace")
        is ReactorTurnEvent.PlayerMove -> 0
        is ReactorTurnEvent.Merge -> 1
        is ReactorTurnEvent.SettlingMove,
        is ReactorTurnEvent.SettlingSwap,
        -> 2
        is ReactorTurnEvent.FeedAttempted -> 3
        is ReactorTurnEvent.FeedPlaced,
        is ReactorTurnEvent.FeedBlocked,
        -> 4
        is ReactorTurnEvent.PressureChanged -> 5
        is ReactorTurnEvent.OverflowTriggered -> 6
        is ReactorTurnEvent.RecoveryRequested -> 7
        is ReactorTurnEvent.EmergencyVentApplied -> 8
        is ReactorTurnEvent.EntityVented -> 9
        is ReactorTurnEvent.RecoveryCompleted -> 10
    }
}
