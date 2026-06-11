package com.chemtable.interactive.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.component.ChemSearchBar
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementCategory
import com.chemtable.interactive.core.model.IsotopeSearchResult
import com.chemtable.interactive.core.model.IsotopeStabilityFilter

@Composable
fun SearchScreen(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onElementClick: (Int) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.queryFlow.collectAsState()
    val elementResults by viewModel.results.collectAsState()
    val isotopeResults by viewModel.isotopeResults.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val electronegativityRange by viewModel.electronegativityRange.collectAsState()
    val atomicRadiusRange by viewModel.atomicRadiusRange.collectAsState()
    val isElectroEnabled by viewModel.isElectronegativityEnabled.collectAsState()
    val isRadiusEnabled by viewModel.isAtomicRadiusEnabled.collectAsState()
    val molarMassRange by viewModel.molarMassRange.collectAsState()
    val isMolarMassEnabled by viewModel.isMolarMassEnabled.collectAsState()
    val thermalConductivityRange by viewModel.thermalConductivityRange.collectAsState()
    val isThermalConductivityEnabled by viewModel.isThermalConductivityEnabled.collectAsState()
    val matchAll by viewModel.matchAll.collectAsState()
    val isotopeStabilityFilter by viewModel.isotopeStabilityFilter.collectAsState()
    val isotopeDecayModeQuery by viewModel.isotopeDecayModeQuery.collectAsState()
    val isotopeHalfLifeMinYears by viewModel.isotopeHalfLifeMinYears.collectAsState()
    val isotopeHalfLifeMaxYears by viewModel.isotopeHalfLifeMaxYears.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ChemSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onClear = viewModel::clearQuery,
                onSearch = { viewModel.onQueryChange(query.trim()) }
            )
        }

        item {
            SearchFilterPanel(
                searchMode = searchMode,
                onSearchModeChange = viewModel::onSearchModeChanged,
                sortOption = sortOption,
                onSortOptionChange = viewModel::onSortOptionChange,
                selectedCategories = selectedCategories,
                onToggleCategory = viewModel::toggleCategory,
                electronegativityRange = electronegativityRange,
                onElectronegativityChange = viewModel::onElectronegativityRangeChange,
                isElectronegativityEnabled = isElectroEnabled,
                onElectronegativityEnabledChange = viewModel::setElectronegativityEnabled,
                atomicRadiusRange = atomicRadiusRange,
                onAtomicRadiusChange = viewModel::onAtomicRadiusRangeChange,
                isAtomicRadiusEnabled = isRadiusEnabled,
                onAtomicRadiusEnabledChange = viewModel::setAtomicRadiusEnabled,
                molarMassRange = molarMassRange,
                onMolarMassRangeChange = viewModel::onMolarMassRangeChange,
                isMolarMassEnabled = isMolarMassEnabled,
                onMolarMassEnabledChange = viewModel::setMolarMassEnabled,
                thermalConductivityRange = thermalConductivityRange,
                onThermalConductivityRangeChange = viewModel::onThermalConductivityRangeChange,
                isThermalConductivityEnabled = isThermalConductivityEnabled,
                onThermalConductivityEnabledChange = viewModel::setThermalConductivityEnabled,
                isMatchAll = matchAll,
                onMatchAllChange = viewModel::onMatchAllChange,
                onClearFilters = viewModel::clearFilters,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            IsotopeFilterPanel(
                stabilityFilter = isotopeStabilityFilter,
                onStabilityFilterChange = viewModel::onIsotopeStabilityFilterChange,
                decayModeQuery = isotopeDecayModeQuery,
                onDecayModeQueryChange = viewModel::onIsotopeDecayModeQueryChange,
                halfLifeMinYears = isotopeHalfLifeMinYears,
                onHalfLifeMinYearsChange = viewModel::onIsotopeHalfLifeMinYearsChange,
                halfLifeMaxYears = isotopeHalfLifeMaxYears,
                onHalfLifeMaxYearsChange = viewModel::onIsotopeHalfLifeMaxYearsChange
            )
        }

        item {
            Text("원소 검색 결과 ${elementResults.size}건", style = MaterialTheme.typography.titleMedium)
        }

        items(elementResults) { element ->
            SearchResultCard(
                element = element,
                onClick = { onElementClick(element.atomicNumber) }
            )
        }

        item {
            Text("동위원소 검색 결과 ${isotopeResults.size}건", style = MaterialTheme.typography.titleMedium)
        }

        items(isotopeResults) { result ->
            IsotopeResultCard(
                result = result,
                onClick = { onElementClick(result.isotope.atomicNumber) }
            )
        }
    }
}

@Composable
private fun IsotopeFilterPanel(
    stabilityFilter: IsotopeStabilityFilter,
    onStabilityFilterChange: (IsotopeStabilityFilter) -> Unit,
    decayModeQuery: String,
    onDecayModeQueryChange: (String) -> Unit,
    halfLifeMinYears: String,
    onHalfLifeMinYearsChange: (String) -> Unit,
    halfLifeMaxYears: String,
    onHalfLifeMaxYearsChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("동위원소 필터", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = stabilityFilter == IsotopeStabilityFilter.ALL,
                onClick = { onStabilityFilterChange(IsotopeStabilityFilter.ALL) },
                label = { Text("전체") }
            )
            FilterChip(
                selected = stabilityFilter == IsotopeStabilityFilter.STABLE,
                onClick = { onStabilityFilterChange(IsotopeStabilityFilter.STABLE) },
                label = { Text("안정") }
            )
            FilterChip(
                selected = stabilityFilter == IsotopeStabilityFilter.RADIOACTIVE,
                onClick = { onStabilityFilterChange(IsotopeStabilityFilter.RADIOACTIVE) },
                label = { Text("방사성") }
            )
        }
        OutlinedTextField(
            value = decayModeQuery,
            onValueChange = onDecayModeQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("붕괴 모드 (예: beta, alpha)") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = halfLifeMinYears,
                onValueChange = onHalfLifeMinYearsChange,
                label = { Text("최소 반감기(년)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = halfLifeMaxYears,
                onValueChange = onHalfLifeMaxYearsChange,
                label = { Text("최대 반감기(년)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    element: Element,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${element.nameKo} (${element.symbol})", style = MaterialTheme.typography.titleSmall)
            Text(
                "#${element.atomicNumber} · ${element.category}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("상태: ${element.stateOfMatter}", style = MaterialTheme.typography.bodySmall)
            Text(
                "원자량: ${"%.3f".format(element.molarMass)} g/mol",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider()
            Text("카테고리: ${element.categoryLabel()}", style = MaterialTheme.typography.bodySmall)
            PropertyMeter(
                label = "전기음성도",
                value = element.electronegativity,
                max = 4.0
            )
            PropertyMeter(
                label = "열전도도",
                value = element.thermalConductivity,
                max = 500.0
            )
        }
    }
}

@Composable
private fun IsotopeResultCard(
    result: IsotopeSearchResult,
    onClick: () -> Unit
) {
    val isotope = result.isotope
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${isotope.symbol} · ${result.elementNameKo} (${result.elementSymbol})",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "원자번호 ${isotope.atomicNumber} · 질량수 ${isotope.massNumber} · 중성자 ${isotope.neutronCount}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "분류: ${if (isotope.isStable) "안정" else "방사성"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "반감기: ${isotope.halfLife ?: if (isotope.isStable) "Stable" else "N/A"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "붕괴 모드: ${isotope.decayMode ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PropertyMeter(
    label: String,
    value: Double?,
    max: Double
) {
    val safeMax = if (max <= 0.0) 1.0 else max
    val progress = ((value ?: 0.0) / safeMax).coerceIn(0.0, 1.0).toFloat()
    Text(
        "$label: ${value?.let { "%.2f".format(it) } ?: "N/A"}",
        style = MaterialTheme.typography.labelSmall
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun Element.categoryLabel(): String = when (this.category) {
    ElementCategory.ALKALI_METAL -> "알칼리금속"
    ElementCategory.ALKALINE_EARTH -> "알칼리토금속"
    ElementCategory.TRANSITION_METAL -> "전이금속"
    ElementCategory.POST_TRANSITION_METAL -> "후전이금속"
    ElementCategory.METALLOID -> "준금속"
    ElementCategory.NONMETAL -> "비금속"
    ElementCategory.HALOGEN -> "할로겐"
    ElementCategory.NOBLE_GAS -> "비활성기체"
    ElementCategory.LANTHANIDE -> "란탄족"
    ElementCategory.ACTINIDE -> "악티늄"
    ElementCategory.UNKNOWN -> "기타"
}
