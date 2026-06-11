package com.chemtable.interactive.feature.minigame.model

/**
 * Result overlay link from a generated molecule formula to an existing glossary term.
 */
data class MoleculeGlossaryLink(
    val termId: String,
    val label: String,
)
