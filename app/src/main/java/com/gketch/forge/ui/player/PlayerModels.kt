package com.gketch.forge.ui.player

import androidx.media3.ui.AspectRatioFrameLayout

internal val SPEED_PRESETS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
internal val SLEEP_OPTIONS = listOf(15, 30, 45, 60)
internal val SUBTITLE_SIZES = listOf(16f, 20f, 24f, 28f, 32f)

internal enum class AspectMode(
    val label: String,
    val resizeMode: Int,
    val forcedRatio: Float? = null,
) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    RATIO_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIT, 16f / 9f),
    RATIO_4_3("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIT, 4f / 3f),
    ORIGINAL("Original", AspectRatioFrameLayout.RESIZE_MODE_FIT),
}

internal enum class Panel { None, Speed, Aspect, Sleep, Subtitle, Audio, Quality, Equalizer, Orientation, AbLoop, MediaInfo, Bookmarks, VolumeBoost, SubDelay, AudioDelay, Queue, Chapters, JumpToTime, VideoColor, AudioBalance, Lyrics, Transform, QuickSubDelay, QuickAudioDelay, NightFilter }

internal data class MediaChapter(val title: String, val startMs: Long)

internal enum class OrientationLock(val label: String) {
    AUTO("Auto"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
}

internal data class TrackChoice(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean,
)

