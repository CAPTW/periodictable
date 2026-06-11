package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.repository.IsotopeRepository
import javax.inject.Inject

class GetIsotopesByElementUseCase @Inject constructor(
    private val repository: IsotopeRepository
) {
    operator fun invoke(atomicNumber: Int) = repository.getIsotopesByAtomicNumber(atomicNumber)
}
