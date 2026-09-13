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

/** Undo any leftover 1.18 window dim so chrome stays at system brightness. */
@Composable
internal fun ScrubTimecodeHud(elapsedMs: Long, totalMs: Long) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (totalMs > 0L) {
                    "${formatDuration(elapsedMs)} / ${formatDuration(totalMs)}"
                } else {
                    formatDuration(elapsedMs)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        }
    }
}

internal class ScrubHold {
    var active: Boolean = false
    var wasPlaying: Boolean = false
    var volume: Float = 1f
    var lastSeekMs: Long = -1L
    var lastSeekAt: Long = 0L
    var pendingMs: Long = -1L
}

/** ~120–150ms throttle + keyframe seeks while dragging; EXACT settle on release. */
internal const val SCRUB_THROTTLE_MS = 130L

internal const val SCRUB_MIN_DELTA_MS = 350L

internal const val SCRUB_FORCE_DELTA_MS = 2_000L

internal fun startVideoScrub(player: Player?, hold: ScrubHold) {
    if (player == null || hold.active) return
    hold.active = true
    hold.wasPlaying = player.isPlaying
    hold.volume = player.volume
    hold.lastSeekMs = -1L
    hold.pendingMs = -1L
    // Mute once — do not thrash audio session / EQ attach on every tick.
    if (hold.volume > 0f) runCatching { player.volume = 0f }
    if (hold.wasPlaying) runCatching { player.pause() }
    ForgeEngine.setScrubSeek(true)
}

internal fun previewSeekTo(player: Player?, target: Long, hold: ScrubHold) {
    if (player == null) return
    val clamped = target.coerceAtLeast(0L)
    hold.pendingMs = clamped
    val now = SystemClock.elapsedRealtime()
    val last = hold.lastSeekMs
    val elapsed = now - hold.lastSeekAt
    if (last >= 0L) {
        val delta = abs(clamped - last)
        val smallMove = delta < SCRUB_MIN_DELTA_MS && elapsed < SCRUB_THROTTLE_MS
        val tooSoon = delta < SCRUB_FORCE_DELTA_MS && elapsed < SCRUB_THROTTLE_MS
        if (smallMove || tooSoon) return
    }
    hold.lastSeekMs = clamped
    hold.lastSeekAt = now
    hold.pendingMs = -1L
    runCatching { player.seekTo(clamped) }
}

internal fun finishVideoScrub(player: Player?, target: Long, hold: ScrubHold) {
    val clamped = target.coerceAtLeast(0L)
    // Restore EXACT/DEFAULT before the settle seek so the final frame is accurate.
    runCatching { ForgeEngine.setScrubSeek(false) }
    if (player != null) {
        runCatching { player.seekTo(clamped) }
        if (hold.active) {
            // Restore volume once (was muted for preview).
            runCatching { player.volume = hold.volume }
            if (hold.wasPlaying) runCatching { player.play() }
        }
    }
    hold.active = false
    hold.lastSeekMs = -1L
    hold.pendingMs = -1L
}
