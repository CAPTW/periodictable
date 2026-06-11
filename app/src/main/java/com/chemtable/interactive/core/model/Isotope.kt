package com.chemtable.interactive.core.model

data class Isotope(
    val id: Long,
    val atomicNumber: Int,
    val massNumber: Int,
    val neutronCount: Int,
    val symbol: String,
    val isStable: Boolean,
    val halfLife: String?,
    val halfLifeSeconds: Double?,
    val decayMode: String?,
    val naturalAbundance: Double?,
    val applicationTags: List<String>
)

enum class IsotopeStabilityFilter {
    ALL,
    STABLE,
    RADIOACTIVE
}

data class IsotopeSearchResult(
    val isotope: Isotope,
    val elementSymbol: String,
    val elementNameKo: String,
    val elementNameEn: String
)
