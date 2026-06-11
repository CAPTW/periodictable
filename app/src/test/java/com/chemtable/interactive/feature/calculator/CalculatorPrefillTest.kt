package com.chemtable.interactive.feature.calculator

import com.chemtable.interactive.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 2B: 계산기 프리필 정규화 로직 검증.
 *
 * CalculatorViewModel 전체 생성은 viewModelScope(Main 디스패처) 에 의존해
 * 순수 JUnit 환경에서 인스턴스화할 수 없고, kotlinx-coroutines-test 추가는
 * 이번 단계 제약(Gradle dependency 수정 금지)에 걸린다. 따라서 프리필 반영을
 * 결정하는 순수 함수만 단위 테스트로 검증한다(나머지는 런타임 스모크로 확인).
 */
class CalculatorPrefillTest {

    @Test
    fun resolvePrefill_withFormula_returnsTrimmedFormula() {
        assertEquals("H2O", CalculatorViewModel.resolvePrefillFormula("H2O"))
        assertEquals("CO2", CalculatorViewModel.resolvePrefillFormula("  CO2  "))
    }

    @Test
    fun resolvePrefill_withNullOrBlank_returnsNull() {
        assertNull(CalculatorViewModel.resolvePrefillFormula(null))
        assertNull(CalculatorViewModel.resolvePrefillFormula(""))
        assertNull(CalculatorViewModel.resolvePrefillFormula("   "))
    }

    @Test
    fun decodeArg_preservesPlusSignsAcrossDifferentEncodedStates() {
        // 1. Fully encoded input (where '+' was encoded to '%2B')
        assertEquals("Na+", Screen.Calculator.decodeArg("Na%2B"))
        assertEquals("H2O+NaCl", Screen.Calculator.decodeArg("H2O%2BNaCl"))

        // 2. Already decoded/raw input (where '+' is literal '+')
        // In form-url-encoding, '+' becomes a space. But decodeArg must preserve it!
        assertEquals("Na+", Screen.Calculator.decodeArg("Na+"))
        assertEquals("H2O+NaCl", Screen.Calculator.decodeArg("H2O+NaCl"))

        // 3. Encoded spaces
        assertEquals("H 2 O", Screen.Calculator.decodeArg("H%202%20O"))
    }
}
