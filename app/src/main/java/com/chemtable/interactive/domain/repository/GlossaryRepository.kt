package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.GlossaryTerm
import kotlinx.coroutines.flow.Flow

interface GlossaryRepository {
    fun getAllTerms(): Flow<List<GlossaryTerm>>
    fun getTermsByCategory(category: String): Flow<List<GlossaryTerm>>
    fun searchTerms(query: String): Flow<List<GlossaryTerm>>
    suspend fun setBookmark(termId: String, bookmarked: Boolean)
}
