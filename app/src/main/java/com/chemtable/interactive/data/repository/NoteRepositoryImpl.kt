package com.chemtable.interactive.data.repository

import com.chemtable.interactive.core.database.dao.NoteDao
import com.chemtable.interactive.core.model.ElementNote
import com.chemtable.interactive.data.mapper.toDomain
import com.chemtable.interactive.data.mapper.toEntity
import com.chemtable.interactive.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao
) : NoteRepository {
    override fun getNoteById(id: Long): Flow<ElementNote?> =
        dao.getNoteById(id).map { it?.toDomain() }

    override fun getNotesForElement(atomicNumber: Int): Flow<List<ElementNote>> =
        dao.getNotesForElement(atomicNumber).map { it.map { note -> note.toDomain() } }

    override fun getAllNotes(): Flow<List<ElementNote>> =
        dao.getAllNotes().map { it.map { note -> note.toDomain() } }

    override fun searchNotes(query: String): Flow<List<ElementNote>> =
        dao.searchNotes(query).map { it.map { note -> note.toDomain() } }

    override suspend fun upsertNote(note: ElementNote) = dao.upsertNote(note.toEntity())

    override suspend fun deleteNote(note: ElementNote) = dao.deleteNote(note.toEntity())
}
