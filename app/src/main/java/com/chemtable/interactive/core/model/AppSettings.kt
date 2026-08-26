package com.chemtable.interactive.core.model

/** User-selectable app theme. */
enum class ThemeMode { LIGHT, DARK, SOLARIZED }

/**
 * How the periodic table is laid out on the Main screen.
 *
 * [FIT] scales the whole table to fit a single screen (static overview);
 * [ZOOM] renders cells at a readable size and lets the user pan/pinch around a larger table.
 */
enum class TableViewMode { FIT, ZOOM }

/**
 * User preferences applied app-wide. [fontScale] multiplies the system font scale, so all text
 * (sp) sizes scale uniformly; it is clamped to [[MIN_FONT_SCALE], [MAX_FONT_SCALE]].
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val fontScale: Float = 1f,
    val tableViewMode: TableViewMode = TableViewMode.FIT,
    val preferredClassicBoardSize: ClassicBoardSize = ClassicBoardSize.DEFAULT,
) {
    companion object {
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.30f
        val DEFAULT = AppSettings()
    }
}
