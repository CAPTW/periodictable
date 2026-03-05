package com.chemtable.interactive.core.model

data class ElementNote(
    val id: Long = 0L,
    val elementAtomicNumber: Int,
    val title: String,
    val content: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)
