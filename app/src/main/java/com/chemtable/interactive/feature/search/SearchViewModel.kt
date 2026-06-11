package com.chemtable.interactive.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementCategory
import com.chemtable.interactive.core.model.IsotopeSearchResult
import com.chemtable.interactive.core.model.IsotopeStabilityFilter
import com.chemtable.interactive.core.model.ElementProperty
import com.chemtable.interactive.core.model.SearchQuery
import com.chemtable.interactive.domain.usecase.SearchElementsUseCase
import com.chemtable.interactive.domain.usecase.SearchIsotopesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
    private val searchElementsUseCase: SearchElementsUseCase,
    private val searchIsotopesUseCase: SearchIsotopesUseCase
) : ViewModel() {

    private val _queryFlow = MutableStateFlow("")
    val queryFlow: StateFlow<String> = _queryFlow

    private val _searchMode = MutableStateFlow(SearchMode.All)
    val searchMode: StateFlow<SearchMode> = _searchMode

    private val _selectedCategories = MutableStateFlow<Set<ElementCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<ElementCategory>> = _selectedCategories

    private val _electronegativityRange = MutableStateFlow(0.7f..4.0f)
    val electronegativityRange: StateFlow<ClosedFloatingPointRange<Float>> = _electronegativityRange

    private val _isElectronegativityEnabled = MutableStateFlow(false)
    val isElectronegativityEnabled: StateFlow<Boolean> = _isElectronegativityEnabled

    private val _atomicRadiusRange = MutableStateFlow(30f..300f)
    val atomicRadiusRange: StateFlow<ClosedFloatingPointRange<Float>> = _atomicRadiusRange

    private val _isAtomicRadiusEnabled = MutableStateFlow(false)
    val isAtomicRadiusEnabled: StateFlow<Boolean> = _isAtomicRadiusEnabled

    private val _molarMassRange = MutableStateFlow(1f..300f)
    val molarMassRange: StateFlow<ClosedFloatingPointRange<Float>> = _molarMassRange

    private val _isMolarMassEnabled = MutableStateFlow(false)
    val isMolarMassEnabled: StateFlow<Boolean> = _isMolarMassEnabled

    private val _thermalConductivityRange = MutableStateFlow(0f..500f)
    val thermalConductivityRange: StateFlow<ClosedFloatingPointRange<Float>> = _thermalConductivityRange

    private val _isThermalConductivityEnabled = MutableStateFlow(false)
    val isThermalConductivityEnabled: StateFlow<Boolean> = _isThermalConductivityEnabled

    private val _matchAll = MutableStateFlow(true)
    val matchAll: StateFlow<Boolean> = _matchAll

    private val _sortOption = MutableStateFlow(SearchSortOption.AtomicNumberAsc)
    val sortOption: StateFlow<SearchSortOption> = _sortOption

    private val _isotopeStabilityFilter = MutableStateFlow(IsotopeStabilityFilter.ALL)
    val isotopeStabilityFilter: StateFlow<IsotopeStabilityFilter> = _isotopeStabilityFilter

    private val _isotopeDecayModeQuery = MutableStateFlow("")
    val isotopeDecayModeQuery: StateFlow<String> = _isotopeDecayModeQuery

    private val _isotopeHalfLifeMinYears = MutableStateFlow("")
    val isotopeHalfLifeMinYears: StateFlow<String> = _isotopeHalfLifeMinYears

    private val _isotopeHalfLifeMaxYears = MutableStateFlow("")
    val isotopeHalfLifeMaxYears: StateFlow<String> = _isotopeHalfLifeMaxYears

    @Suppress("UNCHECKED_CAST")
    val results: StateFlow<List<Element>> = combine(
        _queryFlow,
        _searchMode,
        _selectedCategories,
        _electronegativityRange,
        _isElectronegativityEnabled,
        _atomicRadiusRange,
        _isAtomicRadiusEnabled,
        _molarMassRange,
        _isMolarMassEnabled,
        _thermalConductivityRange,
        _isThermalConductivityEnabled,
        _matchAll,
        _sortOption
    ) { values ->
        val query = values[0] as String
        val searchMode = values[1] as SearchMode
        val selectedCategories = values[2] as Set<ElementCategory>
        val electronegativityRange = values[3] as ClosedFloatingPointRange<Float>
        val isElectronegativityEnabled = values[4] as Boolean
        val atomicRadiusRange = values[5] as ClosedFloatingPointRange<Float>
        val isAtomicRadiusEnabled = values[6] as Boolean
        val molarMassRange = values[7] as ClosedFloatingPointRange<Float>
        val isMolarMassEnabled = values[8] as Boolean
        val thermalConductivityRange = values[9] as ClosedFloatingPointRange<Float>
        val isThermalConductivityEnabled = values[10] as Boolean
        val matchAll = values[11] as Boolean
        val sortOption = values[12] as SearchSortOption

        val filters = mutableListOf<SearchQuery>()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotBlank()) {
            val textQuery = when (searchMode) {
                SearchMode.All -> SearchQuery.ByText(trimmedQuery)
                SearchMode.Name -> SearchQuery.ByName(trimmedQuery)
                SearchMode.Symbol -> SearchQuery.BySymbol(trimmedQuery)
                SearchMode.Number -> SearchQuery.ByNumber(trimmedQuery.toIntOrNull() ?: -1)
            }
            filters.add(textQuery)
        }

        val categories = selectedCategories
        if (categories.isNotEmpty()) {
            filters.add(SearchQuery.ByCategory(categories))
        }
        if (isElectronegativityEnabled) {
            filters.add(
                SearchQuery.ByPropertyRange(
                    property = ElementProperty.ELECTRONEGATIVITY,
                    min = electronegativityRange.start.toDouble(),
                    max = electronegativityRange.endInclusive.toDouble()
                )
            )
        }
        if (isAtomicRadiusEnabled) {
            filters.add(
                SearchQuery.ByPropertyRange(
                    property = ElementProperty.ATOMIC_RADIUS,
                    min = atomicRadiusRange.start.toDouble(),
                    max = atomicRadiusRange.endInclusive.toDouble()
                )
            )
        }
        if (isMolarMassEnabled) {
            filters.add(
                SearchQuery.ByPropertyRange(
                    property = ElementProperty.MOLAR_MASS,
                    min = molarMassRange.start.toDouble(),
                    max = molarMassRange.endInclusive.toDouble()
                )
            )
        }
        if (isThermalConductivityEnabled) {
            filters.add(
                SearchQuery.ByPropertyRange(
                    property = ElementProperty.THERMAL_CONDUCTIVITY,
                    min = thermalConductivityRange.start.toDouble(),
                    max = thermalConductivityRange.endInclusive.toDouble()
                )
            )
        }
        SearchRequest(
            query = SearchQuery.Composite(filters = filters, matchAll = matchAll),
            sortOption = sortOption
        )
    }.flatMapLatest { request ->
        searchElementsUseCase(request.query).map { elements ->
            sortElements(elements, request.sortOption)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val isotopeResults: StateFlow<List<IsotopeSearchResult>> = combine(
        _queryFlow,
        _isotopeStabilityFilter,
        _isotopeDecayModeQuery,
        _isotopeHalfLifeMinYears,
        _isotopeHalfLifeMaxYears
    ) { query, stability, decayMode, minYearsText, maxYearsText ->
        IsotopeSearchRequest(
            query = query,
            stabilityFilter = stability,
            decayModeQuery = decayMode,
            minHalfLifeSeconds = yearsToSecondsOrNull(minYearsText),
            maxHalfLifeSeconds = yearsToSecondsOrNull(maxYearsText)
        )
    }.flatMapLatest { request ->
        searchIsotopesUseCase(
            query = request.query,
            stabilityFilter = request.stabilityFilter,
            decayModeQuery = request.decayModeQuery,
            minHalfLifeSeconds = request.minHalfLifeSeconds,
            maxHalfLifeSeconds = request.maxHalfLifeSeconds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onQueryChange(query: String) {
        _queryFlow.value = query
    }

    fun clearQuery() {
        _queryFlow.value = ""
    }

    fun onSearchModeChanged(mode: SearchMode) {
        _searchMode.value = mode
    }

    fun toggleCategory(category: ElementCategory) {
        _selectedCategories.value = _selectedCategories.value.toMutableSet().also {
            if (it.contains(category)) it.remove(category) else it.add(category)
        }
    }

    fun onElectronegativityRangeChange(range: ClosedFloatingPointRange<Float>) {
        _electronegativityRange.value = range
    }

    fun setElectronegativityEnabled(enabled: Boolean) {
        _isElectronegativityEnabled.value = enabled
    }

    fun onAtomicRadiusRangeChange(range: ClosedFloatingPointRange<Float>) {
        _atomicRadiusRange.value = range
    }

    fun setAtomicRadiusEnabled(enabled: Boolean) {
        _isAtomicRadiusEnabled.value = enabled
    }

    fun onMolarMassRangeChange(range: ClosedFloatingPointRange<Float>) {
        _molarMassRange.value = range
    }

    fun setMolarMassEnabled(enabled: Boolean) {
        _isMolarMassEnabled.value = enabled
    }

    fun onThermalConductivityRangeChange(range: ClosedFloatingPointRange<Float>) {
        _thermalConductivityRange.value = range
    }

    fun setThermalConductivityEnabled(enabled: Boolean) {
        _isThermalConductivityEnabled.value = enabled
    }

    fun onMatchAllChange(isMatchAll: Boolean) {
        _matchAll.value = isMatchAll
    }

    fun onSortOptionChange(option: SearchSortOption) {
        _sortOption.value = option
    }

    fun onIsotopeStabilityFilterChange(filter: IsotopeStabilityFilter) {
        _isotopeStabilityFilter.value = filter
    }

    fun onIsotopeDecayModeQueryChange(query: String) {
        _isotopeDecayModeQuery.value = query
    }

    fun onIsotopeHalfLifeMinYearsChange(value: String) {
        _isotopeHalfLifeMinYears.value = value
    }

    fun onIsotopeHalfLifeMaxYearsChange(value: String) {
        _isotopeHalfLifeMaxYears.value = value
    }

    fun clearFilters() {
        _selectedCategories.value = emptySet()
        _isElectronegativityEnabled.value = false
        _isAtomicRadiusEnabled.value = false
        _isMolarMassEnabled.value = false
        _isThermalConductivityEnabled.value = false
        _electronegativityRange.value = 0.7f..4.0f
        _atomicRadiusRange.value = 30f..300f
        _molarMassRange.value = 1f..300f
        _thermalConductivityRange.value = 0f..500f
        _matchAll.value = true
        _sortOption.value = SearchSortOption.AtomicNumberAsc
        _isotopeStabilityFilter.value = IsotopeStabilityFilter.ALL
        _isotopeDecayModeQuery.value = ""
        _isotopeHalfLifeMinYears.value = ""
        _isotopeHalfLifeMaxYears.value = ""
    }

    private fun sortElements(elements: List<Element>, option: SearchSortOption): List<Element> = when (option) {
        SearchSortOption.AtomicNumberAsc -> elements.sortedBy { it.atomicNumber }
        SearchSortOption.AtomicNumberDesc -> elements.sortedByDescending { it.atomicNumber }
        SearchSortOption.MolarMassAsc -> elements.sortedBy { it.molarMass }
        SearchSortOption.MolarMassDesc -> elements.sortedByDescending { it.molarMass }
        SearchSortOption.ElectronegativityDesc -> elements.sortedWith(
            compareByDescending<Element> { it.electronegativity ?: Double.NEGATIVE_INFINITY }
                .thenBy { it.atomicNumber }
        )
        SearchSortOption.ThermalConductivityDesc -> elements.sortedWith(
            compareByDescending<Element> { it.thermalConductivity ?: Double.NEGATIVE_INFINITY }
                .thenBy { it.atomicNumber }
        )
    }

    private data class SearchRequest(
        val query: SearchQuery,
        val sortOption: SearchSortOption
    )

    private data class IsotopeSearchRequest(
        val query: String,
        val stabilityFilter: IsotopeStabilityFilter,
        val decayModeQuery: String,
        val minHalfLifeSeconds: Double?,
        val maxHalfLifeSeconds: Double?
    )

    private fun yearsToSecondsOrNull(value: String): Double? {
        val years = value.trim().toDoubleOrNull() ?: return null
        if (years < 0) return null
        return years * 31_557_600.0
    }
}
