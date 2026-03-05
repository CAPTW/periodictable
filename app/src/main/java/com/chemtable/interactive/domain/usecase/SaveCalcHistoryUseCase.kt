package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.core.model.CalcHistory
import com.chemtable.interactive.domain.repository.CalcHistoryRepository
import javax.inject.Inject

class SaveCalcHistoryUseCase @Inject constructor(
    private val repository: CalcHistoryRepository
) {
    private val maxEntries = 20

    suspend operator fun invoke(history: CalcHistory) {
        repository.save(history)
        val total = repository.countAll()
        val excess = total - maxEntries
        if (excess > 0) {
            repository.trim(excess)
        }
    }
}
