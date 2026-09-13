package com.gketch.forge.widget

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gketch.forge.player.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Glance widget bridge. Does **not** bind a MediaController from Application.onCreate —
 * that early bind was a 1.9.0 regressor (service start / wrong-thread / FGS).
 * Updates are pushed from [PlaybackService]; toggle binds on-demand on the main thread.
 */
object PlaybackWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val main = Handler(Looper.getMainLooper())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** No-op warm path — kept for ForgeApp call site compatibility. */
    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        // Intentionally empty: never start MediaSessionService from Application.onCreate.
    }

    fun publishFromPlayer(context: Context, player: Player) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { publishFromPlayer(context, player) }
            return
        }
        val title = player.mediaMetadata.title?.toString()
            ?: player.currentMediaItem?.mediaMetadata?.title?.toString()
            ?: if (player.mediaItemCount > 0) "Now playing" else ""
        val playing = player.isPlaying
        scope.launch {
            runCatching { PlaybackWidgetState.publish(context.applicationContext, title, playing) }
        }
    }

    fun toggle(context: Context) {
        val app = context.applicationContext
        val existing = controller
        if (existing != null) {
            main.post {
                runCatching {
                    if (existing.isPlaying) existing.pause() else existing.play()
                    publishFromPlayer(app, existing)
                }
            }
            return
        }
        ensureController(app) { c ->
            runCatching {
                if (c.isPlaying) c.pause() else c.play()
                publishFromPlayer(app, c)
            }
        }
    }

    private fun ensureController(app: Context, onReady: (MediaController) -> Unit) {
        controller?.let {
            onReady(it)
            return
        }
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                onReady(c)
            },
            ContextCompat.getMainExecutor(app),
        )
    }
}
