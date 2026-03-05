package com.chemtable.interactive.core.model

data class GlossaryTerm(
    val id: String,
    val termKo: String,
    val termEn: String,
    val definition: String,
    val simpleExplanation: String,
    val category: GlossaryCategory,
    val interactiveType: InteractiveType?,
    val relatedElements: List<Int>,
    val relatedTerms: List<String>,
    val isBookmarked: Boolean = false
)

enum class GlossaryCategory {
    ATOMIC_STRUCTURE,
    BONDING,
    REACTIONS,
    THERMODYNAMICS,
    NUCLEAR,
    ORGANIC,
    GENERAL
}

enum class InteractiveType {
    ELECTRON_ANIMATION,
    BOND_VISUALIZATION,
    DECAY_SIMULATION,
    ENERGY_DIAGRAM,
    COMPARISON
}
