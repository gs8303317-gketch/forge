package com.gketch.forge.widget

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.gketch.forge.tile.PlaybackTileService

object PlaybackWidgetState {
    private const val PREFS = "forge_widget"
    private const val KEY_TITLE = "title"
    private const val KEY_PLAYING = "playing"

    fun title(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TITLE, "") ?: ""

    fun isPlaying(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PLAYING, false)

    suspend fun publish(context: Context, title: String?, playing: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TITLE, title.orEmpty())
            .putBoolean(KEY_PLAYING, playing)
            .apply()
        PlaybackWidget().updateAll(context)
        runCatching {
            TileService.requestListeningState(
                context,
                ComponentName(context, PlaybackTileService::class.java),
            )
        }
    }
}
