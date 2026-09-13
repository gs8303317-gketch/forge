package com.gketch.forge.player

import android.media.audiofx.LoudnessEnhancer
import java.util.concurrent.atomic.AtomicReference

/**
 * Volume boost above system 100% via [LoudnessEnhancer].
 * Optional peak-normalize applies a modest fixed gain (ReplayGain-style approximation)
 * when track ReplayGain tags are unavailable — capped for safety.
 */
object ForgeLoudness {
    /** 12 dB ≈ 1200 mB — noticeable boost without extreme clipping risk. */
    const val MAX_GAIN_MB = 1200
    const val MAX_BOOST_PERCENT = 200 // UI: 100% = no boost, 200% = max
    /** Safe normalize target ≈ +6 dB when boost is at unity. */
    const val NORMALIZE_GAIN_MB = 600

    private val enhancerRef = AtomicReference<LoudnessEnhancer?>(null)
    @Volatile private var lastSessionId: Int = 0
    @Volatile private var boostMb: Int = 0
    @Volatile private var normalizeEnabled: Boolean = false

    val currentGainMb: Int get() = effectiveGainMb()

    /** 100 = unity, up to [MAX_BOOST_PERCENT]. */
    val boostPercent: Int
        get() = if (boostMb <= 0) 100
        else 100 + ((boostMb.toFloat() / MAX_GAIN_MB) * (MAX_BOOST_PERCENT - 100)).toInt()

    val isNormalizeEnabled: Boolean get() = normalizeEnabled

    @Synchronized
    fun attach(sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == lastSessionId && enhancerRef.get() != null) {
            applyEffective()
            return
        }
        release()
        try {
            val enhancer = LoudnessEnhancer(sessionId)
            val gain = effectiveGainMb()
            enhancer.enabled = gain > 0
            enhancer.setTargetGain(gain)
            enhancerRef.set(enhancer)
            lastSessionId = sessionId
        } catch (_: Throwable) {
            enhancerRef.set(null)
            lastSessionId = 0
        }
    }

    @Synchronized
    fun release() {
        try {
            enhancerRef.getAndSet(null)?.release()
        } catch (_: Throwable) {
        }
        lastSessionId = 0
    }

    fun setBoostPercent(percent: Int) {
        val p = percent.coerceIn(100, MAX_BOOST_PERCENT)
        boostMb = if (p <= 100) 0
        else (((p - 100).toFloat() / (MAX_BOOST_PERCENT - 100)) * MAX_GAIN_MB).toInt()
        applyEffective()
    }

    fun setNormalizeEnabled(enabled: Boolean) {
        normalizeEnabled = enabled
        applyEffective()
    }

    fun applyGain(millibels: Int) {
        boostMb = millibels.coerceIn(0, MAX_GAIN_MB)
        applyEffective()
    }

    private fun effectiveGainMb(): Int {
        val norm = if (normalizeEnabled) NORMALIZE_GAIN_MB else 0
        return (boostMb + norm).coerceIn(0, MAX_GAIN_MB)
    }

    private fun applyEffective() {
        val gain = effectiveGainMb()
        try {
            val enhancer = enhancerRef.get() ?: return
            enhancer.setTargetGain(gain)
            enhancer.enabled = gain > 0
        } catch (_: Throwable) {
        }
    }
}
