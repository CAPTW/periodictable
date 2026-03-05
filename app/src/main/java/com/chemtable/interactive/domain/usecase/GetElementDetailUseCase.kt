package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.repository.ElementRepository
import javax.inject.Inject

class GetElementDetailUseCase @Inject constructor(
    private val repository: ElementRepository
) {
    operator fun invoke(atomicNumber: Int) = repository.getElementByAtomicNumber(atomicNumber)
}
