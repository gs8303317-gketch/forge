package com.gketch.forge.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

/**
 * Soft volume ramp between consecutive playlist items (single-player approximation).
 * True overlapping decode crossfade is not available in Media3 without dual players —
 * this fades out near the end and fades in after transition.
 *
 * Optional: never take down playback if volume writes fail.
 */
@UnstableApi
object ForgeCrossfade {
    private val durationMsRef = AtomicInteger(0)
    @Volatile private var baseVolume: Float = 1f
    @Volatile private var fadingIn: Boolean = false
    @Volatile private var fadeInStartedAt: Long = 0L

    fun setDurationSec(sec: Int) {
        durationMsRef.set(
            when (sec) {
                1, 2, 3 -> sec * 1000
                else -> 0
            },
        )
    }

    val durationMs: Int get() = durationMsRef.get()

    fun onMediaItemTransition(player: Player) {
        try {
            if (durationMsRef.get() <= 0) {
                player.volume = baseVolume.coerceIn(0f, 1f)
                fadingIn = false
                return
            }
            fadingIn = true
            fadeInStartedAt = System.currentTimeMillis()
            player.volume = 0f
        } catch (_: Throwable) {
            fadingIn = false
        }
    }

    fun tick(player: Player, positionMs: Long, durationMsMedia: Long, sleepFading: Boolean) {
        if (sleepFading) return
        val cf = durationMsRef.get()
        if (cf <= 0) return
        try {
            val now = System.currentTimeMillis()
            if (fadingIn) {
                val elapsed = (now - fadeInStartedAt).toInt()
                val t = min(1f, elapsed.toFloat() / cf.toFloat())
                player.volume = (baseVolume * t).coerceIn(0f, 1f)
                if (t >= 1f) fadingIn = false
                return
            }
            if (durationMsMedia > 0 && positionMs > 0) {
                val remaining = durationMsMedia - positionMs
                if (remaining in 1 until cf.toLong()) {
                    val t = remaining.toFloat() / cf.toFloat()
                    player.volume = (baseVolume * t).coerceIn(0f, 1f)
                    return
                }
            }
            if (!fadingIn) {
                val v = player.volume
                if (v > 0.05f) baseVolume = max(baseVolume, v.coerceAtMost(1f))
                if (kotlin.math.abs(player.volume - baseVolume) > 0.02f) {
                    player.volume = baseVolume.coerceIn(0f, 1f)
                }
            }
        } catch (_: Throwable) {
            fadingIn = false
        }
    }

    fun rememberBaseVolume(volume: Float) {
        if (volume > 0.05f) baseVolume = volume.coerceIn(0.05f, 1f)
    }
}
