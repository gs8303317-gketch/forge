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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gketch.forge.data.BrightnessStore
import com.gketch.forge.ui.library.formatDuration
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal enum class GestureKind { Seek, Volume, Brightness, Dismiss }

/** Edge strips for brightness (left) / volume (right) — center reserved for seek + dismiss. */
internal const val EDGE_FRACTION = 0.22f
/** Left/right double-tap seek zones (center reserved for lock / chrome toggle). */
internal const val DOUBLE_TAP_ZONE = 0.33f
/** Swipe-down must clearly dominate horizontal so it never steals VLC-style seek. */
internal const val DISMISS_VERTICAL_RATIO = 2.75f
/** Fraction of height required before swipe-down closes (VLC-safe / less aggressive). */
internal const val DISMISS_HEIGHT_FRACTION = 0.30f
/** Vertical must beat horizontal by this ratio on edges before brightness/volume locks. */
internal const val EDGE_VERTICAL_RATIO = 1.15f
/** Hold-to-speed press timeout — snappy like VLC, still above tap. */
internal const val HOLD_SPEED_MS = 360L
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
            delay(380)
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
                        when (doubleTapSeekBack(offset.x, size.width.toFloat())) {
                            true -> {
                                doubleTapFlash = true
                                doubleTapState.value(true)
                            }
                            false -> {
                                doubleTapFlash = false
                                doubleTapState.value(false)
                            }
                            null -> {
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
                        val releasedEarly = withTimeoutOrNull(HOLD_SPEED_MS) {
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
                    // VLC-snappy: classify near system touch slop (not sticky 24px).
                    val slop = viewConfiguration.touchSlop.coerceIn(8f, 20f)
                    val invert = invertState.value

                    drag(down.id) { change ->
                        val delta = change.positionChange()
                        total += delta
                        if (classified == null && (abs(total.x) > slop || abs(total.y) > slop)) {
                            classified = classifySwipeGesture(
                                dx = total.x,
                                dy = total.y,
                                startX = start.x,
                                startY = start.y,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                slop = slop,
                                invertSides = invert,
                                swipeDownEnabled = swipeDownState.value && swipeDownCb.value != null,
                            )
                            if (classified != null) {
                                startVol = volumeState.value().coerceIn(0f, VOLUME_SPAN)
                                startBrit = brightnessState.value().coerceIn(BrightnessStore.MIN, BrightnessStore.MAX)
                                startPos = positionState.value
                                kind = classified
                                // Avoid SeekHud jumping to 0 before first drag delta.
                                if (classified == GestureKind.Seek) previewMs = startPos
                            }
                        }
                        if (classified != null) {
                            change.consume()
                        }
                        when (classified) {
                            GestureKind.Seek -> {
                                val sens = sensState.value.coerceIn(0.25f, 3f)
                                val window = if (dur > 0L) minOf(dur, 180_000L).toFloat() else 180_000f
                                // Axis-locked: horizontal only (ignore vertical wobble).
                                val deltaMs = ((total.x / size.width) * window * sens).roundToLong()
                                val target = (startPos + deltaMs).coerceIn(0L, if (dur > 0L) dur else Long.MAX_VALUE)
                                previewMs = target
                                previewState.value(target)
                            }
                            GestureKind.Volume -> {
                                val sens = sensState.value.coerceIn(0.25f, 3f)
                                // Axis-locked vertical; full-height covers 0..200%.
                                val next = (
                                    startVol - (total.y / size.height) * sens * VOLUME_SPAN
                                    ).coerceIn(0f, VOLUME_SPAN)
                                barFraction = next
                                volCb.value(next)
                            }
                            GestureKind.Brightness -> {
                                val sens = sensState.value.coerceIn(0.25f, 3f)
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
                        if (dismissShouldClose(total.y, total.x, size.height.toFloat())) {
                            runCatching { swipeDownCb.value?.invoke() }
                        }
                    }
                    kind = null
                }
            },
    ) {
        when (kind) {
            GestureKind.Seek -> SeekHud(previewMs = previewMs, fromMs = positionMs)
            GestureKind.Dismiss -> DismissHud()
            GestureKind.Volume -> SideHud(
                icon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
                displayPercent = (barFraction * 100).toInt().coerceIn(0, 200),
                barFill = (barFraction / VOLUME_SPAN).coerceIn(0f, 1f),
                alignment = if (invertGestureSides) Alignment.CenterStart else Alignment.CenterEnd,
            )
            GestureKind.Brightness -> SideHud(
                icon = { Icon(Icons.Rounded.BrightnessHigh, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
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
            Text("↓", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(2.dp))
            Text("Close", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun HoldSpeedHud(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        RowHud {
            Icon(Icons.Rounded.Speed, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
    }
}

@Composable
private fun RowHud(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (back) Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (back) "−${seconds}s" else "+${seconds}s",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

/** VLC-like seek overlay: icon + time + delta (no noisy total). */
@Composable
private fun SeekHud(previewMs: Long, fromMs: Long) {
    val delta = previewMs - fromMs
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.70f))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (delta >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDuration(previewMs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = (if (delta >= 0) "+" else "−") + formatDuration(abs(delta)),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
    }
}

/** VLC-like side HUD: icon + thin bar + percent. */
@Composable
private fun SideHud(
    icon: @Composable () -> Unit,
    displayPercent: Int,
    barFill: Float,
    alignment: Alignment,
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(barFill.coerceIn(0f, 1f))
                        .background(Color.White),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$displayPercent%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
    }
}

/** VLC-like swipe classification. Horizontal wins; edges need a vertical lock; dismiss is center-only. */
internal fun classifySwipeGesture(
    dx: Float,
    dy: Float,
    startX: Float,
    startY: Float,
    width: Float,
    height: Float,
    slop: Float,
    invertSides: Boolean,
    swipeDownEnabled: Boolean,
): GestureKind? {
    val ax = abs(dx)
    val ay = abs(dy)
    if (ax <= slop && ay <= slop) return null
    val leftEdge = width * EDGE_FRACTION
    val rightEdge = width * (1f - EDGE_FRACTION)
    return when {
        ax >= ay -> GestureKind.Seek
        startX <= leftEdge && ay > ax * EDGE_VERTICAL_RATIO ->
            if (invertSides) GestureKind.Volume else GestureKind.Brightness
        startX >= rightEdge && ay > ax * EDGE_VERTICAL_RATIO ->
            if (invertSides) GestureKind.Brightness else GestureKind.Volume
        swipeDownEnabled &&
            startX in leftEdge..rightEdge &&
            startY < height * 0.55f &&
            dy > slop &&
            ay > ax * DISMISS_VERTICAL_RATIO -> GestureKind.Dismiss
        else -> null
    }
}

internal fun dismissShouldClose(totalY: Float, totalX: Float, height: Float): Boolean {
    return totalY >= height * DISMISS_HEIGHT_FRACTION &&
        abs(totalY) > abs(totalX) * DISMISS_VERTICAL_RATIO
}

/** true = back/left, false = forward/right, null = center (chrome / lock). */
internal fun doubleTapSeekBack(x: Float, width: Float): Boolean? {
    val zone = width * DOUBLE_TAP_ZONE
    return when {
        x < zone -> true
        x > width - zone -> false
        else -> null
    }
}
