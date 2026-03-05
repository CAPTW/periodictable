package com.chemtable.interactive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chemtable.interactive.core.database.entity.GlossaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossaryDao {
    @Query("SELECT * FROM glossary ORDER BY term_ko")
    fun getAllTerms(): Flow<List<GlossaryEntity>>

    @Query("SELECT * FROM glossary WHERE category = :category ORDER BY term_ko")
    fun getTermsByCategory(category: String): Flow<List<GlossaryEntity>>

    @Query(
        "SELECT * FROM glossary WHERE term_ko LIKE '%' || :query || '%' OR term_en LIKE '%' || :query || '%'"
    )
    fun searchTerms(query: String): Flow<List<GlossaryEntity>>

    @Query("UPDATE glossary SET is_bookmarked = :bookmarked WHERE id = :termId")
    suspend fun setBookmark(termId: String, bookmarked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTerms(terms: List<GlossaryEntity>)

    @Query("SELECT COUNT(*) FROM glossary")
    suspend fun countTerms(): Int
}
