package com.gketch.forge.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesEpisodeTest {
    @Test
    fun parsesSxxExx() {
        val m = SeriesEpisode.parse("Show.Name.S01E02.1080p.mkv")
        assertNotNull(m)
        assertEquals(1, m!!.season)
        assertEquals(2, m.episode)
    }

    @Test
    fun parsesNxNN() {
        val m = SeriesEpisode.parse("Show 1x03.mp4")
        assertNotNull(m)
        assertEquals(1, m!!.season)
        assertEquals(3, m.episode)
    }

    @Test
    fun parsesEpisodeWord() {
        val m = SeriesEpisode.parse("My Show Episode 12.mp4")
        assertNotNull(m)
        assertNull(m!!.season)
        assertEquals(12, m.episode)
    }

    @Test
    fun sortKeyOrdersEpisodes() {
        val a = SeriesEpisode.parse("Show S01E01.mkv")!!
        val b = SeriesEpisode.parse("Show S01E02.mkv")!!
        val c = SeriesEpisode.parse("Show S02E01.mkv")!!
        assertTrue(a.sortKey < b.sortKey)
        assertTrue(b.sortKey < c.sortKey)
    }

    @Test
    fun nonEpisodeReturnsNull() {
        assertNull(SeriesEpisode.parse("Vacation Video 2024.mp4"))
    }
}
