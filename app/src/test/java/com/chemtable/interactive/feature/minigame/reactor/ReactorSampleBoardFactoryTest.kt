package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.adapter.ClassicRecipeBookReactorAdapter
import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorBoardEngine
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorEntityIdFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSampleBoardFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSettlingResolver
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardSize
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactorSampleBoardFactoryTest {

    private val profile = MassReferenceSettlingProfile()
    private val elements = listOf(
        ReactorElementSpecification(1, "H", "수소", 1.008),
        ReactorElementSpecification(6, "C", "탄소", 12.011),
        ReactorElementSpecification(7, "N", "질소", 14.007),
        ReactorElementSpecification(8, "O", "산소", 15.999),
        ReactorElementSpecification(11, "Na", "나트륨", 22.99),
        ReactorElementSpecification(17, "Cl", "염소", 35.45),
    )
    private val catalog = ReactorElementCatalog { symbol -> elements.firstOrNull { it.symbol == symbol } }
    private val factory = ReactorSampleBoardFactory(catalog, profile)

    @Test
    fun sampleUsesExactFiveByFiveAuthorityAndDeterministicIds() {
        val sample = factory.create()
        val expected = mapOf(
            ReactorPosition(0, 0) to "H",
            ReactorPosition(0, 2) to "H",
            ReactorPosition(0, 4) to "C",
            ReactorPosition(1, 0) to "O",
            ReactorPosition(1, 2) to "O",
            ReactorPosition(1, 4) to "O",
            ReactorPosition(2, 0) to "N",
            ReactorPosition(2, 2) to "N",
            ReactorPosition(2, 4) to "O",
            ReactorPosition(3, 1) to "Na",
            ReactorPosition(3, 3) to "Cl",
            ReactorPosition(4, 2) to "O",
        )

        assertEquals(ReactorBoardSize.FIVE_BY_FIVE, sample.boardSize)
        assertEquals(25, sample.cells.size)
        assertEquals(0, sample.turnIndex)
        assertEquals(0, sample.settlingPhase)
        assertEquals(expected, sample.occupiedPositions().associateWith { sample.entityAt(it)?.visibleLabel })
        assertEquals(
            listOf(
                "sample-H-1", "sample-H-2", "sample-C-1",
                "sample-O-1", "sample-O-2", "sample-O-3",
                "sample-N-1", "sample-N-2", "sample-O-4",
                "sample-Na-1", "sample-Cl-1", "sample-O-5",
            ).toSet(),
            sample.entityStore.ids.map { it.value }.toSet(),
        )
        assertEquals(13, sample.emptyPositions().size)
    }

    @Test
    fun sampleContainsBothDirectionsAndAnInitialPhaseZeroInstability() {
        val sample = factory.create()
        val behaviors = sample.entityStore.entities.values.map { it.settlingBehavior }.toSet()

        assertTrue(SettlingBehavior.RISE in behaviors)
        assertTrue(SettlingBehavior.SINK in behaviors)
        assertTrue(ReactorSettlingResolver(profile).resolve(sample).events.isNotEmpty())
    }

    @Test
    fun leftSwipeCreatesCanonicalMoleculesFromTheSample() {
        val masses = mapOf(
            "H2" to 2.016,
            "O2" to 31.998,
            "N2" to 28.014,
            "H2O" to 18.015,
            "CO2" to 44.009,
            "NaCl" to 58.44,
        )
        val engine = ReactorBoardEngine(
            reactionCatalog = ClassicRecipeBookReactorAdapter(),
            massAuthority = ReactorMassAuthority { product -> requireNotNull(masses[product.formula]) },
            settlingProfile = profile,
            idFactory = ReactorEntityIdFactory { turn, ordinal, consumed, product ->
                ReactorEntityId("$turn-$ordinal-${consumed.joinToString("+") { it.value }}-${product.formula}")
            },
        )

        val result = engine.resolveTurn(factory.create(), ReactorDirection.LEFT)

        assertEquals(listOf("H2", "O2", "N2", "NaCl"), result.formulasCreated)
        assertEquals(1, result.resultingTurnIndex)
        assertEquals(1, result.nextPhase)
        assertTrue(result.board.validate().isValid)
    }

    @Test
    fun resetFactoryRecreatesExactValueAndMissingAuthorityFailsClosed() {
        assertEquals(factory.create(), factory.create())

        val missingChlorine = ReactorSampleBoardFactory(
            elementCatalog = ReactorElementCatalog { symbol ->
                elements.firstOrNull { it.symbol == symbol && symbol != "Cl" }
            },
            settlingProfile = profile,
        )
        assertThrows(IllegalArgumentException::class.java) {
            missingChlorine.create()
        }
    }
}
