package com.chemtable.interactive.data.mapper

import com.chemtable.interactive.core.database.entity.ElementEntity
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementCategory
import com.chemtable.interactive.core.model.StateOfMatter

fun ElementEntity.toDomain(): Element = Element(
    atomicNumber = atomicNumber,
    symbol = symbol,
    name = name,
    nameKo = nameKo,
    category = ElementCategory.from(category),
    stateOfMatter = StateOfMatter.from(state),
    electronConfiguration = electronConfiguration,
    molarMass = molarMass,
    heatOfVaporization = heatOfVaporization,
    specificHeatCapacity = specificHeatCapacity,
    thermalExpansionCoefficient = thermalExpansionCoefficient,
    halfLife = halfLife,
    neutronCrossSection = neutronCrossSection,
    barn = barn,
    thermalConductivity = thermalConductivity,
    electronegativity = electronegativity,
    atomicRadius = atomicRadius,
    period = period,
    group = group
)

fun Element.toEntity(): ElementEntity = ElementEntity(
    atomicNumber = atomicNumber,
    symbol = symbol,
    name = name,
    nameKo = nameKo,
    category = category.name,
    state = stateOfMatter.name,
    electronConfiguration = electronConfiguration,
    molarMass = molarMass,
    heatOfVaporization = heatOfVaporization,
    specificHeatCapacity = specificHeatCapacity,
    thermalExpansionCoefficient = thermalExpansionCoefficient,
    halfLife = halfLife,
    neutronCrossSection = neutronCrossSection,
    barn = barn,
    thermalConductivity = thermalConductivity,
    electronegativity = electronegativity,
    atomicRadius = atomicRadius,
    period = period,
    group = group
)
