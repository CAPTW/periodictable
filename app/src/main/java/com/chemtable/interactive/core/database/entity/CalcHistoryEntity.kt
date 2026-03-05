package com.chemtable.interactive.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calc_history")
data class CalcHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formula: String,
    val result: Double,
    @ColumnInfo(name = "components_json") val componentsJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
