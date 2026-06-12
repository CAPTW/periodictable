package com.chemtable.interactive.feature.minigame.dex

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.feature.minigame.MoleculeElementLinkResolver
import com.chemtable.interactive.feature.minigame.MoleculeGlossaryLinkResolver
import com.chemtable.interactive.feature.minigame.model.MoleculeElementLink
import com.chemtable.interactive.feature.minigame.model.MoleculeGlossaryLink

data class MoleculeDexUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val highScore: Int? = null,
    val discoveries: List<MoleculeDexItem> = emptyList(),
    val recentSessions: List<MoleculeDexSessionItem> = emptyList(),
) {
    val discoveredCount: Int = discoveries.size
    val isEmpty: Boolean = !isLoading && errorMessage == null && discoveries.isEmpty()
}

data class MoleculeDexItem(
    val formula: String,
    val discoveryCount: Int,
    val firstDiscoveredAt: Long,
    val lastDiscoveredAt: Long,
    val elementLinks: List<MoleculeElementLink>,
    val glossaryLinks: List<MoleculeGlossaryLink>,
)

data class MoleculeDexSessionItem(
    val id: Long,
    val score: Int,
    val success: Boolean,
    val difficulty: String,
    val missionFormula: String?,
    val missionTargetCount: Int?,
    val playedAt: Long,
    val moleculesMade: List<String>,
)

internal fun buildMoleculeDexUiState(
    discoveries: List<GameMoleculeDiscovery>,
    highScore: Int?,
    recentSessions: List<GameSession>,
    elements: List<Element>,
    glossaryTerms: List<GlossaryTerm>,
    elementLinkResolver: MoleculeElementLinkResolver,
    glossaryLinkResolver: MoleculeGlossaryLinkResolver,
): MoleculeDexUiState = MoleculeDexUiState(
    isLoading = false,
    highScore = highScore,
    discoveries = discoveries.map { discovery ->
        MoleculeDexItem(
            formula = discovery.formula,
            discoveryCount = discovery.discoveryCount,
            firstDiscoveredAt = discovery.firstDiscoveredAt,
            lastDiscoveredAt = discovery.lastDiscoveredAt,
            elementLinks = elementLinkResolver.linksForFormula(discovery.formula, elements),
            glossaryLinks = glossaryLinkResolver.linksForFormula(discovery.formula, glossaryTerms),
        )
    },
    recentSessions = recentSessions.map { session ->
        MoleculeDexSessionItem(
            id = session.id,
            score = session.score,
            success = session.success,
            difficulty = session.difficulty,
            missionFormula = session.missionFormula,
            missionTargetCount = session.missionTargetCount,
            playedAt = session.playedAt,
            moleculesMade = session.moleculesMade,
        )
    },
)
