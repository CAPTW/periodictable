package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.repository.ElementRepository
import javax.inject.Inject

class GetElementsUseCase @Inject constructor(
    private val repository: ElementRepository
) {
    operator fun invoke() = repository.getElements()
}
