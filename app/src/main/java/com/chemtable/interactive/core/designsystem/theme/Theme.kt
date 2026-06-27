package com.chemtable.interactive.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.chemtable.interactive.core.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = ChemTableColors.primary,
    onPrimary = ChemTableColors.onPrimary,
    primaryContainer = ChemTableColors.primaryContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = ChemTableColors.primary,
    onPrimary = ChemTableColors.onPrimary,
    primaryContainer = ChemTableColors.primaryContainer,
)

// Solarized (light) — the iconic Ethan Schoonover palette.
private val SolarizedColorScheme = lightColorScheme(
    primary = SolarizedColors.blue,
    onPrimary = SolarizedColors.base3,
    primaryContainer = SolarizedColors.base2,
    onPrimaryContainer = SolarizedColors.base01,
    secondary = SolarizedColors.cyan,
    onSecondary = SolarizedColors.base3,
    secondaryContainer = SolarizedColors.base2,
    onSecondaryContainer = SolarizedColors.base01,
    background = SolarizedColors.base3,
    onBackground = SolarizedColors.base00,
    surface = SolarizedColors.base3,
    onSurface = SolarizedColors.base00,
    surfaceVariant = SolarizedColors.base2,
    onSurfaceVariant = SolarizedColors.base01,
    outline = SolarizedColors.base1,
    error = SolarizedColors.red,
)

/**
 * App theme. [themeMode] selects the color scheme (Light / Dark / Solarized) and [fontScale]
 * multiplies the system font scale so every sp text size scales uniformly app-wide.
 */
@Composable
fun ChemTableTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.SOLARIZED -> SolarizedColorScheme
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * fontScale,
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChemTableTypography,
            shapes = ChemTableShapes,
            content = content,
        )
    }
}
