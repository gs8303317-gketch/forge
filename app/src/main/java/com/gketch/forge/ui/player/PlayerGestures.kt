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
import androidx.compose.material.icons.rounded.Speed
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
import kotlinx.coroutines.withTimeoutOrNull

private enum class GestureKind { Seek, Volume, Brightness }

private const val EDGE_FRACTION = 0.20f
private const val DOUBLE_TAP_THIRD = 1f / 3f

@Composable
fun PlayerGestureLayer(
    durationMs: Long,
    positionMs: Long,
    seekSeconds: Int = 10,
    onSeek: (Long) -> Unit,
    onSeekPreview: (Long) -> Unit = {},
    onDoubleTapSeek: (back: Boolean) -> Unit,
    onVolumeFraction: (Float) -> Unit,
    onBrightnessFraction: (Float) -> Unit,
    onHoldSpeedStart: () -> Unit = {},
    onHoldSpeedEnd: () -> Unit = {},
    onTap: () -> Unit,
    currentVolume: () -> Float,
    currentBrightness: () -> Float,
    gesturesEnabled: Boolean = true,
    controlsVisible: Boolean = false,
    excludeTopPx: Float = 0f,
    excludeBottomPx: Float = 0f,
    sensitivityMultiplier: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val durationState = rememberUpdatedState(durationMs)
    val positionState = rememberUpdatedState(positionMs)
    val volumeState = rememberUpdatedState(currentVolume)
    val brightnessState = rememberUpdatedState(currentBrightness)
    val seekState = rememberUpdatedState(onSeek)
    val previewState = rememberUpdatedState(onSeekPreview)
    val doubleTapState = rememberUpdatedState(onDoubleTapSeek)
    val volCb = rememberUpdatedState(onVolumeFraction)
    val britCb = rememberUpdatedState(onBrightnessFraction)
    val tapState = rememberUpdatedState(onTap)
    val holdStart = rememberUpdatedState(onHoldSpeedStart)
    val holdEnd = rememberUpdatedState(onHoldSpeedEnd)
    val seekSecState = rememberUpdatedState(seekSeconds)
    val controlsState = rememberUpdatedState(controlsVisible)
    val topExclude = rememberUpdatedState(excludeTopPx)
    val bottomExclude = rememberUpdatedState(excludeBottomPx)

    var kind by remember { mutableStateOf<GestureKind?>(null) }
    var previewMs by remember { mutableLongStateOf(0L) }
    var barFraction by remember { mutableFloatStateOf(0f) }
    var doubleTapFlash by remember { mutableStateOf<Boolean?>(null) }
    var holdSpeedActive by remember { mutableStateOf(false) }

    LaunchedEffect(doubleTapFlash) {
        if (doubleTapFlash != null) {
            delay(450)
            doubleTapFlash = null
        }
    }

    val enabledState = rememberUpdatedState(gesturesEnabled)
    val sensState = rememberUpdatedState(sensitivityMultiplier)

    fun inChrome(y: Float, height: Float): Boolean {
        val top = topExclude.value
        val bottom = bottomExclude.value
        return y < top || y > height - bottom
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gesturesEnabled, controlsVisible, excludeTopPx, excludeBottomPx) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!enabledState.value) return@detectTapGestures
                        if (inChrome(offset.y, size.height.toFloat())) return@detectTapGestures
                        val third = size.width * DOUBLE_TAP_THIRD
                        when {
                            offset.x < third -> {
                                doubleTapFlash = true
                                doubleTapState.value(true)
                            }
                            offset.x > size.width - third -> {
                                doubleTapFlash = false
                                doubleTapState.value(false)
                            }
                            else -> tapState.value()
                        }
                    },
                    onTap = { offset ->
                        if (inChrome(offset.y, size.height.toFloat()) && controlsState.value) return@detectTapGestures
                        tapState.value()
                    },
                    onPress = { offset ->
                        if (!enabledState.value) {
                            tryAwaitRelease()
                            return@detectTapGestures
                        }
                        if (inChrome(offset.y, size.height.toFloat())) {
                            tryAwaitRelease()
                            return@detectTapGestures
                        }
                        val releasedEarly = withTimeoutOrNull(400) {
                            tryAwaitRelease()
                            true
                        }
                        if (releasedEarly == true) return@detectTapGestures
                        holdSpeedActive = true
                        holdStart.value()
                        try {
                            tryAwaitRelease()
                        } finally {
                            holdSpeedActive = false
                            holdEnd.value()
                        }
                    },
                )
            }
            .pointerInput(gesturesEnabled, controlsVisible, excludeTopPx, excludeBottomPx, sensitivityMultiplier) {
                if (!gesturesEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val start = down.position
                    if (inChrome(start.y, size.height.toFloat())) {
                        return@awaitEachGesture
                    }
                    var total = Offset.Zero
                    var classified: GestureKind? = null
                    var startVol = 0f
                    var startBrit = 0f
                    var startPos = positionState.value
                    val dur = durationState.value.coerceAtLeast(0L)
                    val slop = 24f
                    val leftEdge = size.width * EDGE_FRACTION
                    val rightEdge = size.width * (1f - EDGE_FRACTION)

                    drag(down.id) { change ->
                        val delta = change.positionChange()
                        total += delta
                        if (classified == null && (abs(total.x) > slop || abs(total.y) > slop)) {
                            classified = when {
                                abs(total.x) >= abs(total.y) -> {
                                    if (start.x in leftEdge..rightEdge) GestureKind.Seek else null
                                }
                                start.x <= leftEdge -> GestureKind.Brightness
                                start.x >= rightEdge -> GestureKind.Volume
                                else -> null
                            }
                            startVol = volumeState.value().coerceIn(0f, 1f)
                            startBrit = brightnessState.value().coerceIn(0f, 1f)
                            startPos = positionState.value
                            kind = classified
                        }
                        if (classified != null) {
                            change.consume()
                        }
                        when (classified) {
                            GestureKind.Seek -> {
                                val sens = sensState.value.coerceIn(0.25f, 3f)
                                val window = if (dur > 0L) minOf(dur, 180_000L).toFloat() else 180_000f
                                val deltaMs = ((total.x / size.width) * window * sens).roundToLong()
                                val target = (startPos + deltaMs).coerceIn(0L, if (dur > 0L) dur else Long.MAX_VALUE)
                                previewMs = target
                                previewState.value(target)
                            }
                            GestureKind.Volume -> {
                                val sens = sensState.value.coerceIn(0.25f, 3f)
                                val next = (startVol - (total.y / size.height) * sens).coerceIn(0f, 1f)
                                barFraction = next
                                volCb.value(next)
                            }
                            GestureKind.Brightness -> {
                                val sens = sensState.value.coerceIn(0.25f, 3f)
                                val next = (startBrit - (total.y / size.height) * sens).coerceIn(0f, 1f)
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
            true -> DoubleTapHud(back = true, seconds = seekSecState.value)
            false -> DoubleTapHud(back = false, seconds = seekSecState.value)
            null -> Unit
        }
        if (holdSpeedActive) {
            HoldSpeedHud()
        }
    }
}

@Composable
private fun HoldSpeedHud() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        RowHud {
            Icon(Icons.Rounded.Speed, null, tint = ForgeAccent)
            Spacer(Modifier.height(4.dp))
            Text("2×", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
private fun RowHud(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun DoubleTapHud(back: Boolean, seconds: Int) {
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
                text = if (back) "−${seconds}s" else "+${seconds}s",
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ForgeGraphite),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight(fraction.coerceIn(0f, 1f))
                        .background(ForgeAccent),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
