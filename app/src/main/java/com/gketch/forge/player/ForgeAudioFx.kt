package com.gketch.forge.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import java.util.concurrent.atomic.AtomicReference

/**
 * Session-scoped BassBoost + Virtualizer, attached next to [ForgeEqualizer].
 */
object ForgeAudioFx {
    private val bassRef = AtomicReference<BassBoost?>(null)
    private val virtRef = AtomicReference<Virtualizer?>(null)
    @Volatile private var lastSessionId: Int = 0

    @Volatile var bassEnabled: Boolean = false
        private set
    @Volatile var virtualizerEnabled: Boolean = false
        private set

    /** 0..1000 per Android audiofx strength range. */
    @Volatile var bassStrength: Short = 750
        private set
    @Volatile var virtualizerStrength: Short = 750
        private set

    @Synchronized
    fun attach(sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == lastSessionId && bassRef.get() != null && virtRef.get() != null) {
            apply()
            return
        }
        release()
        try {
            val bass = BassBoost(0, sessionId)
            bass.setStrength(bassStrength)
            bass.enabled = bassEnabled
            bassRef.set(bass)
        } catch (_: Throwable) {
            bassRef.set(null)
        }
        try {
            val virt = Virtualizer(0, sessionId)
            virt.setStrength(virtualizerStrength)
            virt.enabled = virtualizerEnabled
            virtRef.set(virt)
        } catch (_: Throwable) {
            virtRef.set(null)
        }
        lastSessionId = sessionId
    }

    @Synchronized
    fun release() {
        try { bassRef.getAndSet(null)?.release() } catch (_: Throwable) {}
        try { virtRef.getAndSet(null)?.release() } catch (_: Throwable) {}
        lastSessionId = 0
    }

    fun setBassEnabled(on: Boolean) {
        bassEnabled = on
        try { bassRef.get()?.enabled = on } catch (_: Throwable) {}
    }

    fun setVirtualizerEnabled(on: Boolean) {
        virtualizerEnabled = on
        try { virtRef.get()?.enabled = on } catch (_: Throwable) {}
    }

    fun setBassStrength(value: Short) {
        bassStrength = value.coerceIn(0, 1000)
        try {
            bassRef.get()?.setStrength(bassStrength)
        } catch (_: Throwable) {
        }
    }

    fun setVirtualizerStrength(value: Short) {
        virtualizerStrength = value.coerceIn(0, 1000)
        try {
            virtRef.get()?.setStrength(virtualizerStrength)
        } catch (_: Throwable) {
        }
    }

    private fun apply() {
        try {
            bassRef.get()?.let {
                it.setStrength(bassStrength)
                it.enabled = bassEnabled
            }
        } catch (_: Throwable) {
        }
        try {
            virtRef.get()?.let {
                it.setStrength(virtualizerStrength)
                it.enabled = virtualizerEnabled
            }
        } catch (_: Throwable) {
        }
    }
}
