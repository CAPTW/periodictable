package com.chemtable.interactive.feature.minigame.reactor.model

/** Fictional prototype substrates; these are not chemical symbols or enzyme families. */
enum class ReactorSubstrate { A, B, SYNTHETIC }

data class ReactorPolymerEntity(
    override val id: ReactorEntityId,
    val substrate: ReactorSubstrate,
    val units: Int,
) : ReactorEntity {
    init { require(units in 1..4) }
    override val kind = ReactorEntityKind.POLYMER_BUNDLE
    override val composition = ReactorComposition.of(mapOf("@${substrate.name}" to units))
    override val visibleLabel = "${if (substrate == ReactorSubstrate.SYNTHETIC) "S" else substrate.name}$units"
    override val displayName = "가상 기질 묶음"
    // Nonphysical sentinel. UI must show N/A rather than present this as a measured mass.
    override val molarMass = 0.0
    override val settlingIndex = 0.0
    override val settlingBehavior = SettlingBehavior.NEUTRAL
}
