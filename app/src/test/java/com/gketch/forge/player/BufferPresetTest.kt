package com.gketch.forge.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BufferPresetTest {
    @Test
    fun playbackMsIsLowerThanRebufferForQuickStart() {
        BufferPreset.entries.forEach { preset ->
            assertTrue(
                "${preset.name} playbackMs should be well below rebufferMs",
                preset.playbackMs < preset.rebufferMs,
            )
            assertTrue(
                "${preset.name} playbackMs should start faster than Media3 default 2500",
                preset.playbackMs <= 1_500,
            )
        }
    }

    @Test
    fun largePresetKeepsSafeRebuffer() {
        assertEquals(1_000, BufferPreset.LARGE.playbackMs)
        assertEquals(8_000, BufferPreset.LARGE.rebufferMs)
        assertEquals(35_000, BufferPreset.LARGE.minMs)
    }
}
