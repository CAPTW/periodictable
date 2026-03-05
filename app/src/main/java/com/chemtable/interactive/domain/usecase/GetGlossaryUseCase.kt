package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.domain.repository.GlossaryRepository
import javax.inject.Inject

class GetGlossaryUseCase @Inject constructor(
    private val repository: GlossaryRepository
) {
    fun allTerms() = repository.getAllTerms()
    fun termsByCategory(category: String) = repository.getTermsByCategory(category)
    fun searchTerms(query: String) = repository.searchTerms(query)
    suspend fun setBookmark(termId: String, bookmarked: Boolean) =
        repository.setBookmark(termId, bookmarked)
}
