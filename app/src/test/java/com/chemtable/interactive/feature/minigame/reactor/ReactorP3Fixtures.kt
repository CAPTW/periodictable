package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.adapter.ClassicRecipeBookReactorAdapter
import com.chemtable.interactive.feature.minigame.reactor.engine.DeterministicReactorEntityIdFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorBoardEngine
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSampleBoardFactory
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardSize
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorElementEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition

internal object ReactorP3Fixtures {
    val profile = MassReferenceSettlingProfile()
    val elements = listOf(
        ReactorElementSpecification(1, "H", "수소", 1.008),
        ReactorElementSpecification(6, "C", "탄소", 12.011),
        ReactorElementSpecification(7, "N", "질소", 14.007),
        ReactorElementSpecification(8, "O", "산소", 15.999),
        ReactorElementSpecification(11, "Na", "나트륨", 22.99),
        ReactorElementSpecification(17, "Cl", "염소", 35.45),
    )
    val catalog = ReactorElementCatalog { symbol ->
        elements.firstOrNull { it.symbol == symbol }
    }
    val masses = mapOf(
        "H2" to 2.016,
        "O2" to 31.998,
        "N2" to 28.014,
        "H2O" to 18.015,
        "CO2" to 44.009,
        "NaCl" to 58.44,
    )
    val massAuthority = ReactorMassAuthority { product ->
        requireNotNull(masses[product.formula])
    }

    fun sampleBoard(): ReactorBoardState =
        ReactorSampleBoardFactory(catalog, profile).create()

    fun p2Engine(): ReactorBoardEngine = ReactorBoardEngine(
        reactionCatalog = ClassicRecipeBookReactorAdapter(),
        massAuthority = massAuthority,
        settlingProfile = profile,
        idFactory = DeterministicReactorEntityIdFactory(),
    )

    fun element(
        id: String,
        symbol: String,
        settlingIndex: Double? = null,
    ): ReactorElementEntity {
        val spec = requireNotNull(catalog.find(symbol))
        val settling = profile.evaluate(spec.molarMass)
        return ReactorElementEntity(
            id = ReactorEntityId(id),
            atomicNumber = spec.atomicNumber,
            symbol = spec.symbol,
            displayName = spec.displayName,
            molarMass = spec.molarMass,
            settlingIndex = settlingIndex ?: settling.settlingIndex,
            settlingBehavior = settling.behavior,
        )
    }

    fun board(vararg placements: Pair<ReactorPosition, ReactorEntity>): ReactorBoardState =
        ReactorBoardState.fromPlacements(
            boardSize = ReactorBoardSize.FIVE_BY_FIVE,
            placements = placements.toMap(),
        )

    fun fullTopRowBoard(fillRest: Boolean = false): ReactorBoardState {
        val placements = mutableMapOf<ReactorPosition, ReactorEntity>()
        listOf("H", "O", "N", "C", "Na").forEachIndexed { column, symbol ->
            placements[ReactorPosition(0, column)] = element("top-$symbol-$column", symbol)
        }
        if (fillRest) {
            var serial = 0
            for (row in 1..4) {
                for (column in 0..4) {
                    val symbol = listOf("H", "O", "N", "C", "Cl")[column]
                    placements[ReactorPosition(row, column)] =
                        element("rest-$row-$column-$serial", symbol)
                    serial += 1
                }
            }
        }
        return board(*placements.toList().toTypedArray())
    }
}
