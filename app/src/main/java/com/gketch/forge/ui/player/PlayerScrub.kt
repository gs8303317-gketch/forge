package com.gketch.forge.ui.player

import android.content.Context
import android.net.Uri
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.gketch.forge.player.ForgeEngine
import com.gketch.forge.player.ForgeScrubPlayerFactory
import com.gketch.forge.ui.library.formatDuration

internal const val SCRUB_SECOND_MS = 1_000L

internal fun quantizeToSecondMs(positionMs: Long): Long {
    val clamped = positionMs.coerceAtLeast(0L)
    return (clamped / SCRUB_SECOND_MS) * SCRUB_SECOND_MS
}

internal fun shouldPreviewSeekSecond(targetMs: Long, lastPreviewSecondMs: Long): Boolean {
    val second = quantizeToSecondMs(targetMs)
    return lastPreviewSecondMs < 0L || second != lastPreviewSecondMs
}

@Composable
internal fun ScrubTimecodeHud(elapsedMs: Long, totalMs: Long) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.70f))
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

@UnstableApi
internal class ScrubHold {
    var active: Boolean = false
    var wasPlaying: Boolean = false
    var volume: Float = 1f
    var lastPreviewSecondMs: Long = -1L
    var pendingMs: Long = -1L
    var mediaUri: String? = null
    var scrubPlayer: ExoPlayer? = null
}

@UnstableApi
internal fun ensureScrubPlayer(
    context: Context,
    hold: ScrubHold,
    mediaUri: Uri?,
    template: MediaItem?,
): ExoPlayer? {
    if (mediaUri == null) return hold.scrubPlayer
    val key = mediaUri.toString()
    val existing = hold.scrubPlayer
    if (existing != null && hold.mediaUri == key) return existing
    runCatching { existing?.release() }
    hold.scrubPlayer = null
    hold.mediaUri = null
    val created = runCatching { ForgeScrubPlayerFactory.create(context) }.getOrNull() ?: return null
    val item = when {
        template != null && template.localConfiguration?.uri == mediaUri -> template
        template != null -> template.buildUpon().setUri(mediaUri).build()
        else -> MediaItem.fromUri(mediaUri)
    }
    runCatching {
        created.setMediaItem(item)
        created.prepare()
        created.playWhenReady = false
        created.volume = 0f
    }.onFailure {
        runCatching { created.release() }
        return null
    }
    hold.scrubPlayer = created
    hold.mediaUri = key
    return created
}

@UnstableApi
internal fun startVideoScrub(
    main: Player?,
    hold: ScrubHold,
    context: Context,
    mediaUri: Uri?,
) {
    if (hold.active) return
    hold.active = true
    hold.wasPlaying = main?.isPlaying == true
    hold.volume = main?.volume?.takeIf { it > 0f } ?: hold.volume.coerceAtLeast(0.01f).coerceAtMost(1f)
    if (hold.volume <= 0f) hold.volume = 1f
    hold.lastPreviewSecondMs = -1L
    hold.pendingMs = -1L
    if (main != null) {
        if (main.volume > 0f) runCatching { main.volume = 0f }
        if (hold.wasPlaying) runCatching { main.pause() }
    }
    runCatching { ForgeEngine.setScrubSeek(true) }
    // Do not build a second ExoPlayer here. Watch / back stay on one decoder.
    // Preview seeks the main player (PREVIOUS_SYNC). Overlay factory remains if UI asks later.
    hold.mediaUri = mediaUri?.toString()
}

@UnstableApi
internal fun previewSeekTo(hold: ScrubHold, targetMs: Long, mainFallback: Player? = null) {
    val clamped = targetMs.coerceAtLeast(0L)
    hold.pendingMs = clamped
    if (!shouldPreviewSeekSecond(clamped, hold.lastPreviewSecondMs)) return
    val second = quantizeToSecondMs(clamped)
    hold.lastPreviewSecondMs = second
    val scrub = hold.scrubPlayer
    if (scrub != null) {
        runCatching {
            try {
                scrub.setSeekParameters(SeekParameters.PREVIOUS_SYNC)
            } catch (_: Throwable) {
                runCatching { scrub.setSeekParameters(SeekParameters.CLOSEST_SYNC) }
            }
            scrub.seekTo(second)
        }
        return
    }
    if (mainFallback != null) {
        runCatching { ForgeEngine.setScrubSeek(true) }
        runCatching { mainFallback.seekTo(second) }
    }
}

@UnstableApi
internal fun finishVideoScrub(main: Player?, hold: ScrubHold, targetMs: Long) {
    val settle = quantizeToSecondMs(targetMs.coerceAtLeast(0L))
    hold.pendingMs = settle
    if (main != null) {
        runCatching { main.seekTo(settle) }
        runCatching { ForgeEngine.setScrubSeek(false) }
        if (hold.active) {
            runCatching { main.volume = hold.volume }
            if (hold.wasPlaying) runCatching { main.play() }
        }
    } else {
        runCatching { ForgeEngine.setScrubSeek(false) }
    }
    hold.active = false
    hold.lastPreviewSecondMs = -1L
    hold.pendingMs = -1L
    val extra = hold.scrubPlayer
    hold.scrubPlayer = null
    hold.mediaUri = null
    runCatching { extra?.release() }
}

@UnstableApi
internal fun releaseScrubResources(hold: ScrubHold) {
    hold.active = false
    hold.lastPreviewSecondMs = -1L
    hold.pendingMs = -1L
    hold.mediaUri = null
    val p = hold.scrubPlayer
    hold.scrubPlayer = null
    runCatching { p?.release() }
    runCatching { ForgeEngine.setScrubSeek(false) }
}
