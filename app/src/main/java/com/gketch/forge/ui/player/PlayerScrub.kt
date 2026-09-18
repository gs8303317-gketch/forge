package com.gketch.forge.ui.player

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.gketch.forge.player.ForgeEngine
import com.gketch.forge.ui.library.formatDuration
import kotlin.math.abs

@Composable
internal fun ScrubTimecodeHud(elapsedMs: Long, totalMs: Long) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            if (totalMs > 0L) {
                Text(
                    text = formatDuration(totalMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
    }
}

internal class ScrubHold {
    var active: Boolean = false
    var wasPlaying: Boolean = false
    var volume: Float = 1f
    var lastSeekMs: Long = -1L
    var lastSeekAt: Long = 0L
    var pendingMs: Long = -1L
    var lastSampleMs: Long = -1L
    var lastSampleAt: Long = 0L
    var fast: Boolean = false
}

internal data class ScrubPreviewDecision(
    val shouldSeek: Boolean,
    val fling: Boolean,
)

internal const val SCRUB_THROTTLE_MS = 180L
internal const val SCRUB_FAST_THROTTLE_MS = 280L
internal const val SCRUB_MIN_DELTA_MS = 400L
internal const val SCRUB_FORCE_DELTA_MS = 2_500L
internal const val SCRUB_FLING_MEDIA_PER_WALL = 8.0
internal const val SCRUB_FAST_MEDIA_PER_WALL = 3.0

internal fun decidePreviewSeek(
    targetMs: Long,
    hold: ScrubHold,
    nowElapsedRealtime: Long,
): ScrubPreviewDecision {
    val clamped = targetMs.coerceAtLeast(0L)
    hold.pendingMs = clamped

    var fling = false
    val sampleMs = hold.lastSampleMs
    val sampleAt = hold.lastSampleAt
    if (sampleMs >= 0L && nowElapsedRealtime > sampleAt) {
        val wall = (nowElapsedRealtime - sampleAt).coerceAtLeast(1L).toDouble()
        val velocity = abs(clamped - sampleMs).toDouble() / wall
        fling = velocity >= SCRUB_FLING_MEDIA_PER_WALL
        hold.fast = velocity >= SCRUB_FAST_MEDIA_PER_WALL
    }
    hold.lastSampleMs = clamped
    hold.lastSampleAt = nowElapsedRealtime

    val last = hold.lastSeekMs
    if (last < 0L) {
        return ScrubPreviewDecision(shouldSeek = true, fling = false)
    }

    val elapsed = nowElapsedRealtime - hold.lastSeekAt
    val delta = abs(clamped - last)
    val throttle = if (hold.fast || fling) SCRUB_FAST_THROTTLE_MS else SCRUB_THROTTLE_MS
    if (delta < SCRUB_MIN_DELTA_MS && elapsed < throttle) {
        return ScrubPreviewDecision(shouldSeek = false, fling = fling)
    }
    if (delta < SCRUB_FORCE_DELTA_MS && elapsed < throttle) {
        return ScrubPreviewDecision(shouldSeek = false, fling = fling)
    }
    return ScrubPreviewDecision(shouldSeek = true, fling = fling)
}

internal fun startVideoScrub(player: Player?, hold: ScrubHold) {
    if (player == null || hold.active) return
    hold.active = true
    hold.wasPlaying = player.isPlaying
    hold.volume = player.volume
    hold.lastSeekMs = -1L
    hold.pendingMs = -1L
    hold.lastSampleMs = -1L
    hold.lastSampleAt = 0L
    hold.fast = false
    if (hold.volume > 0f) runCatching { player.volume = 0f }
    if (hold.wasPlaying) runCatching { player.pause() }
    ForgeEngine.setScrubSeek(true)
}

internal fun previewSeekTo(player: Player?, target: Long, hold: ScrubHold) {
    if (player == null) return
    val now = SystemClock.elapsedRealtime()
    val decision = decidePreviewSeek(target, hold, now)
    if (!decision.shouldSeek) return
    val clamped = hold.pendingMs.coerceAtLeast(0L)
    if (hold.lastSeekMs >= 0L && player.playbackState == Player.STATE_BUFFERING) {
        hold.pendingMs = clamped
        return
    }
    hold.lastSeekMs = clamped
    hold.lastSeekAt = now
    hold.pendingMs = -1L
    runCatching { player.seekTo(clamped) }
}

internal fun finishVideoScrub(player: Player?, target: Long, hold: ScrubHold) {
    val clamped = target.coerceAtLeast(0L)
    if (player != null) {
        runCatching { player.seekTo(clamped) }
        runCatching { ForgeEngine.setScrubSeek(false) }
        if (hold.active) {
            runCatching { player.volume = hold.volume }
            if (hold.wasPlaying) runCatching { player.play() }
        }
    } else {
        runCatching { ForgeEngine.setScrubSeek(false) }
    }
    hold.active = false
    hold.lastSeekMs = -1L
    hold.pendingMs = -1L
    hold.lastSampleMs = -1L
    hold.lastSampleAt = 0L
    hold.fast = false
}
