package com.chemtable.interactive.data.prepopulate

import android.content.Context
import com.chemtable.interactive.core.database.entity.IsotopeEntity
import org.json.JSONArray
import org.json.JSONObject

class IsotopeDataLoader(
    private val context: Context
) {
    private val fileName = "isotopes.json"

    fun load(): List<IsotopeEntity> {
        val jsonString = context.assets.open(fileName).bufferedReader().readText()
        val array = JSONArray(jsonString)
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                array.getJSONObject(index).toIsotopeEntity()
            }.getOrNull()
        }
    }

    private fun JSONObject.toIsotopeEntity(): IsotopeEntity {
        val atomicNumber = getInt("atomicNumber")
        val massNumber = getInt("massNumber")
        return IsotopeEntity(
            atomicNumber = atomicNumber,
            massNumber = massNumber,
            neutronCount = massNumber - atomicNumber,
            symbol = getString("symbol"),
            isStable = optBoolean("isStable", false),
            halfLife = optStringOrNull("halfLife"),
            halfLifeSeconds = optDoubleOrNull("halfLifeSeconds"),
            decayMode = optStringOrNull("decayMode"),
            naturalAbundance = optDoubleOrNull("naturalAbundance"),
            applicationTags = getStringArray("applicationTags")
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key, "").ifBlank { null }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key, Double.NaN).takeIf { !it.isNaN() }
    }

    private fun JSONObject.getStringArray(key: String): List<String> {
        if (!has(key) || isNull(key)) return emptyList()
        val array = getJSONArray(key)
        return (0 until array.length()).map { index -> array.optString(index, "").trim() }.filter { it.isNotBlank() }
    }
}
