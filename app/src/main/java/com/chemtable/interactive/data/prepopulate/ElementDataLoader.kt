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
}
