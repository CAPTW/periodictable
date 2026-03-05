package com.chemtable.interactive.data.mapper

import com.chemtable.interactive.core.database.entity.CalcHistoryEntity
import com.chemtable.interactive.core.model.CalcHistory

fun CalcHistory.toEntity(): CalcHistoryEntity = CalcHistoryEntity(
    id = id,
    formula = formula,
    result = result,
    componentsJson = components.joinToString("|") { it },
    createdAt = createdAt
)

fun CalcHistoryEntity.toDomain(): CalcHistory = CalcHistory(
    id = id,
    formula = formula,
    result = result,
    components = if (componentsJson.isBlank()) {
        emptyList()
    } else {
        componentsJson.split("|").map { it.trim() }.filter { it.isNotBlank() }
    },
    createdAt = createdAt
)

