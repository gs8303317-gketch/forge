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

    private fun applyLive(player: ExoPlayer) {
        val prefs = ForgePlayerPrefs.snapshot
        player.skipSilenceEnabled = prefs.skipSilence
        player.setSeekParameters(
            if (prefs.preciseSeek) androidx.media3.exoplayer.SeekParameters.EXACT
            else androidx.media3.exoplayer.SeekParameters.DEFAULT,
        )
        audioDelayMsRef.set(prefs.audioDelayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS))
    }

    const val MIN_DELAY_MS = -5000
    const val MAX_DELAY_MS = 5000
}
