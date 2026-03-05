package com.chemtable.interactive.feature.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.CalcHistory
import com.chemtable.interactive.core.model.FormulaComponentResult
import com.chemtable.interactive.domain.usecase.CalculateMolarMassUseCase
import com.chemtable.interactive.domain.usecase.GetCalcHistoryUseCase
import com.chemtable.interactive.domain.usecase.SaveCalcHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculateMolarMassUseCase: CalculateMolarMassUseCase,
    getCalcHistoryUseCase: GetCalcHistoryUseCase,
    private val saveCalcHistoryUseCase: SaveCalcHistoryUseCase
) : ViewModel() {

    val history: StateFlow<List<CalcHistory>> = getCalcHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _formula = MutableStateFlow("")
    val formula: StateFlow<String> = _formula

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating

    private val _resultText = MutableStateFlow("")
    val resultText: StateFlow<String> = _resultText

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _components = MutableStateFlow<List<FormulaComponentResult>>(emptyList())
    val components: StateFlow<List<FormulaComponentResult>> = _components

    fun onFormulaChanged(text: String) {
        _formula.value = text
        if (text.isBlank()) {
            _error.value = null
            _resultText.value = ""
            _components.value = emptyList()
        }
    }

    fun appendToken(token: String) {
        _formula.value = _formula.value + token.toSubscriptDigits()
        if (_error.value != null) _error.value = null
    }

    fun clearFormula() {
        _formula.value = ""
        _resultText.value = ""
        _components.value = emptyList()
        _error.value = null
    }

    fun deleteLast() {
        if (_formula.value.isNotEmpty()) {
            _formula.value = _formula.value.dropLast(1)
        }
    }

    fun calculate(onCalculated: () -> Unit = {}) {
        val target = _formula.value.trim()
        if (target.isBlank()) {
            _error.value = "수식을 입력하세요."
            return
        }

        viewModelScope.launch {
            _isCalculating.value = true
            _error.value = null
            try {
                val result = calculateMolarMassUseCase(target)
                _resultText.value = result.totalMolarMass.toString()
                _components.value = result.components
                saveCalcHistoryUseCase(
                    CalcHistory(
                        formula = result.formula,
                        result = result.totalMolarMass,
                        components = result.components.map { component ->
                            "${component.symbol}:${component.count}"
                        }
                    )
                )
                onCalculated()
            } catch (exception: Exception) {
                _error.value = exception.message ?: "계산에 실패했습니다."
                _resultText.value = ""
                _components.value = emptyList()
            } finally {
                _isCalculating.value = false
            }
        }
    }
}

private fun String.toSubscriptDigits(): String = this
    .map {
        when (it) {
            '0' -> '₀'
            '1' -> '₁'
            '2' -> '₂'
            '3' -> '₃'
            '4' -> '₄'
            '5' -> '₅'
            '₅' -> '₅'
            '6' -> '₆'
            '7' -> '₇'
            '8' -> '₈'
            '9' -> '₉'
            else -> it
        }
    }
    .joinToString("")
