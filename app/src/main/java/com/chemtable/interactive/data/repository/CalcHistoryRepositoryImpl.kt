package com.chemtable.interactive.data.repository

import com.chemtable.interactive.core.database.dao.CalcHistoryDao
import com.chemtable.interactive.core.model.CalcHistory
import com.chemtable.interactive.data.mapper.toDomain
import com.chemtable.interactive.data.mapper.toEntity
import com.chemtable.interactive.domain.repository.CalcHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CalcHistoryRepositoryImpl @Inject constructor(
    private val dao: CalcHistoryDao
) : CalcHistoryRepository {

    override suspend fun countAll(): Int = dao.countAll()

    override fun getRecent(limit: Int): Flow<List<CalcHistory>> =
        dao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun save(history: CalcHistory) {
        dao.insert(history.toEntity())
    }

    override suspend fun trim(countToRemove: Int) {
        if (countToRemove > 0) {
            dao.trim(countToRemove)
        }
    }
}
