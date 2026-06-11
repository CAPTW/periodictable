package com.chemtable.interactive.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2B: Calculator 라우트 헬퍼 검증.
 * - formula 가 없으면 기존(빈 입력) 라우트를 반환해야 한다.
 * - formula 가 있으면 query 로 안전하게(percent-encoding) 포함해야 한다.
 * (android.net.Uri 가 아닌 순수 JVM URLEncoder 를 쓰므로 단위 테스트에서 검증 가능.)
 */
class CalculatorRouteTest {

    @Test
    fun createRoute_withoutFormula_returnsBaseRoute() {
        assertEquals("calculator", Screen.Calculator.createRoute())
        assertEquals("calculator", Screen.Calculator.createRoute(null))
    }

    @Test
    fun createRoute_withBlankFormula_returnsBaseRoute() {
        assertEquals("calculator", Screen.Calculator.createRoute(""))
        assertEquals("calculator", Screen.Calculator.createRoute("   "))
    }

    @Test
    fun createRoute_withSimpleFormula_includesFormulaSafely() {
        assertEquals("calculator?formula=H2O", Screen.Calculator.createRoute("H2O"))
        assertEquals("calculator?formula=CO2", Screen.Calculator.createRoute("CO2"))
        assertEquals("calculator?formula=NaCl", Screen.Calculator.createRoute("NaCl"))
    }

    @Test
    fun createRoute_trimsSurroundingWhitespace() {
        assertEquals("calculator?formula=H2O", Screen.Calculator.createRoute("  H2O  "))
    }

    @Test
    fun createRoute_encodesSpecialCharacters() {
        // 향후 괄호/점/전하 등 특수문자를 안전하게 처리하는지 확인.
        val route = Screen.Calculator.createRoute("Ca(OH)2")
        assertEquals("calculator?formula=Ca%28OH%292", route)
        assertTrue("placeholder 가 남으면 안 됨", !route.contains("{"))
    }

    @Test
    fun route_pattern_declaresOptionalFormulaArgument() {
        assertEquals("calculator?formula={formula}", Screen.Calculator.route)
        assertEquals("formula", Screen.Calculator.ARG_FORMULA)
    }

    @Test
    fun roundTrip_verifiesEncodingAndDecoding() {
        val testCases = listOf(
            "H2O",
            "Ca(OH)2",
            "CuSO4·5H2O",
            "H2O+NaCl",
            "Na+",
            "H 2 O"
        )
        for (formula in testCases) {
            val route = Screen.Calculator.createRoute(formula)
            val encodedArg = route.substringAfter("calculator?formula=")
            val decoded = Screen.Calculator.decodeArg(encodedArg)
            assertEquals(formula, decoded)
        }
    }
}
