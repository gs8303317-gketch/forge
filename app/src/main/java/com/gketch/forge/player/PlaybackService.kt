package com.gketch.forge.player

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.gketch.forge.MainActivity
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.widget.PlaybackWidgetUpdater
import com.gketch.forge.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.async
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
    private var noisyRegistered = false
    /** Covers enable-after-create when ExoPlayer was built with handleAudioBecomingNoisy=false. */
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            if (!ForgeHeadsetPause.enabled) return
            runCatching {
                val player = exoPlayer ?: return
                if (player.isPlaying || player.playWhenReady) player.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Warm prefs before building the player (decoder/load-control are create-time).
        // Parallel DataStore reads — do not serialize two cold starts on the main thread.
        runBlocking {
            val prefsJob = async(Dispatchers.IO) {
                runCatching { ForgePlayerPrefsStore(this@PlaybackService).prefs.first() }
            }
            val appJob = async(Dispatchers.IO) {
                runCatching {
                    val app = AppSettingsStore(this@PlaybackService).settings.first()
                    ForgeStreamOptions.update(app.streamUserAgent, app.streamTimeoutSec)
                    ForgeHeadsetPause.enabled = app.pauseOnHeadsetUnplug
                    ForgeAudioFocus.behavior = app.audioFocusBehavior
                }
            }
            prefsJob.await()
            appJob.await()
        }

        val exo = ForgePlayerFactory.create(this, ForgePlayerPrefs.snapshot)
        exoPlayer = exo
        exo.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    maybeStopWhenIdle()
                    PlaybackWidgetUpdater.publishFromPlayer(this@PlaybackService, exo)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    PlaybackWidgetUpdater.publishFromPlayer(this@PlaybackService, exo)
                    ForgeAudioFocus.onPlayWhenReady(this@PlaybackService, exo, exo.playWhenReady && isPlaying)
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    ForgeAudioFocus.onPlayWhenReady(this@PlaybackService, exo, playWhenReady)
                }

                override fun onMediaItemTransition(
                    mediaItem: androidx.media3.common.MediaItem?,
                    reason: Int,
                ) {
                    maybeStopWhenIdle()
                    PlaybackWidgetUpdater.publishFromPlayer(this@PlaybackService, exo)
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.containsAny(
                            Player.EVENT_MEDIA_METADATA_CHANGED,
                            Player.EVENT_TIMELINE_CHANGED,
                        )
                    ) {
                        PlaybackWidgetUpdater.publishFromPlayer(this@PlaybackService, exo)
                    }
                }
            },
        )

        scope.launch {
            ForgePlayerPrefsStore(this@PlaybackService).prefs.collect { prefs ->
                // Live-apply knobs that do not require recreating renderers/load control.
                ForgeEngine.setAudioDelayMs(prefs.audioDelayMs)
                ForgeEngine.setSkipSilence(prefs.skipSilence)
                ForgeEngine.setPreciseSeek(prefs.preciseSeek)
            }
        }
        scope.launch {
            AppSettingsStore(this@PlaybackService).settings.collect { app ->
                ForgeStreamOptions.update(app.streamUserAgent, app.streamTimeoutSec)
                ForgeHeadsetPause.enabled = app.pauseOnHeadsetUnplug
                ForgeAudioFocus.behavior = app.audioFocusBehavior
                ForgeAudioFocus.apply(exo, this@PlaybackService)
            }
        }
        registerNoisyReceiver()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationProvider = object : DefaultMediaNotificationProvider(this) {
            override fun getMediaButtons(
                session: MediaSession,
                playerCommands: Player.Commands,
                customLayout: ImmutableList<CommandButton>,
                showPauseButton: Boolean,
            ): ImmutableList<CommandButton> {
                val defaults = super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
                val alreadyHasStop = defaults.any { button ->
                    button.sessionCommand?.customAction == ACTION_STOP ||
                        button.playerCommand == Player.COMMAND_STOP
                }
                if (alreadyHasStop) return defaults
                return ImmutableList.builder<CommandButton>()
                    .addAll(defaults)
                    .add(stopButton())
                    .build()
            }
        }
        notificationProvider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, exo)
            .setId("forge")
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback())
            .build()
            .also { session ->
                session.setCustomLayout(listOf(stopButton()))
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WIDGET_TOGGLE -> {
                PlaybackWidgetUpdater.toggle(this)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null ||
            !p.playWhenReady ||
            p.mediaItemCount == 0 ||
            p.playbackState == Player.STATE_ENDED ||
            p.playbackState == Player.STATE_IDLE
        ) {
            stopPlaybackAndService()
        }
    }

    override fun onDestroy() {
        ForgeAudioFocus.release(this)
        unregisterNoisyReceiver()
        scope.cancel()
        ForgeLoudness.release()
        ForgeEqualizer.release()
        ForgeAudioFx.release()
        exoPlayer?.let { ForgeEngine.detach(it) }
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    private fun maybeStopWhenIdle() {
        val p = exoPlayer ?: return
        if (p.mediaItemCount == 0 && p.playbackState == Player.STATE_IDLE) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopPlaybackAndService() {
        exoPlayer?.run {
            playWhenReady = false
            stop()
            clearMediaItems()
        }
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(STOP_COMMAND)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(listOf(stopButton()))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_STOP) {
                stopPlaybackAndService()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

    }

    private fun registerNoisyReceiver() {
        if (noisyRegistered) return
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(becomingNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(becomingNoisyReceiver, filter)
            }
            noisyRegistered = true
        } catch (_: Throwable) {
            noisyRegistered = false
        }
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyRegistered) return
        runCatching { unregisterReceiver(becomingNoisyReceiver) }
        noisyRegistered = false
    }

    companion object {
        const val ACTION_STOP = "com.gketch.forge.STOP"
        const val ACTION_WIDGET_TOGGLE = "com.gketch.forge.WIDGET_TOGGLE"
        val STOP_COMMAND = SessionCommand(ACTION_STOP, Bundle.EMPTY)

        fun stopButton(): CommandButton =
            CommandButton.Builder(CommandButton.ICON_STOP)
                .setDisplayName("Stop")
                .setSessionCommand(STOP_COMMAND)
                .setIconResId(R.drawable.ic_stop)
                .setSlots(CommandButton.SLOT_OVERFLOW, CommandButton.SLOT_FORWARD_SECONDARY)
                .build()
    }
}
