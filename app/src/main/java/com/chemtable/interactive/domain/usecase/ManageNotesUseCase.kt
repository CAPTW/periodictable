package com.chemtable.interactive.domain.usecase

import com.chemtable.interactive.core.model.ElementNote
import com.chemtable.interactive.domain.repository.NoteRepository
import javax.inject.Inject

class ManageNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    fun getNoteById(id: Long) = repository.getNoteById(id)
    fun getNotesForElement(atomicNumber: Int) = repository.getNotesForElement(atomicNumber)
    fun getAllNotes() = repository.getAllNotes()
    fun searchNotes(query: String) = repository.searchNotes(query)

    suspend fun upsert(note: ElementNote) = repository.upsertNote(note)
    suspend fun delete(note: ElementNote) = repository.deleteNote(note)
}
