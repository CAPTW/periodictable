package com.chemtable.interactive.domain.model

data class GameResultRecord(
    val score: Int,
    val success: Boolean,
    val difficulty: String,
    val missionFormula: String?,
    val missionTargetCount: Int?,
    val playedAt: Long,
    val moleculesMade: List<String>,
)
