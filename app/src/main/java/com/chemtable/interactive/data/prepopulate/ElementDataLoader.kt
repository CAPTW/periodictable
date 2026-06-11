package com.chemtable.interactive.data.prepopulate

import android.content.Context
import com.chemtable.interactive.core.database.entity.ElementEntity
import org.json.JSONArray
import org.json.JSONObject

class ElementDataLoader(
    private val context: Context
) {
    private val fileName = "elements.json"

    fun load(): List<ElementEntity> {
        val jsonString = context.assets.open(fileName).bufferedReader().readText()
        val array = JSONArray(jsonString)
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val o = array.getJSONObject(index)
                o.toElementEntity()
            }.getOrNull()
        }
    }

    private fun JSONObject.toElementEntity() = ElementEntity(
        atomicNumber = getInt("atomicNumber"),
        symbol = getString("symbol"),
        name = getString("name"),
        nameKo = getString("nameKo"),
        latinName = optStringOrNull("latinName"),
        latinPronunciation = optStringOrNull("latinPronunciation"),
        englishPronunciation = optStringOrNull("englishPronunciation"),
        discoveryYear = optIntOrNull("discoveryYear"),
        discoverer = optStringOrNull("discoverer"),
        discoveryCountry = optStringOrNull("discoveryCountry"),
        costPer100gUsd = optDoubleOrNull("costPer100gUsd"),
        costReferenceDate = optStringOrNull("costReferenceDate"),
        casNumber = optStringOrNull("casNumber"),
        pubchemCid = optStringOrNull("pubchemCid"),
        rtecsNumber = optStringOrNull("rtecsNumber"),
        protonCount = optIntOrNull("protonCount"),
        neutronCount = optIntOrNull("neutronCount"),
        electronCount = optIntOrNull("electronCount"),
        electronShells = optStringOrNull("electronShells"),
        commonIonCharge = optStringOrNull("commonIonCharge"),
        ionizationPotential = optDoubleOrNull("ionizationPotential"),
        ionizationPossibility = optStringOrNull("ionizationPossibility"),
        covalentRadius = optDoubleOrNull("covalentRadius"),
        vanDerWaalsRadius = optDoubleOrNull("vanDerWaalsRadius"),
        block = optStringOrNull("block"),
        meltingPoint = optDoubleOrNull("meltingPoint"),
        boilingPoint = optDoubleOrNull("boilingPoint"),
        heatOfFusion = optDoubleOrNull("heatOfFusion"),
        liquidDensity = optDoubleOrNull("liquidDensity"),
        electricalConductivity = optDoubleOrNull("electricalConductivity"),
        electricalType = optStringOrNull("electricalType"),
        resistivity = optDoubleOrNull("resistivity"),
        superconductingTemperature = optDoubleOrNull("superconductingTemperature"),
        magnetism = optStringOrNull("magnetism"),
        volumeMagneticSusceptibility = optDoubleOrNull("volumeMagneticSusceptibility"),
        massMagneticSusceptibility = optDoubleOrNull("massMagneticSusceptibility"),
        molarMagneticSusceptibility = optDoubleOrNull("molarMagneticSusceptibility"),
        crystalStructure = optStringOrNull("crystalStructure"),
        crystalSystem = optStringOrNull("crystalSystem"),
        latticeA = optDoubleOrNull("latticeA"),
        latticeB = optDoubleOrNull("latticeB"),
        latticeC = optDoubleOrNull("latticeC"),
        latticeAlpha = optDoubleOrNull("latticeAlpha"),
        latticeBeta = optDoubleOrNull("latticeBeta"),
        latticeGamma = optDoubleOrNull("latticeGamma"),
        crystalHabit = optStringOrNull("crystalHabit"),
        debyeTemperature = optDoubleOrNull("debyeTemperature"),
        hardnessBrinell = optDoubleOrNull("hardnessBrinell"),
        hardnessMohs = optDoubleOrNull("hardnessMohs"),
        hardnessVickers = optDoubleOrNull("hardnessVickers"),
        bulkModulus = optDoubleOrNull("bulkModulus"),
        youngModulus = optDoubleOrNull("youngModulus"),
        poissonRatio = optDoubleOrNull("poissonRatio"),
        shearModulus = optDoubleOrNull("shearModulus"),
        speedOfSound = optDoubleOrNull("speedOfSound"),
        refractiveIndex = optDoubleOrNull("refractiveIndex"),
        electronAffinity = optDoubleOrNull("electronAffinity"),
        standardElectrodePotential = optDoubleOrNull("standardElectrodePotential"),
        radioactivityLevel = optStringOrNull("radioactivityLevel"),
        reactivityLevel = optStringOrNull("reactivityLevel"),
        hazardHealth = optIntOrNull("hazardHealth"),
        hazardFlammability = optIntOrNull("hazardFlammability"),
        hazardReactivity = optIntOrNull("hazardReactivity"),
        hazardSpecial = optStringOrNull("hazardSpecial"),
        abundanceUniverse = optDoubleOrNull("abundanceUniverse"),
        abundanceSun = optDoubleOrNull("abundanceSun"),
        abundanceOcean = optDoubleOrNull("abundanceOcean"),
        abundanceHuman = optDoubleOrNull("abundanceHuman"),
        abundanceCrust = optDoubleOrNull("abundanceCrust"),
        abundanceMeteorite = optDoubleOrNull("abundanceMeteorite"),
        dataSource = optStringOrNull("dataSource"),
        dataLicense = optStringOrNull("dataLicense"),
        dataUpdatedAt = optStringOrNull("dataUpdatedAt"),
        dataConfidence = optDoubleOrNull("dataConfidence"),
        category = getString("category"),
        state = optString("state", "UNKNOWN"),
        electronConfiguration = optString("electronConfiguration", ""),
        molarMass = getDouble("molarMass"),
        heatOfVaporization = optDoubleOrNull("heatOfVaporization"),
        specificHeatCapacity = optDoubleOrNull("specificHeatCapacity"),
        thermalExpansionCoefficient = optDoubleOrNull("thermalExpansionCoefficient"),
        halfLife = optStringOrNull("halfLife"),
        neutronCrossSection = optDoubleOrNull("neutronCrossSection"),
        barn = optDoubleOrNull("barn"),
        thermalConductivity = optDoubleOrNull("thermalConductivity"),
        electronegativity = optDoubleOrNull("electronegativity"),
        atomicRadius = optDoubleOrNull("atomicRadius"),
        period = getInt("period"),
        group = getInt("group")
    )

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key, Double.NaN).takeIf { !it.isNaN() }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return value.ifBlank { null }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getInt(key) }.getOrNull()
    }
}
