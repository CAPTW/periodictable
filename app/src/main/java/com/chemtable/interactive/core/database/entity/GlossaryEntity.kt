package com.chemtable.interactive.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glossary")
data class GlossaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "term_ko") val termKo: String,
    @ColumnInfo(name = "term_en") val termEn: String,
    val definition: String,
    @ColumnInfo(name = "simple_explain") val simpleExplanation: String,
    val category: String,
    @ColumnInfo(name = "interactive_type") val interactiveType: String?,
    @ColumnInfo(name = "related_elements") val relatedElements: List<Int>,
    @ColumnInfo(name = "related_terms") val relatedTerms: List<String>,
    @ColumnInfo(name = "is_bookmarked") val isBookmarked: Boolean = false
)
