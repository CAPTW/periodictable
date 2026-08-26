package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.AppSettings
import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.core.model.TableViewMode
import com.chemtable.interactive.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Persists and exposes user app settings (theme, font scale, table view mode). Offline-first, on-device only. */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setFontScale(scale: Float)
    suspend fun setTableViewMode(mode: TableViewMode)
    suspend fun setPreferredClassicBoardSize(size: ClassicBoardSize)
}
