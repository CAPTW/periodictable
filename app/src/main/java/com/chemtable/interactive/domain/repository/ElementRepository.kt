package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementProperty
import kotlinx.coroutines.flow.Flow

interface ElementRepository {
    fun getElements(): Flow<List<Element>>
    fun getElementByAtomicNumber(number: Int): Flow<Element?>
    fun searchElements(query: String): Flow<List<Element>>
    fun searchBySymbol(symbol: String): Flow<List<Element>>
    fun filterByProperty(property: ElementProperty, min: Double, max: Double): Flow<List<Element>>
}
