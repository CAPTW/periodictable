package com.chemtable.interactive.core.model

/** User-selectable app theme. */
enum class ThemeMode { LIGHT, DARK, SOLARIZED }

/**
 * User preferences applied app-wide. [fontScale] multiplies the system font scale, so all text
 * (sp) sizes scale uniformly; it is clamped to [[MIN_FONT_SCALE], [MAX_FONT_SCALE]].
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val fontScale: Float = 1f,
) {
    companion object {
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.30f
        val DEFAULT = AppSettings()
    }
}
