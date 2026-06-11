package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.Isotope
import kotlinx.coroutines.flow.Flow

interface IsotopeRepository {
    fun getAllIsotopes(): Flow<List<Isotope>>
    fun getIsotopesByAtomicNumber(atomicNumber: Int): Flow<List<Isotope>>
}
