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
import androidx.compose.ui.util.fastForEach
import kotlin.math.sqrt
import androidx.compose.ui.unit.dp
import com.gketch.forge.data.BrightnessStore
import com.gketch.forge.ui.library.formatDuration
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeGraphite
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private enum class GestureKind { Seek, Volume, Brightness, Dismiss }

private const val EDGE_FRACTION = 0.20f
private const val DOUBLE_TAP_THIRD = 1f / 3f
/** Gesture brightness spans 0..2 (0%..200%); full-height swipe covers the range. */
private const val BRIGHTNESS_SPAN = BrightnessStore.MAX
/** Gesture volume spans 0..2 (0%..200%); above 1.0 continues into loudness boost. */
private const val VOLUME_SPAN = 2f

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
    /** Pinch zoom scale change (multiplicative) + pan delta. */
    onTransformZoomPan: ((zoomChange: Float, pan: Offset) -> Unit)? = null,
    onZoomReset: (() -> Unit)? = null,
    currentZoom: () -> Float = { 1f },
    gesturesEnabled: Boolean = true,
    controlsVisible: Boolean = false,
    excludeTopPx: Float = 0f,
    excludeBottomPx: Float = 0f,
    sensitivityMultiplier: Float = 1f,
    /** When false, brightness / volume / seek swipes are disabled (tap/hold/pinch still work). */
    swipeGesturesEnabled: Boolean = true,
    /** Swap left/right: brightness ↔ volume. */
    invertGestureSides: Boolean = false,
    holdSpeedLabel: String = "2×",
    swipeDownToClose: Boolean = true,
    onSwipeDownClose: (() -> Unit)? = null,
    doubleTapToLock: Boolean = false,
    onDoubleTapLock: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val durationState = rememberUpdatedState(durationMs)
    val positionState = rememberUpdatedState(positionMs)
    val volumeState = rememberUpdatedState(currentVolume)
    val brightnessState = rememberUpdatedState(currentBrightness)
    val zoomState = rememberUpdatedState(currentZoom)
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
    val zoomPanCb = rememberUpdatedState(onTransformZoomPan)
    val zoomResetCb = rememberUpdatedState(onZoomReset)

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
    val swipeState = rememberUpdatedState(swipeGesturesEnabled)
    val invertState = rememberUpdatedState(invertGestureSides)
    val holdLabelState = rememberUpdatedState(holdSpeedLabel)
    val swipeDownState = rememberUpdatedState(swipeDownToClose)
    val swipeDownCb = rememberUpdatedState(onSwipeDownClose)
    val doubleTapLockState = rememberUpdatedState(doubleTapToLock)
    val doubleTapLockCb = rememberUpdatedState(onDoubleTapLock)

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
                        // Zoomed: double-tap resets zoom/pan (any zone).
                        if (zoomState.value() > 1.01f) {
                            zoomResetCb.value?.invoke()
                            return@detectTapGestures
                        }
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
                            else -> {
                                if (doubleTapLockState.value && doubleTapLockCb.value != null) {
                                    doubleTapLockCb.value?.invoke()
                                } else {
                                    tapState.value()
                                }
                            }
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
            // Pinch-to-zoom (2-finger only) — must not steal 1-finger seek/brightness/volume.
            .pointerInput(gesturesEnabled) {
                if (!gesturesEnabled || zoomPanCb.value == null) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pastSlop = false
                    var lastCentroid: Offset? = null
                    var lastSpan = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size < 2) {
                            // Allow 1-finger pan only while already zoomed.
                            if (pressed.size == 1 && zoomState.value() > 1.01f && pastSlop) {
                                val pan = pressed[0].positionChange()
                                if (pan != Offset.Zero) {
                                    pressed[0].consume()
                                    runCatching { zoomPanCb.value?.invoke(1f, pan) }
                                }
                            }
                            if (pressed.isEmpty()) break
                            lastCentroid = null
                            lastSpan = 0f
                            continue
                        }
                        if (!enabledState.value) break
                        val c = Offset(
                            pressed.map { it.position.x }.average().toFloat(),
                            pressed.map { it.position.y }.average().toFloat(),
                        )
                        var span = 0f
                        for (p in pressed) {
                            val dx = p.position.x - c.x
                            val dy = p.position.y - c.y
                            span += sqrt(dx * dx + dy * dy)
                        }
                        span /= pressed.size
                        val prevC = lastCentroid
                        val prevSpan = lastSpan
                        if (prevC != null && prevSpan > 0.01f) {
                            val zoomChange = (span / prevSpan).coerceIn(0.5f, 2f)
                            val pan = c - prevC
                            if (kotlin.math.abs(zoomChange - 1f) > 0.001f || pan.getDistance() > 0.5f) {
                                pastSlop = true
                                pressed.fastForEach { it.consume() }
                                runCatching { zoomPanCb.value?.invoke(zoomChange, pan) }
                            }
                        }
                        lastCentroid = c
                        lastSpan = span
                        if (pressed.isEmpty()) break
                    }
                }
            }
            .pointerInput(
                gesturesEnabled,
                controlsVisible,
                excludeTopPx,
                excludeBottomPx,
                sensitivityMultiplier,
                swipeGesturesEnabled,
                invertGestureSides,
                swipeDownToClose,
            ) {
                if (!gesturesEnabled || !swipeState.value) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val start = down.position
                    if (inChrome(start.y, size.height.toFloat())) {
                        return@awaitEachGesture
                    }
                    // When zoomed, leave 1-finger drag to transform pan (above); skip edge/seek.
                    if (zoomState.value() > 1.01f) {
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
                    val invert = invertState.value

                    drag(down.id) { change ->
                        val delta = change.positionChange()
                        total += delta
                        if (classified == null && (abs(total.x) > slop || abs(total.y) > slop)) {
                            classified = when {
                                abs(total.x) >= abs(total.y) -> {
                                    if (start.x in leftEdge..rightEdge) GestureKind.Seek else null
                                }
                                start.x <= leftEdge -> if (invert) GestureKind.Volume else GestureKind.Brightness
                                start.x >= rightEdge -> if (invert) GestureKind.Brightness else GestureKind.Volume
                                // Center vertical: swipe-down closes (never steals edge brightness/volume).
                                // Require clear vertical dominance so slight diagonal seek doesn't dismiss.
                                swipeDownState.value && swipeDownCb.value != null &&
                                    total.y > slop && abs(total.y) > abs(total.x) * 1.15f -> GestureKind.Dismiss
                                else -> null
                            }
                            startVol = volumeState.value().coerceIn(0f, VOLUME_SPAN)
                            startBrit = brightnessState.value().coerceIn(BrightnessStore.MIN, BrightnessStore.MAX)
                            startPos = positionState.value
                            kind = classified
                            // Avoid SeekHud jumping to 0 before first drag delta.
                            if (classified == GestureKind.Seek) previewMs = startPos
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
                                // Full-height swipe covers 0..200% (system 0..100, then loudness boost).
                                val next = (
                                    startVol - (total.y / size.height) * sens * VOLUME_SPAN
                                    ).coerceIn(0f, VOLUME_SPAN)
                                barFraction = next
                                volCb.value(next)
                            }
                            GestureKind.Brightness -> {
                                val sens = sensState.value.coerceIn(0.25f, 3f)
                                // Full-height swipe covers 0..200% (span = 2.0).
                                val next = (
                                    startBrit - (total.y / size.height) * sens * BRIGHTNESS_SPAN
                                    ).coerceIn(BrightnessStore.MIN, BrightnessStore.MAX)
                                barFraction = next
                                britCb.value(next)
                            }
                            GestureKind.Dismiss -> Unit
                            null -> Unit
                        }
                    }

                    if (classified == GestureKind.Seek) {
                        seekState.value(previewMs)
                    }
                    if (classified == GestureKind.Dismiss) {
                        val threshold = size.height * 0.18f
                        if (total.y >= threshold) {
                            runCatching { swipeDownCb.value?.invoke() }
                        }
                    }
                    kind = null
                }
            },
    ) {
        when (kind) {
            GestureKind.Seek -> SeekHud(previewMs = previewMs, fromMs = positionMs, totalMs = durationMs)
            GestureKind.Dismiss -> DismissHud()
            GestureKind.Volume -> SideHud(
                icon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White) },
                displayPercent = (barFraction * 100).toInt().coerceIn(0, 200),
                barFill = (barFraction / VOLUME_SPAN).coerceIn(0f, 1f),
                alignment = if (invertGestureSides) Alignment.CenterStart else Alignment.CenterEnd,
            )
            GestureKind.Brightness -> SideHud(
                icon = { Icon(Icons.Rounded.BrightnessHigh, null, tint = Color.White) },
                displayPercent = (barFraction * 100).toInt().coerceIn(1, 200),
                barFill = (barFraction / BRIGHTNESS_SPAN).coerceIn(0f, 1f),
                alignment = if (invertGestureSides) Alignment.CenterEnd else Alignment.CenterStart,
            )
            null -> Unit
        }
        when (doubleTapFlash) {
            true -> DoubleTapHud(back = true, seconds = seekSecState.value)
            false -> DoubleTapHud(back = false, seconds = seekSecState.value)
            null -> Unit
        }
        if (holdSpeedActive) {
            HoldSpeedHud(label = holdLabelState.value)
        }
    }
}

@Composable
private fun DismissHud() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        RowHud {
            Text("↓", style = MaterialTheme.typography.titleLarge, color = ForgeAccent)
            Spacer(Modifier.height(4.dp))
            Text("Close", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun HoldSpeedHud(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        RowHud {
            Icon(Icons.Rounded.Speed, null, tint = ForgeAccent)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
private fun SeekHud(previewMs: Long, fromMs: Long, totalMs: Long = 0L) {
    val delta = previewMs - fromMs
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (delta >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = ForgeAccent,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (totalMs > 0L) {
                    "${formatDuration(previewMs)} / ${formatDuration(totalMs)}"
                } else {
                    formatDuration(previewMs)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = (if (delta >= 0) "+" else "−") + formatDuration(abs(delta)),
                style = MaterialTheme.typography.labelMedium,
                color = ForgeAccent,
            )
        }
    }
}

@Composable
private fun SideHud(
    icon: @Composable () -> Unit,
    displayPercent: Int,
    barFill: Float,
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
                        .fillMaxHeight(barFill.coerceIn(0f, 1f))
                        .background(ForgeAccent),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$displayPercent%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
