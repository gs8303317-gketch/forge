package com.gketch.forge.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** VLC Android night surfaces — near-black chrome, not blue-grey. */
val ForgeBlack = Color(0xFF121212)
val ForgeGraphite = Color(0xFF1A1A1A)
val ForgeSurface = Color(0xFF161616)
val ForgeSurfaceVariant = Color(0xFF242424)
/** Official VLC orange (#FF8800). */
val ForgeEmber = Color(0xFFFF8800)
val ForgeAccentSoftDefault = Color(0xFFFFB04A)
val ForgeOnDark = Color(0xFFF2F2F2)
val ForgeMuted = Color(0xFF9E9E9E)
val ForgeOutline = Color(0xFF2C2C2C)

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
