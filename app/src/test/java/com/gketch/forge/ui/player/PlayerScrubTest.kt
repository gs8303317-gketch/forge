package com.gketch.forge.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerScrubTest {

    @Test
    fun quantizeFloorsToWholeSecond() {
        assertEquals(0L, quantizeToSecondMs(0L))
        assertEquals(0L, quantizeToSecondMs(999L))
        assertEquals(1_000L, quantizeToSecondMs(1_000L))
        assertEquals(1_000L, quantizeToSecondMs(1_999L))
        assertEquals(12_000L, quantizeToSecondMs(12_450L))
    }

    @Test
    fun quantizeClampsNegativeToZero() {
        assertEquals(0L, quantizeToSecondMs(-50L))
        assertEquals(0L, quantizeToSecondMs(-1_500L))
    }

    @Test
    fun firstDistinctSecondAlwaysSeeks() {
        assertTrue(shouldPreviewSeekSecond(10_400L, lastPreviewSecondMs = -1L))
    }

    @Test
    fun sameSecondDoesNotReseek() {
        assertFalse(shouldPreviewSeekSecond(10_200L, lastPreviewSecondMs = 10_000L))
        assertFalse(shouldPreviewSeekSecond(10_999L, lastPreviewSecondMs = 10_000L))
    }

    @Test
    fun nextSecondSeeksImmediatelyNoThrottle() {
        assertTrue(shouldPreviewSeekSecond(11_000L, lastPreviewSecondMs = 10_000L))
        assertTrue(shouldPreviewSeekSecond(11_050L, lastPreviewSecondMs = 10_000L))
    }

    @Test
    fun flingJumpStillSeeksLatestSecond() {
        assertTrue(shouldPreviewSeekSecond(40_000L, lastPreviewSecondMs = 10_000L))
    }

    @Test
    fun backwardSecondSeeks() {
        assertTrue(shouldPreviewSeekSecond(9_500L, lastPreviewSecondMs = 10_000L))
        assertEquals(9_000L, quantizeToSecondMs(9_500L))
    }
}
