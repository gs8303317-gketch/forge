package com.gketch.forge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidecarSubtitlesTest {
    @Test
    fun basename_stripsExtension() {
        assertEquals("Movie", SidecarSubtitles.basename("Movie.mkv"))
        assertEquals("Movie.Name", SidecarSubtitles.basename("Movie.Name.mp4"))
        assertEquals("noext", SidecarSubtitles.basename("noext"))
    }

    @Test
    fun candidateNames_includeSrtAndVtt() {
        val names = SidecarSubtitles.candidateNames("Episode 01")
        assertTrue(names.any { it.equals("Episode 01.srt", ignoreCase = true) })
        assertTrue(names.any { it.equals("Episode 01.vtt", ignoreCase = true) })
    }
}
