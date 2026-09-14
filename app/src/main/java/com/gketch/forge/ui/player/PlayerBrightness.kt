package com.gketch.forge.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.media3.ui.PlayerView
import com.gketch.forge.data.BrightnessStore
import com.gketch.forge.player.ForgeVideoColor

internal object ForgeWindowBrightness {
    fun apply(context: Context?, fraction: Float) {
        val activity = unwrapActivity(context) ?: return
        val window = activity.window ?: return
        val next = fraction.coerceIn(BrightnessStore.MIN, BrightnessStore.MAX)
        val lp = window.attributes
        if (lp.screenBrightness == next) return
        lp.screenBrightness = next
        window.attributes = lp
    }

    fun clear(activity: Activity?) {
        val window = activity?.window ?: return
        val lp = window.attributes
        if (lp.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) return
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = lp
    }

    private fun unwrapActivity(context: Context?): Activity? {
        var cursor: Context? = context
        while (cursor is ContextWrapper) {
            if (cursor is Activity) return cursor
            cursor = cursor.baseContext
        }
        return cursor as? Activity
    }
}

internal fun applySurfaceBrightness(fraction: Float, playerView: PlayerView?) {
    runCatching {
        ForgeVideoColor.setSurfaceBoost(0f)
        ForgeVideoColor.applyTo(playerView)
        ForgeWindowBrightness.apply(playerView?.context, fraction)
    }
}
