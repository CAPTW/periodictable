package com.chemtable.interactive.data.prepopulate

import android.content.Context
import com.chemtable.interactive.core.database.entity.GlossaryEntity
import com.chemtable.interactive.core.util.StartupTrace
import org.json.JSONArray
import org.json.JSONObject

class GlossaryDataLoader(
    private val context: Context
) {
    private val fileName = "glossary.json"

    fun load(): List<GlossaryEntity> {
        return StartupTrace.measure("GlossaryDataLoader.load") {
            val jsonString = StartupTrace.measure("GlossaryDataLoader.readText") {
                context.assets.open(fileName).bufferedReader().readText()
            }
            val array = StartupTrace.measure("GlossaryDataLoader.parseJsonArray") {
                JSONArray(jsonString)
            }
            StartupTrace.measure("GlossaryDataLoader.mapEntities") {
                (0 until array.length()).mapNotNull { index ->
                    runCatching {
                        array.getJSONObject(index).toGlossaryEntity()
                    }.getOrNull()
                }
            }
        }
    }

    private fun JSONObject.toGlossaryEntity() = GlossaryEntity(
        id = getString("id"),
        termKo = getString("termKo"),
        termEn = getString("termEn"),
        definition = getString("definition"),
        simpleExplanation = getString("simpleExplanation"),
        category = getString("category"),
        interactiveType = optStringOrNull("interactiveType"),
        relatedElements = getIntArray("relatedElements"),
        relatedTerms = getStringArray("relatedTerms"),
        isBookmarked = optBoolean("isBookmarked", false)
    )

    private fun JSONObject.getIntArray(key: String): List<Int> {
        if (!has(key) || isNull(key)) return emptyList()
        val array = getJSONArray(key)
        return (0 until array.length()).map { array.getInt(it) }
    }

    private fun JSONObject.getStringArray(key: String): List<String> {
        if (!has(key) || isNull(key)) return emptyList()
        val array = getJSONArray(key)
        return (0 until array.length()).map { array.getString(it) }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key, "").ifBlank { null }
    }
}
