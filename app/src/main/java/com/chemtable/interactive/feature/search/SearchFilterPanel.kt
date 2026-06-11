package com.chemtable.interactive.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chemtable.interactive.core.model.ElementCategory

private fun ElementCategory.label(): String = when (this) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFilterPanel(
    searchMode: SearchMode,
    onSearchModeChange: (SearchMode) -> Unit,
    sortOption: SearchSortOption,
    onSortOptionChange: (SearchSortOption) -> Unit,
    selectedCategories: Set<ElementCategory>,
    onToggleCategory: (ElementCategory) -> Unit,
    electronegativityRange: ClosedFloatingPointRange<Float>,
    onElectronegativityChange: (ClosedFloatingPointRange<Float>) -> Unit,
    isElectronegativityEnabled: Boolean,
    onElectronegativityEnabledChange: (Boolean) -> Unit,
    atomicRadiusRange: ClosedFloatingPointRange<Float>,
    onAtomicRadiusChange: (ClosedFloatingPointRange<Float>) -> Unit,
    isAtomicRadiusEnabled: Boolean,
    onAtomicRadiusEnabledChange: (Boolean) -> Unit,
    molarMassRange: ClosedFloatingPointRange<Float>,
    onMolarMassRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    isMolarMassEnabled: Boolean,
    onMolarMassEnabledChange: (Boolean) -> Unit,
    thermalConductivityRange: ClosedFloatingPointRange<Float>,
    onThermalConductivityRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    isThermalConductivityEnabled: Boolean,
    onThermalConductivityEnabledChange: (Boolean) -> Unit,
    isMatchAll: Boolean,
    onMatchAllChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("검색 방식", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (mode in SearchMode.values()) {
                    FilterChip(
                        selected = searchMode == mode,
                        onClick = { onSearchModeChange(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("정렬", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (option in SearchSortOption.values()) {
                    FilterChip(
                        selected = sortOption == option,
                        onClick = { onSortOptionChange(option) },
                        label = { Text(option.label) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("조건 결합", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isMatchAll,
                    onClick = { onMatchAllChange(true) },
                    label = { Text("AND (모두)") }
                )
                FilterChip(
                    selected = !isMatchAll,
                    onClick = { onMatchAllChange(false) },
                    label = { Text("OR (하나라도)") }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("카테고리", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (category in ElementCategory.values()) {
                    FilterChip(
                        selected = selectedCategories.contains(category),
                        onClick = { onToggleCategory(category) },
                        label = { Text(category.label()) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("속성 범위 필터", style = MaterialTheme.typography.titleSmall)
            PropertyRangeFilterRow(
                label = "전기음성도",
                valueRange = 0f..4f,
                value = electronegativityRange,
                onValueChange = onElectronegativityChange,
                isEnabled = isElectronegativityEnabled,
                onEnabledChange = onElectronegativityEnabledChange
            )
            PropertyRangeFilterRow(
                label = "원자 반지름",
                valueRange = 25f..300f,
                value = atomicRadiusRange,
                onValueChange = onAtomicRadiusChange,
                isEnabled = isAtomicRadiusEnabled,
                onEnabledChange = onAtomicRadiusEnabledChange
            )
            PropertyRangeFilterRow(
                label = "몰 질량",
                valueRange = 1f..350f,
                value = molarMassRange,
                onValueChange = onMolarMassRangeChange,
                isEnabled = isMolarMassEnabled,
                onEnabledChange = onMolarMassEnabledChange,
                decimals = 1
            )
            PropertyRangeFilterRow(
                label = "열전도도",
                valueRange = 0f..500f,
                value = thermalConductivityRange,
                onValueChange = onThermalConductivityRangeChange,
                isEnabled = isThermalConductivityEnabled,
                onEnabledChange = onThermalConductivityEnabledChange,
                decimals = 1
            )
        }

        ElevatedButton(
            modifier = Modifier.size(width = 140.dp, height = 36.dp),
            onClick = onClearFilters
        ) {
            Text("필터 초기화")
        }
    }
}

@Composable
private fun PropertyRangeFilterRow(
    label: String,
    valueRange: ClosedFloatingPointRange<Float>,
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 2
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            Text(formatRangeValue(value.start, decimals))
            Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
            Text(formatRangeValue(value.endInclusive, decimals))
        }
        RangeSlider(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 4.dp),
            enabled = isEnabled,
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange
        )
    }
}

private fun formatRangeValue(value: Float, decimals: Int): String = "%.${decimals}f".format(value)
