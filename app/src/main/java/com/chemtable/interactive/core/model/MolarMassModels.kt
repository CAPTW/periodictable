package com.chemtable.interactive.core.model

data class FormulaComponentResult(
    val symbol: String,
    val count: Int,
    val atomicWeight: Double,
    val totalMass: Double,
    val percentage: Double
)

data class MolarMassResult(
    val formula: String,
    val totalMolarMass: Double,
    val components: List<FormulaComponentResult>
)

data class ParsedFormulaComponent(
    val symbol: String,
    val count: Int
)

data class ParsedFormulaResult(
    val formula: String,
    val components: List<ParsedFormulaComponent>
)

data class CalcHistory(
    val id: Long = 0L,
    val formula: String,
    val result: Double,
    val components: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

