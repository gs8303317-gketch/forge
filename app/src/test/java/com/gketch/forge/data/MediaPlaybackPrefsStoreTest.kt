package com.gketch.forge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaPlaybackPrefsStoreTest {
    @Test
    fun keyHex_isStableAndDistinct() {
        val a = MediaPlaybackPrefsStore.keyHex("content://media/1")
        val b = MediaPlaybackPrefsStore.keyHex("content://media/1")
        val c = MediaPlaybackPrefsStore.keyHex("content://media/2")
        assertEquals(24, a.length)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
