package com.chemtable.interactive.feature.minigame.dex

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.model.GameSession
import com.chemtable.interactive.feature.minigame.MoleculeElementLinkResolver
import com.chemtable.interactive.feature.minigame.MoleculeGlossaryLinkResolver
import com.chemtable.interactive.feature.minigame.model.Difficulty
import com.chemtable.interactive.feature.minigame.model.MoleculeElementLink
import com.chemtable.interactive.feature.minigame.model.MoleculeGlossaryLink
import java.util.Locale

data class MoleculeDexUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val highScore: Int? = null,
    val difficultyHighScores: List<MoleculeDexDifficultyScore> = emptyList(),
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
    val difficultyLabel: String,
    val missionFormula: String?,
    val missionTargetCount: Int?,
    val playedAt: Long,
    val moleculesMade: List<String>,
)

data class MoleculeDexDifficultyScore(
    val difficulty: String,
    val label: String,
    val highScore: Int?,
)

internal fun buildMoleculeDexUiState(
    discoveries: List<GameMoleculeDiscovery>,
    highScore: Int?,
    difficultyHighScores: Map<String, Int?> = emptyMap(),
    recentSessions: List<GameSession>,
    elements: List<Element>,
    glossaryTerms: List<GlossaryTerm>,
    elementLinkResolver: MoleculeElementLinkResolver,
    glossaryLinkResolver: MoleculeGlossaryLinkResolver,
): MoleculeDexUiState = MoleculeDexUiState(
    isLoading = false,
    highScore = highScore,
    difficultyHighScores = Difficulty.entries.map { difficulty ->
        MoleculeDexDifficultyScore(
            difficulty = difficulty.name,
            label = difficultyLabelFor(difficulty.name),
            highScore = difficultyHighScores[difficulty.name],
        )
    },
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
            difficultyLabel = difficultyLabelFor(session.difficulty),
            missionFormula = session.missionFormula,
            missionTargetCount = session.missionTargetCount,
            playedAt = session.playedAt,
            moleculesMade = session.moleculesMade,
        )
    },
)

internal fun difficultyLabelFor(difficulty: String): String {
    val normalized = difficulty.trim()
    if (normalized.isBlank()) return "알 수 없음"

    return when (normalized.uppercase(Locale.ROOT)) {
        Difficulty.BEGINNER.name -> "초급"
        Difficulty.INTERMEDIATE.name -> "중급"
        Difficulty.ADVANCED.name -> "고급"
        else -> normalized
    }
}
