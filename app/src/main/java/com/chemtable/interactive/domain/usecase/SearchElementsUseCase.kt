package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementProperty
import com.chemtable.interactive.core.model.SearchQuery
import com.chemtable.interactive.domain.repository.ElementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchElementsUseCase @Inject constructor(
    private val repository: ElementRepository,
    private val getElementsUseCase: GetElementsUseCase
) {
    operator fun invoke(query: SearchQuery): Flow<List<Element>> = when (query) {
        is SearchQuery.ByName -> repository.searchElements(query.text)
        is SearchQuery.BySymbol -> repository.searchBySymbol(query.text)
        is SearchQuery.ByNumber -> {
            if (query.number <= 0) {
                flowOf(emptyList())
            } else {
                repository.searchElements(query.number.toString())
            }
        }
        is SearchQuery.ByCategory -> getElementsUseCase().map { elements ->
            elements.filter { element -> element.category in query.categories }
        }
        is SearchQuery.ByText -> {
            val trimmed = query.text.trim()
            if (trimmed.isBlank()) getElementsUseCase() else repository.searchElements(trimmed)
        }
        is SearchQuery.ByPropertyRange -> repository.filterByProperty(query.property, query.min, query.max)
        is SearchQuery.Composite -> {
            if (query.filters.isEmpty()) {
                getElementsUseCase()
            } else {
                getElementsUseCase().map { elements ->
                    elements.filter { element ->
                        if (query.matchAll) {
                            query.filters.all { it.matches(element) }
                        } else {
                            query.filters.any { it.matches(element) }
                        }
                    }
                }
            }
        }
    }
}

private fun SearchQuery.matches(element: Element): Boolean = when (this) {
    is SearchQuery.ByName -> {
        val search = text.trim().lowercase()
        if (search.isBlank()) true else (
            element.name.contains(search, ignoreCase = true) ||
                element.nameKo.contains(search, ignoreCase = true)
        )
    }
    is SearchQuery.BySymbol -> {
        val search = text.trim().lowercase()
        if (search.isBlank()) true else element.symbol.lowercase().startsWith(search)
    }
    is SearchQuery.ByNumber -> element.atomicNumber == number
    is SearchQuery.ByPropertyRange -> matchesProperty(
        element = element,
        property = property,
        min = min,
        max = max
    )
    is SearchQuery.ByCategory -> {
        if (categories.isEmpty()) true else element.category in categories
    }
    is SearchQuery.ByText -> {
        val search = text.trim().lowercase()
        if (search.isBlank()) true else {
            element.name.contains(search, ignoreCase = true) ||
                element.nameKo.contains(search, ignoreCase = true) ||
                element.symbol.contains(search, ignoreCase = true) ||
                element.atomicNumber.toString().contains(search)
        }
    }
    is SearchQuery.Composite -> {
        if (filters.isEmpty()) true else {
            if (matchAll) filters.all { it.matches(element) } else filters.any { it.matches(element) }
        }
    }
}

private fun SearchQuery.matchesProperty(element: Element, property: ElementProperty, min: Double, max: Double): Boolean {
    val value = when (property) {
        ElementProperty.ELECTRONEGATIVITY -> element.electronegativity
        ElementProperty.ATOMIC_RADIUS -> element.atomicRadius
        ElementProperty.MOLAR_MASS -> element.molarMass
        ElementProperty.THERMAL_CONDUCTIVITY -> element.thermalConductivity
    }
    return value?.let { it >= min && it <= max } ?: false
}
