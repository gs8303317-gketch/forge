package com.gketch.forge.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.gketch.forge.data.AccentPreset

@Composable
fun ForgeTheme(
    accentPreset: AccentPreset = AccentPreset.EMBER,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accent = Color(accentPreset.color)
    val accentSoft = Color(accentPreset.soft)
    val base = if (useDynamic) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            primaryContainer = accentSoft,
            onPrimaryContainer = Color.Black,
            secondary = accentSoft,
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
    }
    val scheme = base.copy(
        background = ForgeBlack,
        onBackground = ForgeOnDark,
        surface = ForgeSurface,
        onSurface = ForgeOnDark,
        surfaceVariant = ForgeSurfaceVariant,
        onSurfaceVariant = ForgeMuted,
        outline = ForgeOutline,
        error = Color(0xFFFF5C5C),
        primary = if (useDynamic) base.primary else accent,
        onPrimary = Color.Black,
        secondary = if (useDynamic) base.secondary else accentSoft,
        onSecondary = Color.Black,
    )
    val providedAccent = scheme.primary
    val providedSoft = scheme.secondary
    CompositionLocalProvider(
        LocalForgeAccent provides providedAccent,
        LocalForgeAccentSoft provides providedSoft,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ForgeTypography,
            content = content,
        )
    }
}
