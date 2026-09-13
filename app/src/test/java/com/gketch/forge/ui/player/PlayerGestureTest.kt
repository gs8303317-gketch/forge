package com.gketch.forge.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGestureTest {

    private val w = 1080f
    private val h = 1920f
    private val slop = 12f

    private fun classify(
        dx: Float,
        dy: Float,
        startX: Float,
        startY: Float = h * 0.3f,
        invert: Boolean = false,
        swipeDown: Boolean = true,
    ) = classifySwipeGesture(
        dx = dx,
        dy = dy,
        startX = startX,
        startY = startY,
        width = w,
        height = h,
        slop = slop,
        invertSides = invert,
        swipeDownEnabled = swipeDown,
    )

    @Test
    fun belowSlopIsIgnored() {
        assertNull(classify(5f, 4f, startX = w * 0.5f))
    }

    @Test
    fun horizontalAnywhereIsSeek() {
        assertEquals(GestureKind.Seek, classify(40f, 8f, startX = w * 0.5f))
        assertEquals(GestureKind.Seek, classify(-50f, 10f, startX = w * 0.08f))
        assertEquals(GestureKind.Seek, classify(60f, -12f, startX = w * 0.95f))
    }

    @Test
    fun leftEdgeVerticalIsBrightness() {
        assertEquals(GestureKind.Brightness, classify(4f, -80f, startX = w * 0.08f))
    }

    @Test
    fun rightEdgeVerticalIsVolume() {
        assertEquals(GestureKind.Volume, classify(4f, 80f, startX = w * 0.95f))
    }

    @Test
    fun invertSidesSwapsBrightnessAndVolume() {
        assertEquals(GestureKind.Volume, classify(4f, -80f, startX = w * 0.08f, invert = true))
        assertEquals(GestureKind.Brightness, classify(4f, 80f, startX = w * 0.95f, invert = true))
    }

    @Test
    fun centerVerticalDoesNotStealSeekWhenMostlyHorizontal() {
        assertEquals(GestureKind.Seek, classify(80f, 30f, startX = w * 0.5f))
    }

    @Test
    fun centerDownwardCanDismissWhenClearlyVertical() {
        assertEquals(GestureKind.Dismiss, classify(8f, 80f, startX = w * 0.5f, startY = h * 0.25f))
    }

    @Test
    fun dismissDisabledStaysUnclassifiedInCenterVertical() {
        assertNull(classify(8f, 80f, startX = w * 0.5f, swipeDown = false))
    }

    @Test
    fun dismissFromLowerHalfIsIgnored() {
        assertNull(classify(8f, 80f, startX = w * 0.5f, startY = h * 0.7f))
    }

    @Test
    fun dismissNeedsClearVerticalDominance() {
        // 2.0x vertical is not enough (ratio is 2.75)
        assertNull(classify(30f, 62f, startX = w * 0.5f, startY = h * 0.2f))
    }

    @Test
    fun dismissCloseRequiresThirtyPercentHeight() {
        assertFalse(dismissShouldClose(h * 0.18f, 0f, h))
        assertTrue(dismissShouldClose(h * 0.32f, 10f, h))
        assertFalse(dismissShouldClose(h * 0.40f, h * 0.20f, h))
    }

    @Test
    fun doubleTapZonesAreThirds() {
        assertEquals(true, doubleTapSeekBack(w * 0.10f, w))
        assertEquals(false, doubleTapSeekBack(w * 0.90f, w))
        assertNull(doubleTapSeekBack(w * 0.50f, w))
    }
}
