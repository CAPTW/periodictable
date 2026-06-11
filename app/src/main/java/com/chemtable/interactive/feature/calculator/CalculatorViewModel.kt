package com.chemtable.interactive.feature.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.CalcHistory
import com.chemtable.interactive.core.model.FormulaComponentResult
import com.chemtable.interactive.domain.usecase.CalculateMolarMassUseCase
import com.chemtable.interactive.domain.usecase.GetCalcHistoryUseCase
import com.chemtable.interactive.domain.usecase.SaveCalcHistoryUseCase
import com.chemtable.interactive.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calculateMolarMassUseCase: CalculateMolarMassUseCase,
    getCalcHistoryUseCase: GetCalcHistoryUseCase,
    private val saveCalcHistoryUseCase: SaveCalcHistoryUseCase
) : ViewModel() {

    val history: StateFlow<List<CalcHistory>> = getCalcHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _formula = MutableStateFlow("")
    val formula: StateFlow<String> = _formula

    /**
     * 프리필 식이 들어온 경우 화면이 1회 자동 계산하도록 알리는 one-shot 플래그.
     * 자동 계산은 viewModelScope 코루틴을 쓰므로 init 에서 직접 호출하지 않고
     * 화면(Composable)이 [calculate] 를 호출한 뒤 [onPrefillAutoCalculateHandled] 로 소비한다.
     */
    private val _autoCalculate = MutableStateFlow(false)
    val autoCalculate: StateFlow<Boolean> = _autoCalculate

    init {
        // 네비게이션 인자(formula)가 있으면 입력창에 1회만 프리필한다.
        // Process death 복원 시 중복 프리필 및 사용자 입력 덮어쓰기를 방지하기 위해 prefill_consumed 플래그를 확인한다.
        val isConsumed = savedStateHandle.get<Boolean>("prefill_consumed") ?: false
        if (!isConsumed) {
            val rawFormula = savedStateHandle.get<String>(Screen.Calculator.ARG_FORMULA)
            val decodedFormula = rawFormula?.let { Screen.Calculator.decodeArg(it) }
            val prefill = resolvePrefillFormula(decodedFormula)
            if (prefill != null) {
                _formula.value = prefill
                _autoCalculate.value = true
                savedStateHandle["prefill_consumed"] = true
            }
        }
    }

    /** 프리필 자동 계산을 화면이 소비했음을 표시(중복 계산 방지). */
    fun onPrefillAutoCalculateHandled() {
        _autoCalculate.value = false
    }

    companion object {
        /**
         * 네비게이션 인자를 프리필 대상 식으로 정규화한다.
         * 공백을 제거하고, 비어 있으면 프리필하지 않도록 null 을 반환한다(기존 빈 입력 동작 유지).
         */
        fun resolvePrefillFormula(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }
    }

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
