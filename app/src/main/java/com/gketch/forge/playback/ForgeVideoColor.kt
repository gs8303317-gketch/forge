package com.gketch.forge.playback

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.TextureView
import android.view.View
import androidx.media3.ui.PlayerView
import java.util.concurrent.atomic.AtomicReference

/**
 * Brightness / contrast / saturation / surface boost / night filter via ColorMatrix
 * on PlayerView's TextureView.
 *
 * Panel values: brightness -1..1 (0 = neutral), contrast 0.5..2 (1 = neutral),
 * saturation 0..2 (1 = neutral).
 * surfaceBoost: 0..1 extra brightness from gesture range above 100% (video-only).
 * nightStrength: 0..1 blue-light / warm night filter (video-only).
 *
 * Failures must not affect playback. Avoid re-applying identical state every frame.
 */
object ForgeVideoColor {
    data class Adjust(
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val surfaceBoost: Float = 0f,
        val nightStrength: Float = 0f,
    ) {
        val isNeutral: Boolean
            get() = brightness == 0f &&
                contrast == 1f &&
                saturation == 1f &&
                surfaceBoost == 0f &&
                nightStrength == 0f
    }

    private val adjustRef = AtomicReference(Adjust())
    private val NEUTRAL_TAG = Any()

    val current: Adjust get() = adjustRef.get()

    fun set(brightness: Float, contrast: Float, saturation: Float) {
        val cur = adjustRef.get()
        adjustRef.set(
            cur.copy(
                brightness = brightness.coerceIn(-1f, 1f),
                contrast = contrast.coerceIn(0.5f, 2f),
                saturation = saturation.coerceIn(0f, 2f),
            ),
        )
    }

    fun setSurfaceBoost(boost: Float) {
        val cur = adjustRef.get()
        val next = boost.coerceIn(0f, 1f)
        if (cur.surfaceBoost == next) return
        adjustRef.set(cur.copy(surfaceBoost = next))
    }

    fun setNightStrength(strength: Float) {
        val cur = adjustRef.get()
        val next = strength.coerceIn(0f, 1f)
        if (cur.nightStrength == next) return
        adjustRef.set(cur.copy(nightStrength = next))
    }

    fun reset() = adjustRef.set(Adjust())

    fun buildMatrix(adj: Adjust = current): ColorMatrix {
        val b = adj.brightness + adj.surfaceBoost
        val c = adj.contrast
        val s = adj.saturation
        val translate = (1f - c) / 2f * 255f + b * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        val satMatrix = ColorMatrix().apply { setSaturation(s) }
        satMatrix.postConcat(contrastMatrix)

        val night = adj.nightStrength
        if (night > 0.001f) {
            // Warm / blue-light filter: cut blue, slight red/green lift.
            val blueKeep = 1f - 0.72f * night
            val warmLift = 0.08f * night * 255f
            val nightMatrix = ColorMatrix(
                floatArrayOf(
                    1f + 0.06f * night, 0f, 0f, 0f, warmLift,
                    0f, 1f + 0.03f * night, 0f, 0f, warmLift * 0.5f,
                    0f, 0f, blueKeep, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            satMatrix.postConcat(nightMatrix)
        }
        return satMatrix
    }

    fun applyTo(playerView: PlayerView?) {
        if (playerView == null) return
        try {
            val surface = playerView.videoSurfaceView
            if (surface !is TextureView) return
            val adj = current
            if (adj.isNeutral) {
                if (surface.getTag(TAG_KEY) !== NEUTRAL_TAG) {
                    surface.setLayerType(View.LAYER_TYPE_NONE, null)
                    surface.setTag(TAG_KEY, NEUTRAL_TAG)
                }
            } else {
                val key = adj.hashCode()
                if (surface.getTag(TAG_KEY) == key) return
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(buildMatrix(adj))
                }
                surface.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                surface.setTag(TAG_KEY, key)
            }
        } catch (_: Throwable) {
            // Color matrix is optional — never fail the player surface.
        }
    }

    private val TAG_KEY = 0x46C0101 // forge color tag
}
