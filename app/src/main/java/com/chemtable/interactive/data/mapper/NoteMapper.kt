package com.chemtable.interactive.data.mapper

import com.chemtable.interactive.core.database.entity.NoteEntity
import com.chemtable.interactive.core.model.ElementNote

fun NoteEntity.toDomain(): ElementNote = ElementNote(
    id = id,
    elementAtomicNumber = elementAtomicNumber,
    title = title,
    content = content,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ElementNote.toEntity(): NoteEntity = NoteEntity(
    id = id,
    elementAtomicNumber = elementAtomicNumber,
    title = title,
    content = content,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt
)
