package com.gketch.forge.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val ForgeBlack = Color(0xFF0A0A0B)
val ForgeGraphite = Color(0xFF1A1B1E)
val ForgeSurface = Color(0xFF121214)
val ForgeSurfaceVariant = Color(0xFF222328)
val ForgeEmber = Color(0xFFFF6B35)
val ForgeAccentSoftDefault = Color(0xFFFF8F66)
val ForgeOnDark = Color(0xFFF5F5F7)
val ForgeMuted = Color(0xFF9A9AA3)
val ForgeOutline = Color(0xFF2E2F36)

val LocalForgeAccent = staticCompositionLocalOf { ForgeEmber }
val LocalForgeAccentSoft = staticCompositionLocalOf { ForgeAccentSoftDefault }

val ForgeAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalForgeAccent.current

val ForgeAccentSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalForgeAccentSoft.current
