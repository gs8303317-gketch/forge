package com.gketch.forge.ui.player

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gketch.forge.playback.PlaybackService

@Composable
fun rememberPlayerController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(Unit) {
        val appContext = context.applicationContext
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                try {
                    controller = future.get()
                } catch (_: Exception) {
                    controller = null
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
        onDispose {
            MediaController.releaseFuture(future)
            controller = null
        }
    }
    return controller
}

fun Player.stopCompletely() {
    playWhenReady = false
    stop()
    clearMediaItems()
    // Tear down notification / foreground service (custom STOP), not just clear the playlist.
    (this as? MediaController)?.let { controller ->
        runCatching {
            controller.sendCustomCommand(PlaybackService.STOP_COMMAND, Bundle.EMPTY)
        }
    }
}
