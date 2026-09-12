package com.gketch.forge.playback

import android.media.audiofx.LoudnessEnhancer
import java.util.concurrent.atomic.AtomicReference

/**
 * Volume boost above system 100% via [LoudnessEnhancer].
 * Gain is capped at [MAX_GAIN_MB] millibels for hearing safety.
 */
object ForgeLoudness {
    /** 12 dB ≈ 1200 mB — noticeable boost without extreme clipping risk. */
    const val MAX_GAIN_MB = 1200
    const val MAX_BOOST_PERCENT = 200 // UI: 100% = no boost, 200% = max

    private val enhancerRef = AtomicReference<LoudnessEnhancer?>(null)
    @Volatile private var lastSessionId: Int = 0
    @Volatile private var gainMb: Int = 0

    val currentGainMb: Int get() = gainMb

    /** 100 = unity, up to [MAX_BOOST_PERCENT]. */
    val boostPercent: Int
        get() = if (gainMb <= 0) 100
        else 100 + ((gainMb.toFloat() / MAX_GAIN_MB) * (MAX_BOOST_PERCENT - 100)).toInt()

    @Synchronized
    fun attach(sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == lastSessionId && enhancerRef.get() != null) {
            applyGain(gainMb)
            return
        }
        release()
        try {
            val enhancer = LoudnessEnhancer(sessionId)
            enhancer.enabled = gainMb > 0
            enhancer.setTargetGain(gainMb.coerceIn(0, MAX_GAIN_MB))
            enhancerRef.set(enhancer)
            lastSessionId = sessionId
        } catch (_: Exception) {
            enhancerRef.set(null)
            lastSessionId = 0
        }
    }

    @Synchronized
    fun release() {
        try {
            enhancerRef.getAndSet(null)?.release()
        } catch (_: Exception) {
        }
        lastSessionId = 0
    }

    fun setBoostPercent(percent: Int) {
        val p = percent.coerceIn(100, MAX_BOOST_PERCENT)
        val mb = if (p <= 100) 0
        else (((p - 100).toFloat() / (MAX_BOOST_PERCENT - 100)) * MAX_GAIN_MB).toInt()
        applyGain(mb)
    }

    fun applyGain(millibels: Int) {
        gainMb = millibels.coerceIn(0, MAX_GAIN_MB)
        try {
            val enhancer = enhancerRef.get() ?: return
            enhancer.setTargetGain(gainMb)
            enhancer.enabled = gainMb > 0
        } catch (_: Exception) {
        }
    }
}
