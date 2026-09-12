package com.gketch.forge.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Stereo L/R balance. -100 = full left, 0 = center, +100 = full right.
 * Attached as an AudioProcessor on the DefaultAudioSink.
 */
@UnstableApi
object ForgeBalance {
    private val balanceRef = AtomicInteger(0)

    val balance: Int get() = balanceRef.get()

    fun setBalance(value: Int) {
        balanceRef.set(value.coerceIn(-100, 100))
    }

    fun processor(): AudioProcessor = BalanceAudioProcessor()

    @UnstableApi
    private class BalanceAudioProcessor : BaseAudioProcessor() {
        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
                throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            }
            return inputAudioFormat
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val bal = balanceRef.get()
            val position = inputBuffer.position()
            val limit = inputBuffer.limit()
            val output = replaceOutputBuffer(limit - position)
            if (bal == 0 || inputAudioFormat.channelCount < 2) {
                output.put(inputBuffer)
            } else {
                // -100..100 → leftGain/rightGain in 0..1
                val leftGain = if (bal <= 0) 1f else (100 - bal) / 100f
                val rightGain = if (bal >= 0) 1f else (100 + bal) / 100f
                val channels = inputAudioFormat.channelCount
                while (inputBuffer.hasRemaining()) {
                    for (ch in 0 until channels) {
                        if (!inputBuffer.hasRemaining()) break
                        val sample = inputBuffer.short
                        val gain = when (ch) {
                            0 -> leftGain
                            1 -> rightGain
                            else -> 1f
                        }
                        val scaled = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        output.putShort(scaled.toShort())
                    }
                }
            }
            inputBuffer.position(limit)
            output.flip()
        }
    }
}
