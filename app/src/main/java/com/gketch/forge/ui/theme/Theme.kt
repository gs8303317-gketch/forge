package com.gketch.forge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForgeDarkScheme = darkColorScheme(
    primary = ForgeAccent,
    onPrimary = Color.Black,
    primaryContainer = ForgeAccentSoft,
    onPrimaryContainer = Color.Black,
    secondary = ForgeAccentSoft,
    onSecondary = Color.Black,
    background = ForgeBlack,
    onBackground = ForgeOnDark,
    surface = ForgeSurface,
    onSurface = ForgeOnDark,
    surfaceVariant = ForgeSurfaceVariant,
    onSurfaceVariant = ForgeMuted,
    outline = ForgeOutline,
    error = Color(0xFFFF5C5C),
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeDarkScheme,
        typography = ForgeTypography,
        content = content,
    )
}
