package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.core.model.CalcHistory
import com.chemtable.interactive.domain.repository.CalcHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCalcHistoryUseCase @Inject constructor(
    private val repository: CalcHistoryRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<CalcHistory>> = repository.getRecent(limit)
}

