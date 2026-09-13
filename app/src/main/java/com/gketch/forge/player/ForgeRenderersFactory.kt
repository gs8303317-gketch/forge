package com.gketch.forge.player

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import java.util.ArrayList

/**
 * Media3 renderers with:
 * - Video timestamp shift for A/V audio-delay (clock stays on audio).
 * - Decoder preference via MediaCodecSelector (no FFmpeg .so bundled).
 * - ExtensionRendererMode OFF (extensions not shipped; ON/PREFER would no-op without .so).
 */
@UnstableApi
class ForgeRenderersFactory(
    context: Context,
    decoder: DecoderPreference,
) : DefaultRenderersFactory(context) {

    init {
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_OFF)
        setEnableDecoderFallback(true)
        setMediaCodecSelector(selectorFor(decoder))
    }

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>,
    ) {
        out.add(
            ForgeVideoRenderer(
                context,
                mediaCodecSelector,
                allowedVideoJoiningTimeMs,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY,
            ),
        )
        // Skip reflective extension video renderers — no libvpx/libgav1/FFmpeg shipped.
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        val builder = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        // Balance is optional — if processor wiring fails, ship a plain sink.
        try {
            builder.setAudioProcessors(arrayOf(ForgeBalance.processor()))
        } catch (_: Throwable) {
        }
        return builder.build()
    }

    companion object {
        fun selectorFor(decoder: DecoderPreference): MediaCodecSelector {
            return when (decoder) {
                DecoderPreference.AUTO -> MediaCodecSelector.DEFAULT
                DecoderPreference.HARDWARE -> MediaCodecSelector { mime, secure, tunneling ->
                    val all = MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)
                    val hw = all.filter { it.hardwareAccelerated }
                    hw.ifEmpty { all }
                }
                DecoderPreference.SOFTWARE -> MediaCodecSelector { mime, secure, tunneling ->
                    val all = MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)
                    val sw = all.filter { it.softwareOnly || isLikelySoftware(it) }
                    if (sw.isNotEmpty()) sw + all.filter { it !in sw } else all
                }
            }
        }

        private fun isLikelySoftware(info: MediaCodecInfo): Boolean {
            val n = info.name.lowercase()
            return n.startsWith("omx.google.") ||
                n.startsWith("c2.android.") ||
                n.contains("sw") ||
                n.contains("soft")
        }
    }
}

@UnstableApi
class ForgeVideoRenderer(
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    allowedJoiningTimeMs: Long,
    enableDecoderFallback: Boolean,
    eventHandler: Handler?,
    eventListener: VideoRendererEventListener?,
    maxDroppedFramesToNotify: Int,
) : MediaCodecVideoRenderer(
    context,
    mediaCodecSelector,
    allowedJoiningTimeMs,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    maxDroppedFramesToNotify,
) {
    /**
     * Positive [ForgeEngine.audioDelayMs] delays audio relative to video by presenting
     * video earlier (negative PTS adjustment). Audio remains the media clock, so a pure
     * AudioSink delay would shift both A and V together and would not fix sync.
     */
    override fun getBufferTimestampAdjustmentUs(): Long {
        return -ForgeEngine.audioDelayMs.toLong() * 1_000L
    }
}
