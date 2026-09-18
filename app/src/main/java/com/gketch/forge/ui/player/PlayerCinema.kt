package com.gketch.forge.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val CinemaBottomScrim: Brush = Brush.verticalGradient(
    0.00f to Color.Transparent,
    0.22f to Color.Black.copy(alpha = 0.22f),
    0.55f to Color.Black.copy(alpha = 0.55f),
    1.00f to Color.Black.copy(alpha = 0.82f),
)

internal val CinemaTopScrim: Brush = Brush.verticalGradient(
    0.00f to Color.Black.copy(alpha = 0.72f),
    0.55f to Color.Black.copy(alpha = 0.28f),
    1.00f to Color.Transparent,
)

@Composable
internal fun BoxScope.CinemaEdgeScrims(visible: Boolean) {
    if (!visible) return
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(96.dp)
            .background(CinemaTopScrim),
    )
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(168.dp)
            .background(CinemaBottomScrim),
    )
}
