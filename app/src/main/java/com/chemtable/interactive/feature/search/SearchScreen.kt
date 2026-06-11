package com.chemtable.interactive.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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

@Composable
fun SearchScreen(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onElementClick: (Int) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.queryFlow.collectAsState()
    val results by viewModel.results.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChemSearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
            onSearch = { viewModel.onQueryChange(query.trim()) }
        )

        SearchFilterPanel(
            searchMode = searchMode,
            onSearchModeChange = viewModel::onSearchModeChanged,
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

        Text("검색 결과 ${results.size}건", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { element ->
                SearchResultCard(element = element)
            }
        }
    }
}

@Composable
private fun SearchResultCard(element: Element) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
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
            Divider()
            Text("카테고리: ${element.categoryLabel()}", style = MaterialTheme.typography.bodySmall)
        }
    }
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
