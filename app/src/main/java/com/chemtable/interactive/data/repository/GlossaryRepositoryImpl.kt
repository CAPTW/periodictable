package com.chemtable.interactive.data.repository

import com.chemtable.interactive.core.database.dao.GlossaryDao
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.data.mapper.toDomain
import com.chemtable.interactive.data.mapper.toEntity
import com.chemtable.interactive.domain.repository.GlossaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GlossaryRepositoryImpl @Inject constructor(
    private val dao: GlossaryDao
) : GlossaryRepository {
    override fun getAllTerms(): Flow<List<GlossaryTerm>> =
        dao.getAllTerms().map { list -> list.map { it.toDomain() } }

    override fun getTermsByCategory(category: String): Flow<List<GlossaryTerm>> =
        dao.getTermsByCategory(category).map { list -> list.map { it.toDomain() } }

    override fun searchTerms(query: String): Flow<List<GlossaryTerm>> =
        dao.searchTerms(query).map { list -> list.map { it.toDomain() } }

    override suspend fun setBookmark(termId: String, bookmarked: Boolean) =
        dao.setBookmark(termId, bookmarked)
}
