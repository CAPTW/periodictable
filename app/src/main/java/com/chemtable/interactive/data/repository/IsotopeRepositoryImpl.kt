package com.chemtable.interactive.data.repository

import com.chemtable.interactive.core.database.dao.IsotopeDao
import com.chemtable.interactive.core.model.Isotope
import com.chemtable.interactive.data.mapper.toDomain
import com.chemtable.interactive.domain.repository.IsotopeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IsotopeRepositoryImpl @Inject constructor(
    private val dao: IsotopeDao
) : IsotopeRepository {

    override fun getAllIsotopes(): Flow<List<Isotope>> =
        dao.getAllIsotopes().map { list -> list.map { it.toDomain() } }

    override fun getIsotopesByAtomicNumber(atomicNumber: Int): Flow<List<Isotope>> =
        dao.getIsotopesByAtomicNumber(atomicNumber).map { list -> list.map { it.toDomain() } }
}
