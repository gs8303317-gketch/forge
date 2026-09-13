package com.gketch.forge.player

import android.media.audiofx.Equalizer
import java.util.concurrent.atomic.AtomicReference

data class EqBand(
    val index: Int,
    val frequencyMilliHz: Int,
    val levelMilliBel: Short,
    val minLevel: Short,
    val maxLevel: Short,
)

data class EqPreset(
    val name: String,
    val levels: List<Float>, // -1..1 normalized
)

object ForgeEqualizer {
    private val eqRef = AtomicReference<Equalizer?>(null)
    @Volatile private var lastSessionId: Int = 0
    @Volatile var enabled: Boolean = false
        private set
    @Volatile var presetName: String = "Custom"
        private set

    val presets = listOf(
        EqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
        EqPreset("Bass Boost", listOf(0.85f, 0.55f, 0.1f, 0f, 0f)),
        EqPreset("Treble Boost", listOf(0f, 0f, 0.15f, 0.55f, 0.85f)),
        EqPreset("Vocal", listOf(-0.2f, 0.15f, 0.7f, 0.35f, 0.1f)),
        EqPreset("Rock", listOf(0.6f, 0.35f, -0.15f, 0.35f, 0.55f)),
        EqPreset("Electronic", listOf(0.7f, 0.4f, 0f, 0.3f, 0.65f)),
    )

    @Synchronized
    fun attach(sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == lastSessionId && eqRef.get() != null) return
        release()
        try {
            val eq = Equalizer(0, sessionId)
            eq.enabled = enabled
            eqRef.set(eq)
            lastSessionId = sessionId
        } catch (_: Throwable) {
            eqRef.set(null)
            lastSessionId = 0
        }
    }

    @Synchronized
    fun release() {
        try {
            eqRef.getAndSet(null)?.release()
        } catch (_: Throwable) {
        }
        lastSessionId = 0
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        try {
            eqRef.get()?.enabled = on
        } catch (_: Throwable) {
        }
    }

    fun bandCount(): Int = try {
        eqRef.get()?.numberOfBands?.toInt() ?: 0
    } catch (_: Throwable) {
        0
    }

    fun bands(): List<EqBand> {
        val eq = eqRef.get() ?: return emptyList()
        return try {
            val range = eq.bandLevelRange
            val min = range[0]
            val max = range[1]
            (0 until eq.numberOfBands).map { i ->
                val idx = i.toShort()
                EqBand(
                    index = i,
                    frequencyMilliHz = eq.getCenterFreq(idx),
                    levelMilliBel = eq.getBandLevel(idx),
                    minLevel = min,
                    maxLevel = max,
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun setBandNormalized(index: Int, normalized: Float) {
        val eq = eqRef.get() ?: return
        try {
            val range = eq.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            val mid = (min + max) / 2
            val half = ((max - min) / 2).coerceAtLeast(1)
            val level = (mid + (normalized.coerceIn(-1f, 1f) * half)).toInt()
                .coerceIn(min, max).toShort()
            eq.setBandLevel(index.toShort(), level)
            if (!eq.enabled && enabled) eq.enabled = true
            presetName = "Custom"
        } catch (_: Throwable) {
        }
    }

    fun bandNormalized(band: EqBand): Float {
        val mid = (band.minLevel + band.maxLevel) / 2f
        val half = ((band.maxLevel - band.minLevel) / 2f).coerceAtLeast(1f)
        return ((band.levelMilliBel - mid) / half).coerceIn(-1f, 1f)
    }

    fun applyPreset(preset: EqPreset) {
        val count = bandCount().coerceAtLeast(1)
        val levels = if (preset.levels.size == count) {
            preset.levels
        } else {
            // resample preset levels to device band count
            (0 until count).map { i ->
                val t = if (count == 1) 0f else i.toFloat() / (count - 1)
                val src = t * (preset.levels.lastIndex).coerceAtLeast(0)
                val lo = src.toInt().coerceIn(0, preset.levels.lastIndex)
                val hi = (lo + 1).coerceAtMost(preset.levels.lastIndex)
                val frac = src - lo
                preset.levels[lo] * (1 - frac) + preset.levels[hi] * frac
            }
        }
        levels.forEachIndexed { index, value -> setBandNormalized(index, value) }
        presetName = preset.name
        setEnabled(true)
    }

    fun frequencyLabel(milliHz: Int): String {
        val hz = milliHz / 1000
        return if (hz >= 1000) {
            val k = hz / 1000f
            if (k == k.toInt().toFloat()) "${k.toInt()}k" else "%.1fk".format(k)
        } else {
            "${hz}Hz"
        }
    }
}
