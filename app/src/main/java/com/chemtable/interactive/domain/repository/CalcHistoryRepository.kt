package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.CalcHistory
import kotlinx.coroutines.flow.Flow

interface CalcHistoryRepository {
    suspend fun countAll(): Int
    fun getRecent(limit: Int = 20): Flow<List<CalcHistory>>
    suspend fun save(history: CalcHistory)
    suspend fun trim(countToRemove: Int)
}
