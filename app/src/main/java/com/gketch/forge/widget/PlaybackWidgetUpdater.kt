package com.gketch.forge.widget

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gketch.forge.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PlaybackWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun init(context: Context) {
        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                val listener = object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        push(app, player)
                    }
                }
                c.addListener(listener)
                push(app, c)
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun push(context: Context, player: Player) {
        val title = player.mediaMetadata.title?.toString()
            ?: player.currentMediaItem?.mediaMetadata?.title?.toString()
            ?: if (player.mediaItemCount > 0) "Now playing" else ""
        val playing = player.isPlaying
        scope.launch {
            PlaybackWidgetState.publish(context, title, playing)
        }
    }

    fun toggle(context: Context) {
        val c = controller
        if (c != null) {
            if (c.isPlaying) c.pause() else c.play()
            push(context, c)
            return
        }
        // Controller not ready — try again shortly after service bind
        Handler(Looper.getMainLooper()).postDelayed({
            controller?.let {
                if (it.isPlaying) it.pause() else it.play()
                push(context, it)
            }
        }, 400)
    }
}
