package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.ElementNote
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNoteById(id: Long): Flow<ElementNote?>
    fun getNotesForElement(atomicNumber: Int): Flow<List<ElementNote>>
    fun getAllNotes(): Flow<List<ElementNote>>
    fun searchNotes(query: String): Flow<List<ElementNote>>
    suspend fun upsertNote(note: ElementNote)
    suspend fun deleteNote(note: ElementNote)
}
