package com.chemtable.interactive.feature.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.ElementProperty
import kotlin.math.max
import kotlin.math.min

@Composable
fun VisualizationScreen(
    innerPadding: PaddingValues,
    onPlayMiniGame: () -> Unit = {},
    onOpenMoleculeDex: () -> Unit = {},
    viewModel: VisualizationViewModel = hiltViewModel()
) {
    val mode by viewModel.selectedMode.collectAsState()
    val property by viewModel.selectedProperty.collectAsState()
    val heatMapData by viewModel.heatMapData.collectAsState()
    val chartItems by viewModel.chartItems.collectAsState()
    val compareData by viewModel.compareData.collectAsState()
    val compareSymbols by viewModel.compareSymbols.collectAsState()
    val compareCandidates by viewModel.availableCompareSymbols.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding)
    ) {
        Text("속성 시각화", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))
        Card(
            onClick = onPlayMiniGame,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("분자 만들기", style = MaterialTheme.typography.titleMedium)
                Text(
                    "원소를 모아 분자를 합성하는 미니게임 · 탭하여 플레이",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            onClick = onOpenMoleculeDex,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "분자 도감 열기" },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("분자 도감", style = MaterialTheme.typography.titleMedium)
                Text(
                    "게임에서 발견한 분자와 최고점수를 확인하세요",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (item in VisualMode.values()) {
                FilterChip(
                    selected = item == mode,
                    onClick = { viewModel.onModeChange(item) },
                    label = { Text(visualModeLabel(item)) }
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (item in ElementProperty.values()) {
                FilterChip(
                    selected = item == property,
                    onClick = { viewModel.onPropertyChange(item) },
                    label = { Text(propertyLabel(item)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (mode == VisualMode.COMPARE_RADAR && compareCandidates.isNotEmpty()) {
            Text("비교 원소 선택", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (symbol in compareCandidates) {
                    val selected = compareSymbols.contains(symbol)
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleCompare(symbol) },
                        label = { Text(symbol) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when (mode) {
            VisualMode.HEAT_MAP -> HeatMapPanel(data = heatMapData)
            VisualMode.BAR_CHART -> BarChartPanel(items = chartItems)
            VisualMode.TREND_LINE -> TrendLinePanel(items = chartItems)
            VisualMode.COMPARE_RADAR -> ComparePanel(items = compareData)
        }
    }
}

@Composable
private fun HeatMapPanel(data: List<HeatMapCell>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(data) { item ->
            val color = colorForNormalizedValue(item.normalized)
            Card(
                modifier = Modifier
                    .width(56.dp)
                    .height(60.dp),
                colors = CardDefaults.cardColors(containerColor = color)
            ) {
                ColumnPanel(
                    label = item.element.symbol,
                    detail = "#${item.element.atomicNumber}",
                    value = item.value?.let { "%.2f".format(it) } ?: "-",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BarChartPanel(items: List<ChartItem>) {
    if (items.isEmpty()) {
        Text("표시할 데이터가 없습니다.")
        return
    }
    val maxValue = items.maxOfOrNull { it.value ?: 0.0 }?.takeIf { it > 0.0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (item in items) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.symbol,
                    modifier = Modifier.width(44.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .background(Color(0xFFECEFF1))
                ) {
                    val valAsFloat = (item.value ?: 0.0).toFloat()
                    val ratio = min(1f, max(0f, (valAsFloat / maxValue).toFloat()))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color(0xFF1565C0),
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(
                                width = size.width * ratio,
                                height = size.height
                            )
                        )
                    }
                }
                Text(
                    text = item.valueText,
                    modifier = Modifier.width(72.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TrendLinePanel(items: List<ChartItem>) {
    val values = items.map { (it.value ?: 0.0).toFloat() }
    if (values.isEmpty()) {
        Text("표시할 데이터가 없습니다.")
        return
    }
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 1f

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Atomic number trend")
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val widthStep = if (items.size > 1) size.width / (items.size - 1) else 0f
            val normalizedValues = values.map {
                val denom = if (max == min) 1f else (max - min)
                (it - min) / denom
            }
            for (index in 0 until normalizedValues.lastIndex) {
                val x1 = index * widthStep
                val y1 = size.height - (size.height * normalizedValues[index])
                val x2 = (index + 1) * widthStep
                val y2 = size.height - (size.height * normalizedValues[index + 1])
                drawLine(
                    color = Color(0xFF2E7D32),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 4f
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            for (item in items) {
                Text(
                    text = "${item.symbol}(${item.atomicNumber}) ${item.valueText}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparePanel(items: List<ChartItem>) {
    if (items.isEmpty()) {
        Text("비교할 원소를 먼저 추가하세요.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (item in items) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
            ) {
                ColumnPanel(
                    label = "${item.symbol} (#${item.atomicNumber})",
                    detail = item.valueText,
                    value = "값",
                    color = Color(0xFF006064)
                )
            }
        }
    }
}

@Composable
private fun ColumnPanel(
    label: String,
    detail: String,
    value: String,
    color: Color
) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = color)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.85f))
        Text(value, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun visualModeLabel(mode: VisualMode): String = when (mode) {
    VisualMode.HEAT_MAP -> "Heat Map"
    VisualMode.BAR_CHART -> "Bar Chart"
    VisualMode.TREND_LINE -> "Trend Line"
    VisualMode.COMPARE_RADAR -> "Compare"
}

private fun propertyLabel(property: ElementProperty): String = when (property) {
    ElementProperty.ELECTRONEGATIVITY -> "Electronegativity"
    ElementProperty.ATOMIC_RADIUS -> "Atomic Radius"
    ElementProperty.MOLAR_MASS -> "Molar Mass"
    ElementProperty.THERMAL_CONDUCTIVITY -> "Thermal Conductivity"
}
