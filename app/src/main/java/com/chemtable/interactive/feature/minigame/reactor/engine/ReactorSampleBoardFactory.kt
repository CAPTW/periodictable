package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardSize
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorElementEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition

data class ReactorElementSpecification(
    val atomicNumber: Int,
    val symbol: String,
    val displayName: String,
    val molarMass: Double,
) {
    init {
        require(atomicNumber > 0) { "Reactor element atomic number must be positive" }
        require(symbol.isNotBlank()) { "Reactor element symbol must not be blank" }
        require(displayName.isNotBlank()) { "Reactor element display name must not be blank" }
        require(molarMass.isFinite() && molarMass >= 0.0) {
            "Reactor element molar mass must be finite and non-negative"
        }
    }
}

fun interface ReactorElementCatalog {
    fun find(symbol: String): ReactorElementSpecification?
}

/** Creates the deterministic P2 foundation board from authoritative element metadata. */
class ReactorSampleBoardFactory(
    private val elementCatalog: ReactorElementCatalog,
    private val settlingProfile: ReactorSettlingProfile,
) {
    fun create(): ReactorBoardState {
        val occurrences = mutableMapOf<String, Int>()
        val placements = SAMPLE_LAYOUT.associate { (position, symbol) ->
            val specification = requireNotNull(elementCatalog.find(symbol)) {
                "Missing Reactor sample element authority: $symbol"
            }
            val occurrence = occurrences.getOrDefault(symbol, 0) + 1
            occurrences[symbol] = occurrence
            val settling = settlingProfile.evaluate(specification.molarMass)
            position to ReactorElementEntity(
                id = ReactorEntityId("sample-$symbol-$occurrence"),
                atomicNumber = specification.atomicNumber,
                symbol = specification.symbol,
                displayName = specification.displayName,
                molarMass = specification.molarMass,
                settlingIndex = settling.settlingIndex,
                settlingBehavior = settling.behavior,
            )
        }
        return ReactorBoardState.fromPlacements(
            boardSize = ReactorBoardSize.FIVE_BY_FIVE,
            placements = placements,
            turnIndex = 0,
            settlingPhase = 0,
        )
    }

    private companion object {
        val SAMPLE_LAYOUT = listOf(
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
    }
}
