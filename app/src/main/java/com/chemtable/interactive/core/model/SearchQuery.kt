package com.chemtable.interactive.core.model

sealed interface SearchQuery {
    data class ByName(val text: String) : SearchQuery
    data class BySymbol(val text: String) : SearchQuery
    data class ByNumber(val number: Int) : SearchQuery
    data class ByCategory(val categories: Set<ElementCategory>) : SearchQuery
    data class ByPropertyRange(
        val property: ElementProperty,
        val min: Double,
        val max: Double
    ) : SearchQuery

    data class ByText(val text: String) : SearchQuery

    data class Composite(
        val filters: List<SearchQuery>,
        val matchAll: Boolean = true
    ) : SearchQuery
}
