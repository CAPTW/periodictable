package com.chemtable.interactive.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chemtable.interactive.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM element_notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM element_notes WHERE element_atomic_number = :atomicNumber ORDER BY updated_at DESC")
    fun getNotesForElement(atomicNumber: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM element_notes ORDER BY updated_at DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query(
        "SELECT * FROM element_notes WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'"
    )
    fun searchNotes(query: String): Flow<List<NoteEntity>>
}
