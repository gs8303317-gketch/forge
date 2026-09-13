package com.gketch.forge.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Stereo L/R balance. -100 = full left, 0 = center, +100 = full right.
 * Attached as an AudioProcessor on the DefaultAudioSink.
 *
 * Optional: must never take down playback. Any failure disables processing
 * and falls back to passthrough for the rest of the session.
 */
@UnstableApi
object ForgeBalance {
    private val balanceRef = AtomicInteger(0)
    private val disabledRef = AtomicBoolean(false)

    val balance: Int get() = balanceRef.get()

    /** True when a prior processor fault forced passthrough-only mode. */
    val isDisabled: Boolean get() = disabledRef.get()

    fun setBalance(value: Int) {
        balanceRef.set(value.coerceIn(-100, 100))
    }

    fun processor(): AudioProcessor = BalanceAudioProcessor()

    /** Test/helper: re-enable after a fault (e.g. settings reset). */
    fun resetFaultLatch() {
        disabledRef.set(false)
    }

    @UnstableApi
    private class BalanceAudioProcessor : BaseAudioProcessor() {
        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
                throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            }
            return inputAudioFormat
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val position = inputBuffer.position()
            val limit = inputBuffer.limit()
            val byteCount = limit - position
            val output = replaceOutputBuffer(byteCount)
            if (byteCount <= 0) {
                output.flip()
                return
            }
            val bal = balanceRef.get()
            if (disabledRef.get() || bal == 0 || inputAudioFormat.channelCount < 2) {
                // Passthrough — never touch sample math.
                output.put(inputBuffer)
                inputBuffer.position(limit)
                output.flip()
                return
            }
            try {
                val leftGain = if (bal <= 0) 1f else (100 - bal) / 100f
                val rightGain = if (bal >= 0) 1f else (100 + bal) / 100f
                val channels = inputAudioFormat.channelCount
                val frameBytes = channels * 2 // PCM16
                // Only process whole frames; copy any trailing odd bytes as-is.
                val frameEnd = position + (byteCount / frameBytes) * frameBytes
                inputBuffer.limit(frameEnd)
                while (inputBuffer.remaining() >= 2) {
                    for (ch in 0 until channels) {
                        if (inputBuffer.remaining() < 2) break
                        val sample = inputBuffer.short
                        val gain = when (ch) {
                            0 -> leftGain
                            1 -> rightGain
                            else -> 1f
                        }
                        val scaled = (sample * gain).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        output.putShort(scaled.toShort())
                    }
                }
                inputBuffer.limit(limit)
                if (inputBuffer.hasRemaining()) {
                    output.put(inputBuffer)
                }
                inputBuffer.position(limit)
                output.flip()
            } catch (t: Throwable) {
                // Disable optional balance for this process; finish as passthrough.
                disabledRef.set(true)
                try {
                    inputBuffer.position(position)
                    inputBuffer.limit(limit)
                    output.clear()
                    val again = replaceOutputBuffer(byteCount)
                    again.put(inputBuffer)
                    inputBuffer.position(limit)
                    again.flip()
                } catch (_: Throwable) {
                    inputBuffer.position(limit)
                    output.clear()
                    output.flip()
                }
            }
        }
    }
}
