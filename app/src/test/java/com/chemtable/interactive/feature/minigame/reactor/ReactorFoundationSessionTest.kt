package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorFoundationSessionTest {

    private val elements = listOf(
        ReactorElementSpecification(1, "H", "수소", 1.008),
        ReactorElementSpecification(6, "C", "탄소", 12.011),
        ReactorElementSpecification(7, "N", "질소", 14.007),
        ReactorElementSpecification(8, "O", "산소", 15.999),
        ReactorElementSpecification(11, "Na", "나트륨", 22.99),
        ReactorElementSpecification(17, "Cl", "염소", 35.45),
    )
    private val masses = mapOf(
        "H2" to 2.016,
        "O2" to 31.998,
        "N2" to 28.014,
        "H2O" to 18.015,
        "CO2" to 44.009,
        "NaCl" to 58.44,
    )

    @Test
    fun sessionStartsFromExactSampleAndTracksSelectionOnlyInMemory() {
        val session = session()
        val hydrogenId = session.state.board.entityIdAt(ReactorPosition(0, 0))

        session.selectEntity(hydrogenId)

        assertEquals(25, session.state.board.cells.size)
        assertEquals(0, session.state.board.turnIndex)
        assertEquals(0, session.state.board.settlingPhase)
        assertEquals(hydrogenId, session.state.selectedEntityId)
        assertTrue(session.state.latestEvents.isEmpty())
        assertFalse(session.state.lastReplayVerified)
        assertNull(session.state.errorMessage)
    }

    @Test
    fun oneSwipePublishesOnlyAReplayVerifiedTurnThenResetRestoresExactSample() {
        val session = session()
        val initial = session.state.board

        session.swipe(ReactorDirection.LEFT)

        assertEquals(1, session.state.board.turnIndex)
        assertEquals(1, session.state.board.settlingPhase)
        assertTrue(session.state.lastReplayVerified)
        assertTrue(session.state.latestEvents.isNotEmpty())
        assertEquals(
            listOf("H2", "O2", "N2", "NaCl"),
            session.state.latestEvents.filterIsInstance<ReactorTurnEvent.Merge>()
                .map { it.resultFormula },
        )

        session.reset()

        assertEquals(initial, session.state.board)
        assertTrue(session.state.latestEvents.isEmpty())
        assertFalse(session.state.lastReplayVerified)
        assertNull(session.state.selectedEntityId)
        assertNull(session.state.errorMessage)
    }

    private fun session(): ReactorFoundationSession = ReactorFoundationSession(
        elementCatalog = ReactorElementCatalog { symbol ->
            elements.firstOrNull { it.symbol == symbol }
        },
        massAuthority = ReactorMassAuthority { product ->
            requireNotNull(masses[product.formula])
        },
        settlingProfile = MassReferenceSettlingProfile(),
    )
}
