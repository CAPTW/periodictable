package com.chemtable.interactive.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ChemTableColors.primary,
    onPrimary = ChemTableColors.onPrimary,
    primaryContainer = ChemTableColors.primaryContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = ChemTableColors.primary,
    onPrimary = ChemTableColors.onPrimary,
    primaryContainer = ChemTableColors.primaryContainer
)

@Composable
fun ChemTableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChemTableTypography,
        shapes = ChemTableShapes,
        content = content
    )
}
