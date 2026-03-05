package com.chemtable.interactive.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "elements",
    indices = [Index(value = ["symbol"], unique = true), Index(value = ["name"], unique = false)]
)
data class ElementEntity(
    @PrimaryKey @ColumnInfo(name = "atomic_number") val atomicNumber: Int,
    val symbol: String,
    val name: String,
    @ColumnInfo(name = "name_ko") val nameKo: String,
    val category: String,
    val state: String,
    @ColumnInfo(name = "electron_cfg") val electronConfiguration: String,
    @ColumnInfo(name = "molar_mass") val molarMass: Double,
    @ColumnInfo(name = "heat_vap") val heatOfVaporization: Double?,
    @ColumnInfo(name = "specific_heat") val specificHeatCapacity: Double?,
    @ColumnInfo(name = "thermal_exp") val thermalExpansionCoefficient: Double?,
    @ColumnInfo(name = "half_life") val halfLife: String?,
    @ColumnInfo(name = "neutron_cs") val neutronCrossSection: Double?,
    val barn: Double?,
    @ColumnInfo(name = "thermal_cond") val thermalConductivity: Double?,
    @ColumnInfo(name = "electro_neg") val electronegativity: Double?,
    @ColumnInfo(name = "atomic_radius") val atomicRadius: Double?,
    val period: Int,
    @ColumnInfo(name = "group_num") val group: Int
)
