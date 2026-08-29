package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorBoardEngine
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorEntityIdFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorEventReplayer
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
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorEventReplayerTest {

    private val profile = MassReferenceSettlingProfile()
    private val h2 = ReactorProductSpecification(
        formula = "H2",
        displayName = "수소 분자",
        composition = ReactorComposition.of(mapOf("H" to 2)),
    )
    private val engine = ReactorBoardEngine(
        reactionCatalog = ReactorReactionCatalog { composition -> h2.takeIf { it.composition == composition } },
        massAuthority = ReactorMassAuthority { 2.016 },
        settlingProfile = profile,
        idFactory = ReactorEntityIdFactory { turn, ordinal, consumed, product ->
            ReactorEntityId("$turn-$ordinal-${consumed.joinToString("+") { it.value }}-${product.formula}")
        },
    )
    private val replayer = ReactorEventReplayer()

    @Test
    fun replayFromPreTurnStateProducesExactTurnResult() {
        val initial = initialBoard()
        val result = engine.resolveTurn(initial, ReactorDirection.LEFT)

        val validation = replayer.validate(initial, result)

        assertTrue(validation.errors.toString(), validation.isValid)
        assertEquals(result.board, validation.replayedBoard)
    }

    @Test
    fun repeatedTurnProducesByteStructurallyEqualEventsAndReplay() {
        val initial = initialBoard()
        val first = engine.resolveTurn(initial, ReactorDirection.LEFT)
        val second = engine.resolveTurn(initial, ReactorDirection.LEFT)

        assertEquals(first, second)
        assertEquals(replayer.validate(initial, first), replayer.validate(initial, second))
    }

    @Test
    fun missingMovementEventIsRejected() {
        val initial = initialBoard()
        val result = engine.resolveTurn(initial, ReactorDirection.LEFT)
        val corrupted = result.copy(events = result.events.filterNot { it is ReactorTurnEvent.PlayerMove })

        assertInvalid(initial, corrupted, "source")
    }

    @Test
    fun invalidSourceAndDuplicateMovementAreRejected() {
        val initial = initialBoard()
        val result = engine.resolveTurn(initial, ReactorDirection.LEFT)
        val movement = result.events.filterIsInstance<ReactorTurnEvent.PlayerMove>().single()
        val invalidSource = result.copy(
            events = result.events.map { event ->
                if (event == movement) movement.copy(from = ReactorPosition(4, 4)) else event
            },
        )
        val duplicate = result.copy(events = listOf(movement, movement) + result.events.drop(1))

        assertInvalid(initial, invalidSource, "source")
        assertInvalid(initial, duplicate, "duplicate")
    }

    @Test
    fun mergeWithMissingInputAndDestinationCollisionAreRejected() {
        val initial = initialBoard()
        val result = engine.resolveTurn(initial, ReactorDirection.LEFT)
        val movement = result.events.filterIsInstance<ReactorTurnEvent.PlayerMove>().single()
        val merge = result.events.filterIsInstance<ReactorTurnEvent.Merge>().single()
        val missingInput = result.copy(
            events = result.events.map { event ->
                if (event == merge) {
                    merge.copy(
                        consumedEntityIds = listOf(ReactorEntityId("missing"), merge.consumedEntityIds[1]),
                    )
                } else {
                    event
                }
            },
        )
        val collision = result.copy(
            events = result.events.map { event ->
                if (event == movement) movement.copy(to = ReactorPosition(2, 0)) else event
            },
        )

        assertInvalid(initial, missingInput, "merge")
        assertInvalid(initial, collision, "collision")
    }

    @Test
    fun settlingBeyondOneCellAndWrongStageOrderAreRejected() {
        val initial = initialBoard()
        val result = engine.resolveTurn(initial, ReactorDirection.LEFT)
        val settling = result.events.filterIsInstance<ReactorTurnEvent.SettlingMove>().single()
        val tooFar = result.copy(
            events = result.events.map { event ->
                if (event == settling) settling.copy(to = ReactorPosition(settling.from.row + 2, settling.from.column)) else event
            },
        )
        val player = result.events.filterIsInstance<ReactorTurnEvent.PlayerMove>().single()
        val merge = result.events.filterIsInstance<ReactorTurnEvent.Merge>().single()
        val wrongOrder = result.copy(events = listOf(merge, player, settling))

        assertInvalid(initial, tooFar, "one vertical cell")
        assertInvalid(initial, wrongOrder, "stage")
    }

    @Test
    fun validEventsCannotClaimAMismatchedFinalBoard() {
        val initial = initialBoard()
        val result = engine.resolveTurn(initial, ReactorDirection.LEFT)
        val mismatched = result.copy(
            board = initial.with(turnIndex = 1, settlingPhase = 1),
        )

        assertInvalid(initial, mismatched, "mismatch")
    }

    private fun assertInvalid(
        initial: ReactorBoardState,
        result: com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnResult,
        expectedText: String,
    ) {
        val validation = replayer.validate(initial, result)
        assertFalse(validation.isValid)
        assertEquals(null, validation.replayedBoard)
        assertTrue(validation.errors.joinToString(" | "), validation.errors.any { it.contains(expectedText, ignoreCase = true) })
    }

    private fun initialBoard(): ReactorBoardState = board(
        ReactorPosition(0, 3) to element("h1", "H", 1.008),
        ReactorPosition(0, 4) to element("h2", "H", 1.008),
        ReactorPosition(2, 0) to element("blocker", "Na", 22.99),
        ReactorPosition(2, 4) to element("sink", "Cl", 44.0),
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

    private fun board(vararg placements: Pair<ReactorPosition, ReactorEntity>): ReactorBoardState =
        ReactorBoardState.fromPlacements(ReactorBoardSize.FIVE_BY_FIVE, placements.toMap())
}
