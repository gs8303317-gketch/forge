package com.gketch.forge.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.gketch.forge.ui.library.formatDuration
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeGraphite
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

private enum class GestureKind { Seek, Volume, Brightness }

@Composable
fun PlayerGestureLayer(
    durationMs: Long,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onDoubleTapSeek: (back: Boolean) -> Unit,
    onVolumeFraction: (Float) -> Unit,
    onBrightnessFraction: (Float) -> Unit,
    onTap: () -> Unit,
    currentVolume: () -> Float,
    currentBrightness: () -> Float,
    gesturesEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val durationState = rememberUpdatedState(durationMs)
    val positionState = rememberUpdatedState(positionMs)
    val volumeState = rememberUpdatedState(currentVolume)
    val brightnessState = rememberUpdatedState(currentBrightness)
    val seekState = rememberUpdatedState(onSeek)
    val doubleTapState = rememberUpdatedState(onDoubleTapSeek)
    val volCb = rememberUpdatedState(onVolumeFraction)
    val britCb = rememberUpdatedState(onBrightnessFraction)
    val tapState = rememberUpdatedState(onTap)

    var kind by remember { mutableStateOf<GestureKind?>(null) }
    var previewMs by remember { mutableLongStateOf(0L) }
    var barFraction by remember { mutableFloatStateOf(0f) }
    var doubleTapFlash by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(doubleTapFlash) {
        if (doubleTapFlash != null) {
            delay(450)
            doubleTapFlash = null
        }
    }

    val enabledState = rememberUpdatedState(gesturesEnabled)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gesturesEnabled) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!enabledState.value) return@detectTapGestures
                        val back = offset.x < size.width / 2f
                        doubleTapFlash = back
                        doubleTapState.value(back)
                    },
                    onTap = { tapState.value() },
                )
            }
            .pointerInput(gesturesEnabled) {
                if (!gesturesEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    var total = Offset.Zero
                    var classified: GestureKind? = null
                    var startVol = 0f
                    var startBrit = 0f
                    var startPos = positionState.value
                    val dur = durationState.value.coerceAtLeast(0L)
                    val slop = 24f

                    drag(down.id) { change ->
                        val delta = change.positionChange()
                        change.consume()
                        total += delta
                        if (classified == null && (abs(total.x) > slop || abs(total.y) > slop)) {
                            classified = if (abs(total.x) >= abs(total.y)) {
                                GestureKind.Seek
                            } else if (start.x < size.width / 2f) {
                                GestureKind.Brightness
                            } else {
                                GestureKind.Volume
                            }
                            startVol = volumeState.value().coerceIn(0f, 1f)
                            startBrit = brightnessState.value().coerceIn(0f, 1f)
                            startPos = positionState.value
                            kind = classified
                        }
                        when (classified) {
                            GestureKind.Seek -> {
                                val window = if (dur > 0L) minOf(dur, 180_000L).toFloat() else 180_000f
                                val deltaMs = ((total.x / size.width) * window).roundToLong()
                                val target = (startPos + deltaMs).coerceIn(0L, if (dur > 0L) dur else Long.MAX_VALUE)
                                previewMs = target
                            }
                            GestureKind.Volume -> {
                                val next = (startVol - total.y / size.height).coerceIn(0f, 1f)
                                barFraction = next
                                volCb.value(next)
                            }
                            GestureKind.Brightness -> {
                                val next = (startBrit - total.y / size.height).coerceIn(0f, 1f)
                                barFraction = next
                                britCb.value(next)
                            }
                            null -> Unit
                        }
                    }

                    if (classified == GestureKind.Seek) {
                        seekState.value(previewMs)
                    }
                    kind = null
                }
            },
    ) {
        when (kind) {
            GestureKind.Seek -> SeekHud(previewMs = previewMs, fromMs = positionMs)
            GestureKind.Volume -> SideHud(
                icon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White) },
                fraction = barFraction,
                alignment = Alignment.CenterEnd,
            )
            GestureKind.Brightness -> SideHud(
                icon = { Icon(Icons.Rounded.BrightnessHigh, null, tint = Color.White) },
                fraction = barFraction,
                alignment = Alignment.CenterStart,
            )
            null -> Unit
        }
        when (doubleTapFlash) {
            true -> DoubleTapHud(back = true)
            false -> DoubleTapHud(back = false)
            null -> Unit
        }
    }
}

@Composable
private fun DoubleTapHud(back: Boolean) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = if (back) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (back) Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
                contentDescription = null,
                tint = ForgeAccent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (back) "−10s" else "+10s",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SeekHud(previewMs: Long, fromMs: Long) {
    val delta = previewMs - fromMs
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (delta >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = ForgeAccent,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatDuration(previewMs),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Text(
                text = (if (delta >= 0) "+" else "−") + formatDuration(abs(delta)),
                style = MaterialTheme.typography.labelSmall,
                color = ForgeAccent,
            )
        }
    }
}

@Composable
private fun SideHud(
    icon: @Composable () -> Unit,
    fraction: Float,
    alignment: Alignment,
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ForgeGraphite),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight(fraction.coerceIn(0f, 1f))
                        .background(ForgeAccent),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
