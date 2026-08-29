package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSettlingResolver
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardSize
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorElementEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorSettlingResolverTest {

    private val resolver = ReactorSettlingResolver(MassReferenceSettlingProfile())

    @Test
    fun sinkAboveEmptyMovesDownExactlyOneCell() {
        val sink = entity("sink", 44.0)
        val board = board(phase = 0, ReactorPosition(0, 0) to sink)

        val outcome = resolver.resolve(board)

        assertEquals(sink.id, outcome.board.entityIdAt(ReactorPosition(1, 0)))
        assertEquals(null, outcome.board.entityIdAt(ReactorPosition(0, 0)))
        assertEquals(
            listOf(
                ReactorTurnEvent.SettlingMove(
                    entityId = sink.id,
                    from = ReactorPosition(0, 0),
                    to = ReactorPosition(1, 0),
                    behavior = SettlingBehavior.SINK,
                    settlingIndex = 12.0,
                    phase = 0,
                ),
            ),
            outcome.events,
        )
    }

    @Test
    fun riseBelowEmptyMovesUpExactlyOneCell() {
        val rise = entity("rise", 18.0)
        val outcome = resolver.resolve(board(phase = 0, ReactorPosition(1, 1) to rise))

        assertEquals(rise.id, outcome.board.entityIdAt(ReactorPosition(0, 1)))
        assertEquals(null, outcome.board.entityIdAt(ReactorPosition(1, 1)))
        assertTrue(outcome.changed)
    }

    @Test
    fun emptySpaceNeverMovesNeutralOrTheWrongDirectionalBehavior() {
        val neutralAbove = entity("neutral-above", 32.0)
        val neutralBelow = entity("neutral-below", 32.0)
        val riseAbove = entity("rise-above", 18.0)
        val sinkBelow = entity("sink-below", 44.0)
        val board = board(
            phase = 0,
            ReactorPosition(0, 0) to neutralAbove,
            ReactorPosition(1, 1) to neutralBelow,
            ReactorPosition(0, 2) to riseAbove,
            ReactorPosition(1, 3) to sinkBelow,
        )

        val outcome = resolver.resolve(board)

        assertEquals(board, outcome.board)
        assertTrue(outcome.events.isEmpty())
        assertFalse(outcome.changed)
    }

    @Test
    fun unstableOccupiedPairSwapsWhileStableAndEqualPairsDoNot() {
        val heavyUpper = entity("heavy-upper", 58.0)
        val lightLower = entity("light-lower", 18.0)
        val lightUpper = entity("light-upper", 18.0)
        val heavyLower = entity("heavy-lower", 58.0)
        val equalUpper = entity("equal-upper", 44.0)
        val equalLower = entity("equal-lower", 44.0)
        val board = board(
            phase = 0,
            ReactorPosition(0, 0) to heavyUpper,
            ReactorPosition(1, 0) to lightLower,
            ReactorPosition(0, 1) to lightUpper,
            ReactorPosition(1, 1) to heavyLower,
            ReactorPosition(0, 2) to equalUpper,
            ReactorPosition(1, 2) to equalLower,
        )

        val outcome = resolver.resolve(board)

        assertEquals(lightLower.id, outcome.board.entityIdAt(ReactorPosition(0, 0)))
        assertEquals(heavyUpper.id, outcome.board.entityIdAt(ReactorPosition(1, 0)))
        assertEquals(lightUpper.id, outcome.board.entityIdAt(ReactorPosition(0, 1)))
        assertEquals(heavyLower.id, outcome.board.entityIdAt(ReactorPosition(1, 1)))
        assertEquals(equalUpper.id, outcome.board.entityIdAt(ReactorPosition(0, 2)))
        assertEquals(equalLower.id, outcome.board.entityIdAt(ReactorPosition(1, 2)))
        val swap = outcome.events.single() as ReactorTurnEvent.SettlingSwap
        assertEquals(heavyUpper.id, swap.upperEntityId)
        assertEquals(lightLower.id, swap.lowerEntityId)
        assertEquals(0, swap.phase)
    }

    @Test
    fun disjointPairsUseOneSnapshotAndNoEntityMovesTwice() {
        val sink = entity("sink", 44.0)
        val rise = entity("rise", 18.0)
        val outcome = resolver.resolve(
            board(
                phase = 0,
                ReactorPosition(0, 0) to sink,
                ReactorPosition(3, 0) to rise,
            ),
        )

        assertEquals(sink.id, outcome.board.entityIdAt(ReactorPosition(1, 0)))
        assertEquals(rise.id, outcome.board.entityIdAt(ReactorPosition(2, 0)))
        assertEquals(2, outcome.events.size)
        assertEquals(setOf(sink.id, rise.id), outcome.events.flatMap { it.movedEntityIds }.toSet())
        assertEquals(2, outcome.events.flatMap { it.movedEntityIds }.size)
    }

    @Test
    fun phaseOneUsesOnlyRowsOneTwoAndThreeFourOnOddBoard() {
        val unmatchedTop = entity("top", 44.0)
        val first = entity("first", 44.0)
        val second = entity("second", 44.0)
        val outcome = resolver.resolve(
            board(
                phase = 1,
                ReactorPosition(0, 0) to unmatchedTop,
                ReactorPosition(1, 1) to first,
                ReactorPosition(3, 2) to second,
            ),
        )

        assertEquals(unmatchedTop.id, outcome.board.entityIdAt(ReactorPosition(0, 0)))
        assertEquals(first.id, outcome.board.entityIdAt(ReactorPosition(2, 1)))
        assertEquals(second.id, outcome.board.entityIdAt(ReactorPosition(4, 2)))
        assertEquals(listOf(first.id, second.id), outcome.events.flatMap { it.movedEntityIds })
        assertTrue(outcome.events.all { event ->
            (event as ReactorTurnEvent.SettlingMove).phase == 1
        })
    }

    @Test
    fun columnsResolveIndependentlyAndEventOrderIsPairThenColumn() {
        val rowZeroColTwo = entity("r0c2", 44.0)
        val rowZeroColZero = entity("r0c0", 44.0)
        val rowTwoColOne = entity("r2c1", 44.0)
        val board = board(
            phase = 0,
            ReactorPosition(0, 2) to rowZeroColTwo,
            ReactorPosition(0, 0) to rowZeroColZero,
            ReactorPosition(2, 1) to rowTwoColOne,
        )

        val first = resolver.resolve(board)
        val second = resolver.resolve(board)

        assertEquals(listOf(rowZeroColZero.id, rowZeroColTwo.id, rowTwoColOne.id), first.events.flatMap { it.movedEntityIds })
        assertEquals(first, second)
        assertTrue(first.board.validate().isValid)
    }

    private fun entity(id: String, mass: Double): ReactorElementEntity {
        val settling = MassReferenceSettlingProfile().evaluate(mass)
        return ReactorElementEntity(
            id = ReactorEntityId(id),
            atomicNumber = 1,
            symbol = id.take(2),
            displayName = id,
            molarMass = mass,
            settlingIndex = settling.settlingIndex,
            settlingBehavior = settling.behavior,
        )
    }

    private fun board(
        phase: Int,
        vararg placements: Pair<ReactorPosition, ReactorEntity>,
    ): ReactorBoardState = ReactorBoardState.fromPlacements(
        boardSize = ReactorBoardSize.FIVE_BY_FIVE,
        placements = placements.toMap(),
        settlingPhase = phase,
    )
}
