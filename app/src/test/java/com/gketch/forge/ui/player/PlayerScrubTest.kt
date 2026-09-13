package com.gketch.forge.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerScrubTest {

    @Test
    fun firstPreviewSeekAlwaysFires() {
        val hold = ScrubHold()
        val d = decidePreviewSeek(10_000L, hold, nowElapsedRealtime = 1_000L)
        assertTrue(d.shouldSeek)
        assertFalse(d.fling)
    }

    @Test
    fun smallMoveWithinThrottleIsSkipped() {
        val hold = ScrubHold()
        val first = decidePreviewSeek(10_000L, hold, 1_000L)
        assertTrue(first.shouldSeek)
        hold.lastSeekMs = 10_000L
        hold.lastSeekAt = 1_000L
        val d = decidePreviewSeek(10_200L, hold, 1_100L)
        assertFalse(d.shouldSeek)
    }

    @Test
    fun flingSkipsPreviewSeeks() {
        val hold = ScrubHold()
        decidePreviewSeek(10_000L, hold, 1_000L)
        hold.lastSeekMs = 10_000L
        hold.lastSeekAt = 1_000L
        // 30s of media in 100ms wall ≈ 300× realtime
        val d = decidePreviewSeek(40_000L, hold, 1_100L)
        assertFalse(d.shouldSeek)
        assertTrue(d.fling)
    }

    @Test
    fun slowDragSeeksAfterThrottle() {
        val hold = ScrubHold()
        decidePreviewSeek(10_000L, hold, 1_000L)
        hold.lastSeekMs = 10_000L
        hold.lastSeekAt = 1_000L
        hold.lastSampleMs = 10_000L
        hold.lastSampleAt = 1_000L
        val d = decidePreviewSeek(10_800L, hold, 1_400L)
        assertTrue(d.shouldSeek)
        assertFalse(d.fling)
    }

    @Test
    fun fastDragUsesLargerThrottle() {
        val hold = ScrubHold()
        decidePreviewSeek(10_000L, hold, 1_000L)
        hold.lastSeekMs = 10_000L
        hold.lastSeekAt = 1_000L
        // 2s media / 400ms wall = 5× → fast, not fling; 400ms < 520ms fast throttle
        val d = decidePreviewSeek(12_000L, hold, 1_400L)
        assertFalse(d.shouldSeek)
        assertFalse(d.fling)
        assertTrue(hold.fast)
    }
}
