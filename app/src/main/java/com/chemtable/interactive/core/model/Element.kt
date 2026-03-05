package com.chemtable.interactive.core.model

data class Element(
    val atomicNumber: Int,
    val symbol: String,
    val name: String,
    val nameKo: String,
    val category: ElementCategory,
    val stateOfMatter: StateOfMatter,
    val electronConfiguration: String,
    val molarMass: Double,
    val heatOfVaporization: Double?,
    val specificHeatCapacity: Double?,
    val thermalExpansionCoefficient: Double?,
    val halfLife: String?,
    val neutronCrossSection: Double?,
    val barn: Double?,
    val thermalConductivity: Double?,
    val electronegativity: Double?,
    val atomicRadius: Double?,
    val period: Int,
    val group: Int
) {
    val displayLine = "$symbol · $atomicNumber"
}
