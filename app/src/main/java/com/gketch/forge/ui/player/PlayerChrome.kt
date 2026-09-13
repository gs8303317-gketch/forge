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

// Chrome, dialogs, tracks, scrub helpers extracted from PlayerScreen

@Composable
internal fun PlayerTopBar(
    title: String,
    showPip: Boolean,
    showSnapshot: Boolean,
    speed: Float,
    sleepLabel: String?,
    abLabel: String?,
    boostLabel: String?,
    moreExpanded: Boolean,
    onInteract: () -> Unit,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onSpeed: () -> Unit,
    onLock: () -> Unit,
    onMore: () -> Unit,
    onDismissMore: () -> Unit,
    onSubtitles: () -> Unit,
    onAudio: () -> Unit,
    onQuality: () -> Unit,
    onSleep: () -> Unit,
    onEqualizer: () -> Unit,
    onOrientation: () -> Unit,
    onAbLoop: () -> Unit,
    onMediaInfo: () -> Unit,
    onBookmarks: () -> Unit,
    onVolumeBoost: () -> Unit,
    onSubDelay: () -> Unit,
    onAudioDelay: () -> Unit,
    onSnapshot: () -> Unit,
    onShare: () -> Unit,
    onQueue: () -> Unit,
    onChapters: () -> Unit,
    showChapters: Boolean,
    onJumpToTime: () -> Unit,
    onVideoColor: () -> Unit,
    onAudioBalance: () -> Unit,
    onLyrics: () -> Unit,
    onPlayAsAudio: () -> Unit,
    playAsAudio: Boolean,
    showPlayAsAudio: Boolean,
    onTransform: () -> Unit = {},
    onNightFilter: () -> Unit = {},
    nightStrength: Float = 0f,
    onToggleStats: () -> Unit = {},
    statsVisible: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onInteract,
            )
            .padding(horizontal = 0.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sleepLabel != null) {
                    Text(
                        text = "Sleep · $sleepLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeAccent,
                    )
                }
                if (abLabel != null) {
                    Text(
                        text = abLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeAccent,
                    )
                }
                if (boostLabel != null) {
                    Text(
                        text = boostLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeAccent,
                    )
                }
            }
        }
        if (showPip) {
            IconButton(onClick = onPip) {
                Icon(
                    Icons.Outlined.PictureInPictureAlt,
                    contentDescription = "Picture in picture",
                    tint = Color.White,
                )
            }
        }
        ForgeCastButton()
        Box {
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
            }
            DropdownMenu(
                expanded = moreExpanded,
                onDismissRequest = onDismissMore,
                containerColor = ForgeGraphite,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.lock_controls), color = Color.White) },
                    onClick = onLock,
                    leadingIcon = {
                        Icon(Icons.Rounded.Lock, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Speed · ${formatSpeed(speed)}", color = Color.White) },
                    onClick = onSpeed,
                    leadingIcon = {
                        Icon(Icons.Rounded.Speed, null, tint = ForgeAccent)
                    },
                )
                // Quick shortcuts near top (EQ / sleep / A-B / snapshot / bookmarks)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.equalizer), color = Color.White) },
                    onClick = onEqualizer,
                    leadingIcon = {
                        Icon(Icons.Rounded.Equalizer, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sleep_timer), color = Color.White) },
                    onClick = onSleep,
                    leadingIcon = {
                        Icon(Icons.Rounded.Timer, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ab_loop), color = Color.White) },
                    onClick = onAbLoop,
                    leadingIcon = {
                        Icon(Icons.Rounded.Loop, null, tint = ForgeAccent)
                    },
                )
                if (showSnapshot) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.frame_snapshot), color = Color.White) },
                        onClick = onSnapshot,
                        leadingIcon = {
                            Icon(Icons.Rounded.CameraAlt, null, tint = ForgeAccent)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bookmarks), color = Color.White) },
                    onClick = onBookmarks,
                    leadingIcon = {
                        Icon(Icons.Rounded.Bookmark, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.queue), color = Color.White) },
                    onClick = onQueue,
                    leadingIcon = {
                        Icon(Icons.Rounded.QueueMusic, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.subtitle_delay_quick), color = Color.White) },
                    onClick = onSubDelay,
                    leadingIcon = {
                        Icon(Icons.Rounded.ClosedCaption, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.audio_delay_quick), color = Color.White) },
                    onClick = onAudioDelay,
                    leadingIcon = {
                        Icon(Icons.Rounded.Audiotrack, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.transform), color = Color.White) },
                    onClick = onTransform,
                    leadingIcon = {
                        Icon(Icons.Rounded.ScreenRotation, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (nightStrength > 0.01f) "Night filter · on"
                            else "Night filter",
                            color = Color.White,
                        )
                    },
                    onClick = onNightFilter,
                    leadingIcon = {
                        Icon(Icons.Rounded.NightsStay, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (statsVisible) stringResource(R.string.stats_hide)
                            else stringResource(R.string.stats_show),
                            color = Color.White,
                        )
                    },
                    onClick = onToggleStats,
                    leadingIcon = {
                        Icon(Icons.Rounded.Info, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Subtitles", color = Color.White) },
                    onClick = onSubtitles,
                    leadingIcon = {
                        Icon(Icons.Rounded.ClosedCaption, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Audio track", color = Color.White) },
                    onClick = onAudio,
                    leadingIcon = {
                        Icon(Icons.Rounded.Audiotrack, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Stream quality", color = Color.White) },
                    onClick = onQuality,
                    leadingIcon = {
                        Icon(Icons.Rounded.HighQuality, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Orientation", color = Color.White) },
                    onClick = onOrientation,
                    leadingIcon = {
                        Icon(Icons.Rounded.ScreenRotation, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Media info", color = Color.White) },
                    onClick = onMediaInfo,
                    leadingIcon = {
                        Icon(Icons.Rounded.Info, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Volume boost", color = Color.White) },
                    onClick = onVolumeBoost,
                    leadingIcon = {
                        Icon(Icons.Rounded.VolumeUp, null, tint = ForgeAccent)
                    },
                )
                if (showChapters) {
                    DropdownMenuItem(
                        text = { Text("Chapters", color = Color.White) },
                        onClick = onChapters,
                        leadingIcon = {
                            Icon(Icons.Rounded.Bookmark, null, tint = ForgeAccent)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Jump to time", color = Color.White) },
                    onClick = onJumpToTime,
                    leadingIcon = {
                        Icon(Icons.Rounded.Schedule, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Video color", color = Color.White) },
                    onClick = onVideoColor,
                    leadingIcon = {
                        Icon(Icons.Rounded.Palette, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Audio balance", color = Color.White) },
                    onClick = onAudioBalance,
                    leadingIcon = {
                        Icon(Icons.Rounded.SurroundSound, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.lyrics), color = Color.White) },
                    onClick = onLyrics,
                    leadingIcon = {
                        Icon(Icons.Rounded.ClosedCaption, null, tint = ForgeAccent)
                    },
                )
                if (showPlayAsAudio) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (playAsAudio) "Play with video" else "Play as audio",
                                color = Color.White,
                            )
                        },
                        onClick = onPlayAsAudio,
                        leadingIcon = {
                            Icon(Icons.Rounded.Headset, null, tint = ForgeAccent)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Share", color = Color.White) },
                    onClick = onShare,
                    leadingIcon = {
                        Icon(Icons.Rounded.Share, null, tint = ForgeAccent)
                    },
                )
            }
        }
    }
}

@Composable
internal fun SpeedRow(
    selected: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipRow(modifier = modifier) {
        SPEED_PRESETS.forEach { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                label = { Text(formatSpeed(preset)) },
                colors = chipColors(),
            )
        }
    }
}

@Composable
internal fun ChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Composable
internal fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = Color.Black,
    containerColor = ForgeGraphite,
    labelColor = Color.White,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun PlayerControls(
    progress: PlayerProgressState,
    scrubbing: Boolean,
    scrubValue: Float,
    isPlaying: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    repeatMode: Int,
    shuffleOn: Boolean,
    showFrameStep: Boolean = false,
    showAspect: Boolean = false,
    aspectLabel: String = "Fit",
    showOrientToggle: Boolean = false,
    orientLabel: String = "Auto",
    showChapters: Boolean = false,
    showSkipIntro: Boolean = false,
    skipIntroSeconds: Int = 0,
    onSkipIntro: () -> Unit = {},
    onInteract: () -> Unit = {},
    onFrameStep: (forward: Boolean) -> Unit = {},
    onCycleAspect: () -> Unit = {},
    onLongAspect: () -> Unit = {},
    onToggleOrient: () -> Unit = {},
    onQueue: () -> Unit = {},
    onChapterPrev: () -> Unit = {},
    onChapterNext: () -> Unit = {},
    showRemainingTime: Boolean = false,
    onToggleRemainingTime: () -> Unit = {},
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
) {
    val positionMs = progress.positionMs
    val durationMs = progress.durationMs
    val bufferedMs = progress.bufferedMs

    val config = LocalConfiguration.current
    val landscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val vPad = if (landscape) 2.dp else 4.dp
    val scrubTouch = if (landscape) 28.dp else 32.dp
    val secondaryIcon = if (landscape) 18.dp else 20.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onInteract,
            )
            .padding(horizontal = if (landscape) 14.dp else 10.dp, vertical = vPad),
    ) {
        val playProgress = if (durationMs > 0) {
            (if (scrubbing) scrubValue else positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val bufferedFrac = if (durationMs > 0) {
            (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val displayPos = if (scrubbing) (scrubValue * durationMs).toLong() else positionMs
        Box(Modifier.fillMaxWidth().height(scrubTouch)) {
            BufferedProgressTrack(
                progress = playProgress,
                buffered = bufferedFrac,
                trackHeight = if (landscape) 5.dp else 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 2.dp),
            )
            Slider(
                value = playProgress,
                onValueChange = onScrub,
                onValueChangeFinished = onScrubEnd,
                modifier = Modifier.fillMaxWidth().height(scrubTouch),
                colors = SliderDefaults.colors(
                    thumbColor = ForgeAccent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
        val remainingMs = (durationMs - displayPos).coerceAtLeast(0L)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    onToggleRemainingTime()
                    onInteract()
                }
                .padding(vertical = if (landscape) 0.dp else 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (showRemainingTime) {
                    "−${formatDuration(remainingMs)}"
                } else {
                    formatDuration(displayPos)
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
        // Primary transport — VLC-like centered prev / play / next
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (landscape) 0.dp else 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFrameStep) {
                IconButton(onClick = { onFrameStep(false) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous frame",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            IconButton(onClick = onPrev, enabled = canPrev, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (canPrev) Color.White else ForgeMuted,
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ForgeAccent),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onNext, enabled = canNext, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = if (canNext) Color.White else ForgeMuted,
                    modifier = Modifier.size(32.dp),
                )
            }
            if (showFrameStep) {
                IconButton(onClick = { onFrameStep(true) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Next frame",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        // Secondary tools — compact calm row (aspect / orient / queue / etc.)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (landscape) 0.dp else 2.dp, bottom = if (landscape) 0.dp else 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleOn) ForgeAccent else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(secondaryIcon),
                )
            }
            IconButton(onClick = onQueue, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.queue),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(secondaryIcon),
                )
            }
            if (showChapters) {
                IconButton(onClick = onChapterPrev, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.chapter_prev),
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(secondaryIcon),
                    )
                }
            }
            if (showAspect) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .combinedClickable(
                            onClick = onCycleAspect,
                            onLongClick = onLongAspect,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AspectRatio,
                        contentDescription = stringResource(R.string.aspect_cycle, aspectLabel),
                        tint = ForgeAccent.copy(alpha = 0.95f),
                        modifier = Modifier.size(secondaryIcon),
                    )
                }
            }
            if (showOrientToggle) {
                IconButton(onClick = onToggleOrient, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.ScreenRotation,
                        contentDescription = stringResource(R.string.orient_toggle, orientLabel),
                        tint = if (orientLabel == "Auto") Color.White.copy(alpha = 0.85f) else ForgeAccent,
                        modifier = Modifier.size(secondaryIcon),
                    )
                }
            }
            if (showSkipIntro && skipIntroSeconds > 0) {
                IconButton(onClick = onSkipIntro, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = stringResource(R.string.skip_intro) + " ${skipIntroSeconds}s",
                        tint = ForgeAccent.copy(alpha = 0.95f),
                        modifier = Modifier.size(secondaryIcon),
                    )
                }
            }
            if (showChapters) {
                IconButton(onClick = onChapterNext, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.chapter_next),
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(secondaryIcon),
                    )
                }
            }
            IconButton(onClick = onCycleRepeat, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                        Color.White.copy(alpha = 0.85f)
                    } else {
                        ForgeAccent
                    },
                    modifier = Modifier.size(secondaryIcon),
                )
            }
        }
    }
}

@Composable
internal fun AudioArtwork(title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ForgeGraphite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AudioFile,
                contentDescription = null,
                tint = ForgeAccent,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
