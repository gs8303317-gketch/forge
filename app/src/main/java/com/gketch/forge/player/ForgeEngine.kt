package com.gketch.forge.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@UnstableApi
object ForgeEngine {
    private val playerRef = AtomicReference<ExoPlayer?>(null)
    private val audioDelayMsRef = AtomicInteger(0)
    private val scrubbingRef = AtomicBoolean(false)
    private val hideBufferUntilElapsed = AtomicLong(0L)

    val isScrubbing: Boolean get() = scrubbingRef.get()

    val hideBufferHud: Boolean
        get() = scrubbingRef.get() ||
            android.os.SystemClock.elapsedRealtime() < hideBufferUntilElapsed.get()

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

    fun setScrubSeek(enabled: Boolean) {
        scrubbingRef.set(enabled)
        if (enabled) {
            hideBufferUntilElapsed.set(Long.MAX_VALUE / 4)
        } else {
            hideBufferUntilElapsed.set(android.os.SystemClock.elapsedRealtime() + 700L)
        }
        val p = playerRef.get() ?: return
        try {
            p.setSeekParameters(
                if (enabled) androidx.media3.exoplayer.SeekParameters.PREVIOUS_SYNC
                else androidx.media3.exoplayer.SeekParameters.DEFAULT,
            )
        } catch (_: Throwable) {
            try {
                p.setSeekParameters(androidx.media3.exoplayer.SeekParameters.DEFAULT)
            } catch (_: Throwable) {
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
