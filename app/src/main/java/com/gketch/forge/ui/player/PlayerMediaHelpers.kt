package com.gketch.forge.ui.player

import com.gketch.forge.R
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.util.Rational
import android.util.TypedValue
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gketch.forge.cast.ForgeCastButton
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ExperimentalMaterial3Api
import com.gketch.forge.player.ForgeVideoTransform
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.gketch.forge.MainActivity
import com.gketch.forge.data.ForgeMediaItem
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.ui.zIndex
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import com.gketch.forge.data.AppSettings
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.SleepEndAction
import com.gketch.forge.data.BrightnessStore
import com.gketch.forge.data.SubtitleBackground
import com.gketch.forge.data.SubtitleColor
import com.gketch.forge.data.SubtitlePosition
import com.gketch.forge.data.BookmarkStore
import com.gketch.forge.data.MediaBookmark
import com.gketch.forge.player.ForgeCrossfade
import com.gketch.forge.player.ForgeLoudness
import com.gketch.forge.player.PlaybackErrors
import com.gketch.forge.data.LyricsRepository
import com.gketch.forge.data.LyricsResult
import com.gketch.forge.player.ForgeEngine
import com.gketch.forge.player.ForgePlayerPrefsStore
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeBehavior
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.data.SidecarSubtitles
import com.gketch.forge.data.TrackPrefsStore
import com.gketch.forge.data.MediaPlaybackPrefsStore
import com.gketch.forge.data.SubtitleOutline
import com.gketch.forge.data.WatchedStore
import com.gketch.forge.player.ForgeAudioFx
import com.gketch.forge.player.ForgeBalance
import com.gketch.forge.player.ForgeVideoColor
import com.gketch.forge.player.SeriesEpisode
import com.gketch.forge.data.MediaRepository
import com.gketch.forge.player.ForgeEqualizer
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import com.gketch.forge.ui.library.formatDuration
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Apply saved-position policy for an already-prepared current item. */
internal suspend fun applyResumePolicy(
    player: Player,
    uri: String,
    behavior: ResumeBehavior,
    resumeStore: ResumeStore,
    onPrompt: (Long) -> Unit,
) {
    val saved = try {
        resumeStore.getPosition(uri)
    } catch (_: Exception) {
        0L
    }
    when (behavior) {
        ResumeBehavior.ALWAYS_START_OVER -> {
            player.seekTo(0L)
            player.play()
        }
        ResumeBehavior.ALWAYS_CONTINUE -> {
            if (saved >= ResumeStore.MIN_SAVE_MS) player.seekTo(saved)
            player.play()
        }
        ResumeBehavior.ASK -> {
            when {
                saved >= ResumeStore.RESUME_PROMPT_MS -> onPrompt(saved)
                saved >= ResumeStore.MIN_SAVE_MS -> {
                    player.seekTo(saved)
                    player.play()
                }
                else -> player.play()
            }
        }
    }
}

internal fun shareCurrentMedia(context: android.content.Context, item: ForgeMediaItem?) {
    if (item == null) {
        Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = item.uri
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType.takeIf { it.isNotBlank() && '*' !in it }
            ?: if (item.isVideo) "video/*" else "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, item.title)
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share via"))
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "Share failed", Toast.LENGTH_SHORT).show()
    }
}

internal fun parseChapters(metadata: Metadata): List<MediaChapter> {
    val out = mutableListOf<MediaChapter>()
    for (i in 0 until metadata.length()) {
        when (val entry = metadata.get(i)) {
            is ChapterFrame -> {
                var title = entry.chapterId.ifBlank { "Chapter" }
                for (j in 0 until entry.getSubFrameCount()) {
                    val frame = entry.getSubFrame(j) as? TextInformationFrame
                    val text = frame?.values?.firstOrNull()?.takeIf { it.isNotBlank() }
                    if (text != null) {
                        title = text
                        break
                    }
                }
                out += MediaChapter(title, entry.startTimeMs.toLong().coerceAtLeast(0L))
            }
        }
    }
    return out
}

internal fun applyForcedAspect(playerView: PlayerView, aspect: AspectMode) {
    try {
        val frame = playerView.findViewById<AspectRatioFrameLayout>(
            androidx.media3.ui.R.id.exo_content_frame,
        ) ?: return
        if (frame.resizeMode != aspect.resizeMode) {
            frame.resizeMode = aspect.resizeMode
        }
        val forced = aspect.forcedRatio
        val nextRatio = when {
            forced != null && forced > 0f -> forced
            else -> {
                val vs = playerView.player?.videoSize
                if (vs != null && vs.width > 0 && vs.height > 0) {
                    vs.width * (if (vs.pixelWidthHeightRatio > 0f) vs.pixelWidthHeightRatio else 1f) / vs.height
                } else {
                    null
                }
            }
        }
        if (nextRatio != null) {
            val tagKey = 0x46A50101
            val prev = frame.getTag(tagKey) as? Float
            if (prev == null || kotlin.math.abs(prev - nextRatio) > 0.0001f) {
                frame.setAspectRatio(nextRatio)
                frame.setTag(tagKey, nextRatio)
            }
        }
    } catch (_: Throwable) {
        // Aspect helpers are optional — never fail the surface.
    }
}

internal fun estimateFrameStepMs(player: Player): Long {
    return try {
        val groups = player.currentTracks.groups
        var fps = 0f
        for (g in groups) {
            if (g.type != C.TRACK_TYPE_VIDEO) continue
            for (i in 0 until g.length) {
                val rate = g.getTrackFormat(i).frameRate
                if (rate > 1f && rate < 240f) {
                    fps = rate
                    break
                }
            }
            if (fps > 0f) break
        }
        when {
            fps > 1f -> (1000f / fps).toLong().coerceIn(16L, 100L)
            else -> 33L
        }
    } catch (_: Exception) {
        33L
    }
}

internal fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}×"
    } else {
        "${speed}×"
    }
}

internal fun formatSleep(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

internal fun musicVolumeFraction(context: Context): Float {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
}

internal fun setMusicVolume(context: Context, fraction: Float) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val value = (fraction.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
}

/** 0..1 = system volume; 1..2 = max system + loudness boost (HUD 100%..200%). */
internal fun combinedVolumeLevel(context: Context): Float {
    val boost = ForgeLoudness.boostPercent
    return if (boost > 100) {
        1f + ((boost - 100).toFloat() / (ForgeLoudness.MAX_BOOST_PERCENT - 100).toFloat())
            .coerceIn(0f, 1f)
    } else {
        musicVolumeFraction(context).coerceIn(0f, 1f)
    }
}

internal fun setCombinedVolumeLevel(context: Context, level: Float, onBoostPercent: (Int) -> Unit) {
    val v = level.coerceIn(0f, 2f)
    runCatching {
        if (v <= 1f) {
            setMusicVolume(context, v)
            ForgeLoudness.setBoostPercent(100)
            onBoostPercent(100)
        } else {
            setMusicVolume(context, 1f)
            val pct = (
                100f + (v - 1f) * (ForgeLoudness.MAX_BOOST_PERCENT - 100).toFloat()
                ).toInt().coerceIn(100, ForgeLoudness.MAX_BOOST_PERCENT)
            ForgeLoudness.setBoostPercent(pct)
            onBoostPercent(pct)
        }
    }
}

internal fun shareSnapshotUri(context: Context, uri: android.net.Uri) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_snapshot)))
    }
}

/**
 * Video-only brightness: dim via overlay (caller), boost via ColorMatrix surfaceBoost.
 * 1.0 = native; (1..2] maps to surfaceBoost 0..~0.65.
 */
internal fun applySurfaceBrightness(fraction: Float, playerView: PlayerView?) {
    runCatching {
        val boost = ((fraction - 1f).coerceAtLeast(0f) * 0.65f).coerceIn(0f, 0.65f)
        ForgeVideoColor.setSurfaceBoost(boost)
        ForgeVideoColor.applyTo(playerView)
    }
}

/**
 * If [current] looks like an episode, find the next file in the same MediaStore folder.
 * Auto-plays when queue was a single item; otherwise offers a prompt.
 */
internal suspend fun maybeSeriesAutoNext(
    current: ForgeMediaItem,
    mediaRepository: MediaRepository,
    onOffer: (ForgeMediaItem) -> Unit,
) {
    if (current.bucketId == 0L) return
    val folder = mediaRepository.loadFolderItems(current.bucketId)
    val next = SeriesEpisode.findNext(current, folder) ?: return
    // Always show end card (Play next / Stop) when a next episode resolves.
    onOffer(next)
}

internal fun clearWindowBrightness(activity: Activity?) {
    val window = activity?.window ?: return
    val lp = window.attributes
    if (lp.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = lp
    }
}
