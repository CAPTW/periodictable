package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.model.GameMoleculeDiscovery
import com.chemtable.interactive.domain.repository.GameStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDiscoveredMoleculesUseCase @Inject constructor(
    private val repository: GameStatsRepository
) {
    operator fun invoke(): Flow<List<GameMoleculeDiscovery>> = repository.observeDiscoveredMolecules()
}
