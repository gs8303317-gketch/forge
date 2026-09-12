package com.gketch.forge.playback

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.TextureView
import android.view.View
import androidx.media3.ui.PlayerView
import java.util.concurrent.atomic.AtomicReference

/**
 * Brightness / contrast / saturation applied via ColorMatrix on PlayerView's TextureView.
 * Values: brightness -1..1 (0 = neutral), contrast 0.5..2 (1 = neutral), saturation 0..2 (1 = neutral).
 *
 * Optional: failures must not affect playback. Avoid re-applying identical state every frame
 * (PlayerScreen recomposes on position ticks).
 */
object ForgeVideoColor {
    data class Adjust(
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
    ) {
        val isNeutral: Boolean
            get() = brightness == 0f && contrast == 1f && saturation == 1f
    }

    private val adjustRef = AtomicReference(Adjust())
    private val NEUTRAL_TAG = Any()

    val current: Adjust get() = adjustRef.get()

    fun set(brightness: Float, contrast: Float, saturation: Float) {
        adjustRef.set(
            Adjust(
                brightness = brightness.coerceIn(-1f, 1f),
                contrast = contrast.coerceIn(0.5f, 2f),
                saturation = saturation.coerceIn(0f, 2f),
            ),
        )
    }

    fun reset() = set(0f, 1f, 1f)

    fun buildMatrix(adj: Adjust = current): ColorMatrix {
        val b = adj.brightness
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
