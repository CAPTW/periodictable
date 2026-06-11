package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.core.model.Isotope
import com.chemtable.interactive.core.model.IsotopeSearchResult
import com.chemtable.interactive.core.model.IsotopeStabilityFilter
import com.chemtable.interactive.domain.repository.IsotopeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class SearchIsotopesUseCase @Inject constructor(
    private val isotopeRepository: IsotopeRepository,
    private val getElementsUseCase: GetElementsUseCase
) {
    operator fun invoke(
        query: String,
        stabilityFilter: IsotopeStabilityFilter,
        decayModeQuery: String,
        minHalfLifeSeconds: Double?,
        maxHalfLifeSeconds: Double?
    ): Flow<List<IsotopeSearchResult>> {
        val normalizedQuery = query.trim()
        val normalizedDecay = decayModeQuery.trim()

        return combine(
            isotopeRepository.getAllIsotopes(),
            getElementsUseCase()
        ) { isotopes, elements ->
            val elementMap = elements.associateBy { it.atomicNumber }

            isotopes
                .filter { isotope ->
                    isotope.matchesStability(stabilityFilter) &&
                        isotope.matchesHalfLife(minHalfLifeSeconds, maxHalfLifeSeconds) &&
                        isotope.matchesDecayMode(normalizedDecay)
                }
                .mapNotNull { isotope ->
                    val element = elementMap[isotope.atomicNumber] ?: return@mapNotNull null
                    IsotopeSearchResult(
                        isotope = isotope,
                        elementSymbol = element.symbol,
                        elementNameKo = element.nameKo,
                        elementNameEn = element.name
                    )
                }
                .filter { result ->
                    if (normalizedQuery.isBlank()) return@filter true
                    result.matchesText(normalizedQuery)
                }
                .sortedWith(
                    compareBy<IsotopeSearchResult> { it.isotope.atomicNumber }
                        .thenByDescending { it.isotope.isStable }
                        .thenBy { it.isotope.massNumber }
                )
        }
    }
}

private fun Isotope.matchesStability(filter: IsotopeStabilityFilter): Boolean = when (filter) {
    IsotopeStabilityFilter.ALL -> true
    IsotopeStabilityFilter.STABLE -> isStable
    IsotopeStabilityFilter.RADIOACTIVE -> !isStable
}

private fun Isotope.matchesHalfLife(min: Double?, max: Double?): Boolean {
    if (min == null && max == null) return true
    if (isStable) return false
    val seconds = halfLifeSeconds ?: return false
    val meetsMin = min?.let { seconds >= it } ?: true
    val meetsMax = max?.let { seconds <= it } ?: true
    return meetsMin && meetsMax
}

private fun Isotope.matchesDecayMode(query: String): Boolean {
    if (query.isBlank()) return true
    return decayMode?.contains(query, ignoreCase = true) == true
}

private fun IsotopeSearchResult.matchesText(query: String): Boolean {
    val normalized = query.lowercase()
    return isotope.symbol.lowercase().contains(normalized) ||
        isotope.massNumber.toString().contains(normalized) ||
        isotope.atomicNumber.toString().contains(normalized) ||
        elementSymbol.lowercase().contains(normalized) ||
        elementNameKo.lowercase().contains(normalized) ||
        elementNameEn.lowercase().contains(normalized) ||
        isotope.decayMode?.lowercase()?.contains(normalized) == true
}
