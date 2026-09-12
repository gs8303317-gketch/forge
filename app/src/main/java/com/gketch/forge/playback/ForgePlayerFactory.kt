package com.gketch.forge.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener

@UnstableApi
object ForgePlayerFactory {
    fun create(context: Context, prefs: EnginePrefs = ForgePlayerPrefs.snapshot): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                prefs.buffer.minMs,
                prefs.buffer.maxMs,
                prefs.buffer.playbackMs,
                prefs.buffer.rebufferMs,
            )
            .setBackBuffer(/* backBufferDurationMs = */ 30_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = ForgeRenderersFactory(context, prefs.decoder)

        val exo = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
                skipSilenceEnabled = prefs.skipSilence
                setSeekParameters(
                    if (prefs.preciseSeek) SeekParameters.EXACT else SeekParameters.DEFAULT,
                )
                addAnalyticsListener(object : AnalyticsListener {
                    override fun onAudioSessionIdChanged(
                        eventTime: AnalyticsListener.EventTime,
                        audioSessionId: Int,
                    ) {
                        ForgeEqualizer.attach(audioSessionId)
                        ForgeLoudness.attach(audioSessionId)
                    }
                })
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        ForgeEqualizer.attach(audioSessionId)
                        ForgeLoudness.attach(audioSessionId)
                    }
                })
            }

        ForgeEqualizer.attach(exo.audioSessionId)
        ForgeLoudness.attach(exo.audioSessionId)
        ForgeEngine.attach(exo)
        ForgeEngine.setAudioDelayMs(prefs.audioDelayMs)
        return exo
    }
}
