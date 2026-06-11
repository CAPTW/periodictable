package com.chemtable.interactive.data.mapper

import com.chemtable.interactive.core.database.entity.IsotopeEntity
import com.chemtable.interactive.core.model.Isotope

fun IsotopeEntity.toDomain(): Isotope = Isotope(
    id = id,
    atomicNumber = atomicNumber,
    massNumber = massNumber,
    neutronCount = neutronCount,
    symbol = symbol,
    isStable = isStable,
    halfLife = halfLife,
    halfLifeSeconds = halfLifeSeconds,
    decayMode = decayMode,
    naturalAbundance = naturalAbundance,
    applicationTags = applicationTags
)

fun Isotope.toEntity(): IsotopeEntity = IsotopeEntity(
    id = id,
    atomicNumber = atomicNumber,
    massNumber = massNumber,
    neutronCount = neutronCount,
    symbol = symbol,
    isStable = isStable,
    halfLife = halfLife,
    halfLifeSeconds = halfLifeSeconds,
    decayMode = decayMode,
    naturalAbundance = naturalAbundance,
    applicationTags = applicationTags
)
