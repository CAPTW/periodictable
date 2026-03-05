package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.core.model.MolarMassResult
import com.chemtable.interactive.core.util.MolarMassCalculator
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CalculateMolarMassUseCase @Inject constructor(
    private val getElementsUseCase: GetElementsUseCase,
    private val calculator: MolarMassCalculator
) {
    suspend operator fun invoke(formula: String): MolarMassResult {
        val elements = getElementsUseCase().first()
        return calculator.calculate(formula, elements)
    }
}

