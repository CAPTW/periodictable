package com.chemtable.interactive.feature.visualization

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementProperty
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class VisualMode {
    HEAT_MAP,
    BAR_CHART,
    TREND_LINE,
    COMPARE_RADAR
}

data class HeatMapCell(
    val element: Element,
    val value: Double?,
    val normalized: Float
)

data class ChartItem(
    val symbol: String,
    val value: Double,
    val valueText: String,
    val atomicNumber: Int
)

@HiltViewModel
class VisualizationViewModel @Inject constructor(
    getElementsUseCase: GetElementsUseCase
) : ViewModel() {

    val selectedProperty = kotlinx.coroutines.flow.MutableStateFlow(ElementProperty.ELECTRONEGATIVITY)
    val selectedMode = kotlinx.coroutines.flow.MutableStateFlow(VisualMode.HEAT_MAP)
    private val _compareSymbols =
        kotlinx.coroutines.flow.MutableStateFlow(listOf("H", "C", "O", "Fe"))
    val compareSymbols = _compareSymbols.asStateFlow()

    val elements = getElementsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val heatMapData: StateFlow<List<HeatMapCell>> = combine(
        elements,
        selectedProperty
    ) { all, property ->
        val values = all.mapNotNull { it.getProperty(property) }
        all.map { element ->
            val value = element.getProperty(property)
            HeatMapCell(
                element = element,
                value = value,
                normalized = normalizeElementValue(value, values)
            )
        }
    }.map { cells ->
        cells.sortedWith(compareBy({ it.element.period }, { it.element.group }, { it.element.atomicNumber }))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val chartItems: StateFlow<List<ChartItem>> = combine(
        elements,
        selectedProperty
    ) { all, property ->
        all
            .mapNotNull { element ->
                val value = element.getProperty(property) ?: return@mapNotNull null
                ChartItem(
                    symbol = element.symbol,
                    value = value,
                    valueText = "%.3f".format(value),
                    atomicNumber = element.atomicNumber
                )
            }
            .sortedWith(compareBy<ChartItem> { it.atomicNumber })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val compareData: StateFlow<List<ChartItem>> = combine(
        elements,
        compareSymbols,
        selectedProperty
    ) { all, symbols, property ->
        val atomicNumberBySymbol = all.associate { it.symbol to it.atomicNumber }
        val values = all.mapNotNull { element ->
            val value = element.getProperty(property) ?: return@mapNotNull null
            element.symbol to value
        }.toMap()
        symbols.mapNotNull { symbol ->
            values[symbol]?.let { value ->
                ChartItem(
                    symbol = symbol,
                    value = value,
                    valueText = "%.3f".format(value),
                    atomicNumber = atomicNumberBySymbol[symbol] ?: 0
                )
            }
        }
            .sortedBy { it.atomicNumber }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val availableCompareSymbols: StateFlow<List<String>> = elements.map { list ->
        list.map { it.symbol }.take(40)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun onPropertyChange(property: ElementProperty) {
        selectedProperty.value = property
    }

    fun onModeChange(mode: VisualMode) {
        selectedMode.value = mode
    }

    fun toggleCompare(symbol: String) {
        val current = _compareSymbols.value
        val max = 4
        _compareSymbols.value = if (current.contains(symbol)) {
            current.filterNot { it == symbol }
        } else {
            if (current.size >= max) {
                current.drop(1) + symbol
            } else {
                current + symbol
            }
        }
    }

    private fun normalizeElementValue(
        value: Double?,
        allValues: List<Double>
    ): Float {
        if (value == null || allValues.isEmpty()) return 0f
        val min = allValues.minOrNull() ?: 0.0
        val max = allValues.maxOrNull() ?: 1.0
        if (max == min) return 0.5f
        val normalized = ((value - min) / (max - min)).toFloat()
        return normalized.coerceIn(0f, 1f)
    }
}

private fun Element.getProperty(property: ElementProperty): Double? = when (property) {
    ElementProperty.ELECTRONEGATIVITY -> electronegativity
    ElementProperty.ATOMIC_RADIUS -> atomicRadius
    ElementProperty.MOLAR_MASS -> molarMass
    ElementProperty.THERMAL_CONDUCTIVITY -> thermalConductivity
}

private val HeatColorStart = Color(0xFF1E88E5)
private val HeatColorMid = Color(0xFFFFEB3B)
private val HeatColorEnd = Color(0xFFE53935)

fun colorForNormalizedValue(normalized: Float): Color {
    return when {
        normalized < 0.5f -> {
            val ratio = normalized / 0.5f
            HeatColorStart.interpolate(HeatColorMid, ratio)
        }
        else -> {
            val ratio = (normalized - 0.5f) / 0.5f
            HeatColorMid.interpolate(HeatColorEnd, ratio)
        }
    }
}

private fun Color.interpolate(target: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = red + (target.red - red) * f,
        green = green + (target.green - green) * f,
        blue = blue + (target.blue - blue) * f,
        alpha = alpha + (target.alpha - alpha) * f
    )
}
