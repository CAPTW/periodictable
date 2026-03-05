package com.chemtable.interactive.core.util

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.FormulaComponentResult
import com.chemtable.interactive.core.model.MolarMassResult

import javax.inject.Inject

class MolarMassCalculator @Inject constructor(
    private val parser: FormulaParser
) {
    fun calculate(formula: String, elements: List<Element>): MolarMassResult {
        if (formula.isBlank()) {
            throw IllegalArgumentException("수식을 입력하세요.")
        }

        val parsed = parser.parse(formula)
        if (parsed.components.isEmpty()) {
            throw IllegalArgumentException("유효한 원소를 찾지 못했습니다.")
        }

        val elementBySymbol = elements.associateBy { it.symbol }
        var total = 0.0

        val rawComponents = parsed.components.map { component ->
            val element = elementBySymbol[component.symbol]
                ?: throw IllegalArgumentException("지원하지 않는 원소입니다: ${component.symbol}")

            val totalMass = element.molarMass * component.count
            total += totalMass
            FormulaComponentResult(
                symbol = component.symbol,
                count = component.count,
                atomicWeight = element.molarMass,
                totalMass = totalMass,
                percentage = 0.0
            )
        }

        return MolarMassResult(
            formula = parsed.formula,
            totalMolarMass = total,
            components = rawComponents.map { component ->
                component.copy(
                    percentage = if (total == 0.0) 0.0 else component.totalMass * 100.0 / total
                )
            }
        )
    }
}

