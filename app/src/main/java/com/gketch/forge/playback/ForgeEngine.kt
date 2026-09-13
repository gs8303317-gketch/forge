package com.gketch.forge.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Process-local bridge from UI (MediaController) to ExoPlayer-only engine knobs.
 * MediaController cannot configure AudioSink / video timestamp adjustment / skip-silence.
 */
@UnstableApi
object ForgeEngine {
    private val playerRef = AtomicReference<ExoPlayer?>(null)
    private val audioDelayMsRef = AtomicInteger(0)

    val audioDelayMs: Int get() = audioDelayMsRef.get()

    fun attach(player: ExoPlayer) {
        playerRef.set(player)
        applyLive(player)
    }

    fun detach(player: ExoPlayer) {
        playerRef.compareAndSet(player, null)
    }

    fun setAudioDelayMs(ms: Int) {
        audioDelayMsRef.set(ms.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS))
        // Video renderer reads audioDelayMs on each frame via getBufferTimestampAdjustmentUs.
    }

    fun setSkipSilence(enabled: Boolean) {
        playerRef.get()?.skipSilenceEnabled = enabled
    }

    fun setPreciseSeek(enabled: Boolean) {
        val p = playerRef.get() ?: return
        p.setSeekParameters(
            if (enabled) androidx.media3.exoplayer.SeekParameters.EXACT
            else androidx.media3.exoplayer.SeekParameters.DEFAULT,
        )
    }

    fun setPauseAtEndOfMediaItems(pause: Boolean) {
        playerRef.get()?.pauseAtEndOfMediaItems = pause
    }

    /**
     * Faster keyframe seeks while the user is scrubbing the main surface.
     * Prefer PREVIOUS_SYNC (preceding keyframe) for responsive preview on 720p/x265;
     * CLOSEST_SYNC as fallback. Restores precise-seek preference when [enabled] is false.
     * Stay on Media3 1.5.1 — scrubbingMode APIs need 1.8+ and risk cast/session churn.
     */
    fun setScrubSeek(enabled: Boolean) {
        val p = playerRef.get() ?: return
        try {
            if (enabled) {
                // Preceding keyframe is typically cheaper than bidirectional closest sync.
                p.setSeekParameters(androidx.media3.exoplayer.SeekParameters.PREVIOUS_SYNC)
            } else {
                applySeekPrefs(p)
            }
        } catch (_: Throwable) {
            try {
                if (enabled) {
                    p.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                } else {
                    applySeekPrefs(p)
                }
            } catch (_: Throwable) {
                // Optional — never fail playback.
            }
        }
    }

    private fun applySeekPrefs(player: ExoPlayer) {
        val prefs = ForgePlayerPrefs.snapshot
        player.setSeekParameters(
            if (prefs.preciseSeek) androidx.media3.exoplayer.SeekParameters.EXACT
            else androidx.media3.exoplayer.SeekParameters.DEFAULT,
        )
    }

    private fun applyLive(player: ExoPlayer) {
        val prefs = ForgePlayerPrefs.snapshot
        player.skipSilenceEnabled = prefs.skipSilence
        applySeekPrefs(player)
        audioDelayMsRef.set(prefs.audioDelayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS))
    }

    const val MIN_DELAY_MS = -5000
    const val MAX_DELAY_MS = 5000
}
