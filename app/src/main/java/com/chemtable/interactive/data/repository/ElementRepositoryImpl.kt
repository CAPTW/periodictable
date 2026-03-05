package com.chemtable.interactive.data.repository

import com.chemtable.interactive.core.database.dao.ElementDao
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementProperty
import com.chemtable.interactive.data.mapper.toDomain
import com.chemtable.interactive.domain.repository.ElementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ElementRepositoryImpl @Inject constructor(
    private val dao: ElementDao
) : ElementRepository {

    override fun getElements(): Flow<List<Element>> = dao.getAllElements().map { list -> list.map { it.toDomain() } }

    override fun getElementByAtomicNumber(number: Int): Flow<Element?> =
        dao.getElementByNumber(number).map { it?.toDomain() }

    override fun searchElements(query: String): Flow<List<Element>> =
        dao.searchByNameOrNumber(query).map { list -> list.map { it.toDomain() } }

    override fun searchBySymbol(symbol: String): Flow<List<Element>> =
        dao.searchBySymbol(symbol).map { list -> list.map { it.toDomain() } }

    override fun filterByProperty(
        property: ElementProperty,
        min: Double,
        max: Double
    ): Flow<List<Element>> = when (property) {
        ElementProperty.ELECTRONEGATIVITY -> dao.filterByElectronegativity(min, max)
        ElementProperty.ATOMIC_RADIUS -> dao.filterByAtomicRadius(min, max)
        ElementProperty.MOLAR_MASS -> dao.filterByMolarMass(min, max)
        ElementProperty.THERMAL_CONDUCTIVITY -> dao.filterByThermalConductivity(min, max)
    }.map { list -> list.map { it.toDomain() } }
}
