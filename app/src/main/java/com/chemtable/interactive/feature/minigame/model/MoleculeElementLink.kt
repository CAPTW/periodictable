package com.chemtable.interactive.feature.minigame.model

/**
 * Result overlay link from a generated molecule formula to one of its constituent elements.
 */
data class MoleculeElementLink(
    val symbol: String,
    val count: Int,
    val atomicNumber: Int,
    val displayName: String?,
)

/**
 * Result overlay row model. Formula remains the source for the calculator CTA; elementLinks
 * powers ElementDetail chips.
 */
data class DiscoveredMolecule(
    val formula: String,
    val elementLinks: List<MoleculeElementLink>,
    val glossaryLinks: List<MoleculeGlossaryLink> = emptyList(),
)
