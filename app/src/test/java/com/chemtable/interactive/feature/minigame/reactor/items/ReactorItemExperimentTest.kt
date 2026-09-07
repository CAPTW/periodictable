package com.chemtable.interactive.feature.minigame.reactor.items

import org.junit.Assert.*
import org.junit.Test

class ReactorItemExperimentTest {
    private val a = ExperimentSubstrate.A
    private val b = ExperimentSubstrate.B
    private fun board(vararg pieces: Pair<Int, PolymerBundle>, budget: Int = 6): ItemExperimentState {
        val cells = MutableList<PolymerBundle?>(25) { null }
        pieces.forEach { cells[it.first] = it.second }
        return ItemExperimentState(cells, budget)
    }
    private fun totals(state: ItemExperimentState) = state.cells.filterNotNull().groupBy { it.substrate }.mapValues { (_, pieces) -> pieces.sumOf { it.units } }
    private fun reject(state: ItemExperimentState, action: ItemExperimentAction, expected: ItemEffectStatus) {
        val result = ReactorItemExperiment.resolve(state, action)
        assertFalse(result.applied)
        assertEquals(expected, result.status)
        assertSame(state, result.state)
    }

    @Test fun linkCleaveAndRelinkConserveAllFragmentsAndChargeOnce() {
        val initial = ItemExperimentState.sample()
        val linked = ReactorItemExperiment.resolve(initial, ItemExperimentAction.Link(0, 2)).state
        assertEquals(PolymerBundle(a, 2), linked.cells[0]); assertNull(linked.cells[2])
        assertEquals(PolymerBundle(b, 1), linked.cells[20]); assertEquals(5, linked.remainingActions)
        val split = ReactorItemExperiment.resolve(linked, ItemExperimentAction.Cleave(0, 1, a)).state
        assertEquals(PolymerBundle(a, 1), split.cells[0]); assertEquals(PolymerBundle(a, 1), split.cells[1])
        val again = ReactorItemExperiment.resolve(split, ItemExperimentAction.Link(0, 1)).state
        assertEquals(linked.cells, again.cells); assertEquals(3, again.remainingActions)
        assertEquals(totals(initial), totals(again)); assertEquals(6, initial.remainingActions)
    }

    @Test fun geometryRejectsDiagonalDistantWrapAndInvalidIndices() {
        val state = ItemExperimentState.sample()
        listOf(-1 to 0, 0 to 25, 0 to 0).forEach { (x,y) -> reject(state, ItemExperimentAction.Link(x,y), ItemEffectStatus.INVALID_TARGET) }
        listOf(0 to 6, 0 to 3, 4 to 5).forEach { (x,y) -> reject(state, ItemExperimentAction.Link(x,y), ItemEffectStatus.OUT_OF_RANGE) }
        val vertical = board(0 to PolymerBundle(a,1), 10 to PolymerBundle(a,1))
        assertTrue(ReactorItemExperiment.resolve(vertical, ItemExperimentAction.Link(0,10)).applied)
    }

    @Test fun incompatibleEmptyAndOversizedLinksDoNotCharge() {
        reject(board(0 to PolymerBundle(a,1), 1 to PolymerBundle(b,1)), ItemExperimentAction.Link(0,1), ItemEffectStatus.INCOMPATIBLE)
        reject(board(0 to PolymerBundle(a,1)), ItemExperimentAction.Link(0,1), ItemEffectStatus.INVALID_TARGET)
        reject(board(0 to PolymerBundle(a,3), 1 to PolymerBundle(a,2)), ItemExperimentAction.Link(0,1), ItemEffectStatus.TOO_LARGE)
        assertTrue(ReactorItemExperiment.resolve(board(0 to PolymerBundle(a,2),1 to PolymerBundle(a,2)),ItemExperimentAction.Link(0,1)).applied)
    }

    @Test fun enzymeMustMatchAndNeverCleavesSynthetic() {
        reject(board(0 to PolymerBundle(a,2)), ItemExperimentAction.Cleave(0,1,b), ItemEffectStatus.WRONG_ENZYME)
        ExperimentSubstrate.entries.forEach { enzyme ->
            reject(board(0 to PolymerBundle(ExperimentSubstrate.SYNTHETIC,2)), ItemExperimentAction.Cleave(0,1,enzyme), ItemEffectStatus.WRONG_ENZYME)
        }
        assertTrue(ReactorItemExperiment.resolve(board(0 to PolymerBundle(b,3)),ItemExperimentAction.Cleave(0,1,b)).applied)
    }

    @Test fun cleavageRequiresBundleAndEmptyNearbyDestination() {
        reject(board(0 to PolymerBundle(a,1)), ItemExperimentAction.Cleave(0,1,a), ItemEffectStatus.NOT_A_BUNDLE)
        reject(board(0 to PolymerBundle(a,2),1 to PolymerBundle(a,1)), ItemExperimentAction.Cleave(0,1,a), ItemEffectStatus.NO_SPACE)
        reject(board(0 to PolymerBundle(a,2)), ItemExperimentAction.Cleave(0,6,a), ItemEffectStatus.OUT_OF_RANGE)
    }

    @Test fun exhaustedExperimentCannotChangeAndResetIsFresh() {
        val emptyBudget = board(0 to PolymerBundle(a,2), budget=0)
        reject(emptyBudget, ItemExperimentAction.Cleave(0,1,a), ItemEffectStatus.EXHAUSTED)
        assertEquals(6,ItemExperimentState.sample().remainingActions)
    }

    @Test fun repeatedCommandsAndTraceAreDeterministicWithoutFreeDuplicateCharge() {
        val actions = listOf(ItemExperimentAction.Link(0,2), ItemExperimentAction.Link(0,2), ItemExperimentAction.Cleave(0,1,a), ItemExperimentAction.Link(0,1))
        fun run(): ItemExperimentState = actions.fold(ItemExperimentState.sample()) { state, action -> ReactorItemExperiment.resolve(state,action).state }
        assertEquals(run().cells,run().cells); assertEquals(3,run().remainingActions)
    }

    @Test fun stateDefensivelyCopiesInputAndValidatesBounds() {
        val cells = MutableList<PolymerBundle?>(25) { null }
        val state = ItemExperimentState(cells); cells[0] = PolymerBundle(a,1)
        assertNull(state.cells[0])
        assertThrows(IllegalArgumentException::class.java) { PolymerBundle(a,5) }
        assertThrows(IllegalArgumentException::class.java) { ItemExperimentState(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { ItemExperimentState(cells,-1) }
    }
}
