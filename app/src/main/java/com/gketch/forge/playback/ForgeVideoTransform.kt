package com.gketch.forge.playback

import java.util.concurrent.atomic.AtomicReference

/**
 * Mirror / rotate state for the player surface (Compose graphicsLayer).
 * Does not rebuild ExoPlayer — optional and crash-isolated at call sites.
 */
object ForgeVideoTransform {
    data class State(
        val mirrorH: Boolean = false,
        val mirrorV: Boolean = false,
        /** 0, 90, 180, 270 */
        val rotationDeg: Int = 0,
    ) {
        val isNeutral: Boolean
            get() = !mirrorH && !mirrorV && (rotationDeg % 360 == 0)
    }

    private val stateRef = AtomicReference(State())

    val current: State get() = stateRef.get()

    fun set(mirrorH: Boolean, mirrorV: Boolean, rotationDeg: Int) {
        stateRef.set(
            State(
                mirrorH = mirrorH,
                mirrorV = mirrorV,
                rotationDeg = ((rotationDeg % 360) + 360) % 360,
            ),
        )
    }

    fun toggleMirrorH() {
        val s = current
        set(!s.mirrorH, s.mirrorV, s.rotationDeg)
    }

    fun toggleMirrorV() {
        val s = current
        set(s.mirrorH, !s.mirrorV, s.rotationDeg)
    }

    fun rotateBy(delta: Int) {
        val s = current
        set(s.mirrorH, s.mirrorV, s.rotationDeg + delta)
    }

    fun reset() = set(false, false, 0)
}
