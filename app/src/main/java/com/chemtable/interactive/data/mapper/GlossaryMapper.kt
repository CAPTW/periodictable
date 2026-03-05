package com.chemtable.interactive.data.mapper

import com.chemtable.interactive.core.database.entity.GlossaryEntity
import com.chemtable.interactive.core.model.GlossaryCategory
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.core.model.InteractiveType

fun GlossaryEntity.toDomain(): GlossaryTerm = GlossaryTerm(
    id = id,
    termKo = termKo,
    termEn = termEn,
    definition = definition,
    simpleExplanation = simpleExplanation,
    category = GlossaryCategory.valueOf(category),
    interactiveType = interactiveType?.let { InteractiveType.valueOf(it) },
    relatedElements = relatedElements,
    relatedTerms = relatedTerms,
    isBookmarked = isBookmarked
)

fun GlossaryTerm.toEntity(): GlossaryEntity = GlossaryEntity(
    id = id,
    termKo = termKo,
    termEn = termEn,
    definition = definition,
    simpleExplanation = simpleExplanation,
    category = category.name,
    interactiveType = interactiveType?.name,
    relatedElements = relatedElements,
    relatedTerms = relatedTerms,
    isBookmarked = isBookmarked
)
