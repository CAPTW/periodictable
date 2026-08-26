package com.chemtable.interactive.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chemtable.interactive.core.model.AppSettings
import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.core.model.TableViewMode
import com.chemtable.interactive.core.model.ThemeMode
import com.chemtable.interactive.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = parseThemeMode(prefs[KEY_THEME]),
            fontScale = clampFontScale(prefs[KEY_FONT_SCALE] ?: AppSettings.DEFAULT.fontScale),
            tableViewMode = parseTableViewMode(prefs[KEY_TABLE_VIEW_MODE]),
            preferredClassicBoardSize = ClassicBoardSize.fromPersistenceValue(
                prefs[KEY_PREFERRED_CLASSIC_BOARD_SIZE]
            ),
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    override suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[KEY_FONT_SCALE] = clampFontScale(scale) }
    }

    override suspend fun setTableViewMode(mode: TableViewMode) {
        dataStore.edit { it[KEY_TABLE_VIEW_MODE] = mode.name }
    }

    override suspend fun setPreferredClassicBoardSize(size: ClassicBoardSize) {
        dataStore.edit { it[KEY_PREFERRED_CLASSIC_BOARD_SIZE] = size.persistenceValue }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        private val KEY_TABLE_VIEW_MODE = stringPreferencesKey("table_view_mode")
        private val KEY_PREFERRED_CLASSIC_BOARD_SIZE = intPreferencesKey("preferred_classic_board_size")

        /** Maps a stored string back to a [ThemeMode], falling back to the default for unknown/null. */
        fun parseThemeMode(raw: String?): ThemeMode =
            ThemeMode.entries.firstOrNull { it.name == raw } ?: AppSettings.DEFAULT.themeMode

        /** Maps a stored string back to a [TableViewMode], falling back to the default for unknown/null. */
        fun parseTableViewMode(raw: String?): TableViewMode =
            TableViewMode.entries.firstOrNull { it.name == raw } ?: AppSettings.DEFAULT.tableViewMode

        fun clampFontScale(value: Float): Float =
            value.coerceIn(AppSettings.MIN_FONT_SCALE, AppSettings.MAX_FONT_SCALE)
    }
}
