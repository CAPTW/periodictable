package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorComposition
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId

data class ReactorProductSpecification(
    val formula: String,
    val displayName: String,
    val composition: ReactorComposition,
) {
    init {
        require(formula.isNotBlank()) { "Reactor product formula must not be blank" }
    }
}

fun interface ReactorReactionCatalog {
    fun findProduct(composition: ReactorComposition): ReactorProductSpecification?
}

fun interface ReactorMassAuthority {
    fun molarMassOf(specification: ReactorProductSpecification): Double
}

fun interface ReactorEntityIdFactory {
    fun create(
        turnIndex: Int,
        mergeOrdinal: Int,
        consumedEntityIds: List<ReactorEntityId>,
        specification: ReactorProductSpecification,
    ): ReactorEntityId
}

class DeterministicReactorEntityIdFactory : ReactorEntityIdFactory {
    override fun create(
        turnIndex: Int,
        mergeOrdinal: Int,
        consumedEntityIds: List<ReactorEntityId>,
        specification: ReactorProductSpecification,
    ): ReactorEntityId = ReactorEntityId(
        buildString {
            append("reactor-turn-")
            append(turnIndex)
            append("-merge-")
            append(mergeOrdinal)
            append('-')
            append(consumedEntityIds.joinToString("+") { it.value })
            append('-')
            append(specification.formula)
        },
    )
}
