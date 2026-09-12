package com.gketch.forge.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.gketch.forge.MainActivity
import com.gketch.forge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        // Warm prefs snapshot before building the player (decoder/load-control are create-time).
        runBlocking {
            runCatching {
                ForgePlayerPrefsStore(this@PlaybackService).prefs.first()
            }
        }

        val exo = ForgePlayerFactory.create(this, ForgePlayerPrefs.snapshot)
        exoPlayer = exo

        scope.launch {
            ForgePlayerPrefsStore(this@PlaybackService).prefs.collect { prefs ->
                // Live-apply knobs that do not require recreating renderers/load control.
                ForgeEngine.setAudioDelayMs(prefs.audioDelayMs)
                ForgeEngine.setSkipSilence(prefs.skipSilence)
                ForgeEngine.setPreciseSeek(prefs.preciseSeek)
            }
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.playback_channel)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, exo)
            .setId("forge")
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null ||
            !p.playWhenReady ||
            p.mediaItemCount == 0 ||
            p.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        ForgeLoudness.release()
        ForgeEqualizer.release()
        exoPlayer?.let { ForgeEngine.detach(it) }
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }
}
