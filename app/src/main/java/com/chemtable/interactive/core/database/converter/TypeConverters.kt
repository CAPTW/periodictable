package com.chemtable.interactive.core.database.converter

import androidx.room.TypeConverter

object DbTypeConverters {
    private const val separator = ";;"

    @TypeConverter
    fun fromStringList(values: List<String>?): String =
        values.orEmpty().joinToString(separator = separator)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else value.split(separator)

    @TypeConverter
    fun fromIntList(values: List<Int>?): String =
        values.orEmpty().joinToString(separator = separator)

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(separator).mapNotNull { it.toIntOrNull() }
    }
}
