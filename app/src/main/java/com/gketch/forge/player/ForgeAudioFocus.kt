package com.gketch.forge.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.gketch.forge.data.AudioFocusBehavior

/**
 * Pause vs Duck other apps when Forge holds audio focus.
 * Pause (default): ExoPlayer requests AUDIOFOCUS_GAIN (others pause).
 * Duck: Forge requests AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK (others should duck).
 */
@UnstableApi
object ForgeAudioFocus {
    @Volatile
    var behavior: AudioFocusBehavior = AudioFocusBehavior.PAUSE

    @Volatile
    private var duckRequest: AudioFocusRequest? = null

    @Volatile
    private var duckAm: AudioManager? = null

    @Volatile
    private var playerRef: ExoPlayer? = null

    private val duckListener = AudioManager.OnAudioFocusChangeListener { change ->
        val p = playerRef ?: return@OnAudioFocusChangeListener
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> runCatching { if (p.playWhenReady) p.pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> runCatching {
                p.volume = 0.25f
            }
            AudioManager.AUDIOFOCUS_GAIN -> runCatching {
                p.volume = 1f
            }
        }
    }

    fun attach(player: ExoPlayer) {
        playerRef = player
        apply(player)
    }

    fun apply(player: ExoPlayer, context: Context? = null) {
        playerRef = player
        when (behavior) {
            AudioFocusBehavior.PAUSE -> {
                abandonDuck(context)
                runCatching {
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        /* handleAudioFocus = */ true,
                    )
                }
            }
            AudioFocusBehavior.DUCK -> {
                runCatching {
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        /* handleAudioFocus = */ false,
                    )
                }
                val ctx = context ?: return
                ensureDuckFocus(ctx, player)
            }
        }
    }

    fun onPlayWhenReady(context: Context, player: ExoPlayer, playWhenReady: Boolean) {
        if (behavior != AudioFocusBehavior.DUCK) return
        if (playWhenReady) ensureDuckFocus(context, player) else abandonDuck(context)
    }

    private fun ensureDuckFocus(context: Context, player: ExoPlayer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return
        duckAm = am
        playerRef = player
        if (duckRequest != null) return
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build(),
            )
            .setOnAudioFocusChangeListener(duckListener)
            .setWillPauseWhenDucked(false)
            .build()
        val ok = runCatching { am.requestAudioFocus(req) }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        if (ok == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            duckRequest = req
        }
    }

    private fun abandonDuck(context: Context?) {
        val req = duckRequest ?: return
        val am = duckAm
            ?: context?.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { am?.abandonAudioFocusRequest(req) }
        }
        duckRequest = null
    }

    fun release(context: Context?) {
        abandonDuck(context)
        playerRef = null
    }
}
