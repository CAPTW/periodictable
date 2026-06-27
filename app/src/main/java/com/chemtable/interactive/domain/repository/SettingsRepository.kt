package com.chemtable.interactive.domain.repository

import com.chemtable.interactive.core.model.AppSettings
import com.chemtable.interactive.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Persists and exposes user app settings (theme, font scale). Offline-first, on-device only. */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setFontScale(scale: Float)
}
