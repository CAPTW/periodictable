package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.model.GameResultRecord
import com.chemtable.interactive.domain.repository.GameStatsRepository
import javax.inject.Inject

class RecordGameResultUseCase @Inject constructor(
    private val repository: GameStatsRepository
) {
    suspend operator fun invoke(record: GameResultRecord) = repository.recordGameResult(record)
}
