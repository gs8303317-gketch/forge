package com.gketch.forge.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.gketch.forge.R
import com.gketch.forge.widget.PlaybackWidgetState
import com.gketch.forge.widget.PlaybackWidgetUpdater

/**
 * Quick Settings play/pause tile for the current Forge playback session.
 */
class PlaybackTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        PlaybackWidgetUpdater.toggle(applicationContext)
        // Slight delay so controller state settles before icon update.
        qsTile?.let { refreshTile() }
        android.os.Handler(mainLooper).postDelayed({ refreshTile() }, 250)
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val playing = PlaybackWidgetState.isPlaying(applicationContext)
        val title = PlaybackWidgetState.title(applicationContext)
        tile.label = "Forge"
        tile.contentDescription = if (playing) "Pause Forge" else "Play Forge"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                title.isBlank() -> "Nothing playing"
                playing -> "Playing"
                else -> "Paused"
            }
        }
        tile.state = when {
            title.isBlank() -> Tile.STATE_INACTIVE
            playing -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.icon = Icon.createWithResource(
            this,
            if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
        )
        tile.updateTile()
    }
}
