package com.chemtable.interactive.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "isotopes",
    indices = [
        Index(value = ["atomic_number"], unique = false),
        Index(value = ["symbol"], unique = false)
    ]
)
data class IsotopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "atomic_number") val atomicNumber: Int,
    @ColumnInfo(name = "mass_number") val massNumber: Int,
    @ColumnInfo(name = "neutron_count") val neutronCount: Int,
    val symbol: String,
    @ColumnInfo(name = "is_stable") val isStable: Boolean,
    @ColumnInfo(name = "half_life") val halfLife: String?,
    @ColumnInfo(name = "half_life_seconds") val halfLifeSeconds: Double?,
    @ColumnInfo(name = "decay_mode") val decayMode: String?,
    @ColumnInfo(name = "natural_abundance") val naturalAbundance: Double?,
    @ColumnInfo(name = "application_tags") val applicationTags: List<String>
)
