package com.chemtable.interactive.data.repository

import com.chemtable.interactive.core.model.AppSettings
import com.chemtable.interactive.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-logic tests for the settings preference mapping/clamping (JVM, no device). */
class SettingsPreferencesTest {

    @Test
    fun parseThemeMode_knownValues_roundTrip() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, SettingsRepositoryImpl.parseThemeMode(mode.name))
        }
    }

    @Test
    fun parseThemeMode_unknownOrNull_fallsBackToDefault() {
        assertEquals(AppSettings.DEFAULT.themeMode, SettingsRepositoryImpl.parseThemeMode(null))
        assertEquals(AppSettings.DEFAULT.themeMode, SettingsRepositoryImpl.parseThemeMode("NOPE"))
        assertEquals(AppSettings.DEFAULT.themeMode, SettingsRepositoryImpl.parseThemeMode(""))
    }

    @Test
    fun clampFontScale_boundsValues() {
        assertEquals(AppSettings.MIN_FONT_SCALE, SettingsRepositoryImpl.clampFontScale(0.1f))
        assertEquals(AppSettings.MAX_FONT_SCALE, SettingsRepositoryImpl.clampFontScale(9f))
        assertEquals(1.0f, SettingsRepositoryImpl.clampFontScale(1.0f))
    }
}
