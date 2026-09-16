package com.gketch.forge.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Lightweight second ExoPlayer used only for hard realtime scrub preview.
 * Does not attach ForgeEngine / EQ / audio-focus — main PlaybackService player stays authoritative.
 */
@UnstableApi
object ForgeScrubPlayerFactory {
    fun create(context: Context): ExoPlayer {
        val prefs = ForgePlayerPrefs.snapshot
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 500,
                /* maxBufferMs = */ 3_000,
                /* bufferForPlaybackMs = */ 100,
                /* bufferForPlaybackAfterRebufferMs = */ 250,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = ForgeRenderersFactory(context.applicationContext, prefs.decoder)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ForgeStreamOptions.userAgent)
            .setConnectTimeoutMs(ForgeStreamOptions.connectTimeoutMs)
            .setReadTimeoutMs(ForgeStreamOptions.readTimeoutMs)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context.applicationContext, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context.applicationContext)
            .setDataSourceFactory(dataSourceFactory)

        return ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(false)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .build()
            .apply {
                playWhenReady = false
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                try {
                    setSeekParameters(SeekParameters.PREVIOUS_SYNC)
                } catch (_: Throwable) {
                    runCatching { setSeekParameters(SeekParameters.CLOSEST_SYNC) }
                }
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            }
    }
}
