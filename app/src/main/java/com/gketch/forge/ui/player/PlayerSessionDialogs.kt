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

@Composable
internal fun SubtitleDialog(
    tracks: List<TrackChoice>,
    enabled: Boolean,
    sizeSp: Float,
    color: SubtitleColor,
    background: SubtitleBackground,
    position: SubtitlePosition,
    outline: SubtitleOutline,
    hasExternal: Boolean,
    delayMs: Int,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSelectTrack: (TrackChoice) -> Unit,
    onSize: (Float) -> Unit,
    onColor: (SubtitleColor) -> Unit,
    onBackground: (SubtitleBackground) -> Unit,
    onPosition: (SubtitlePosition) -> Unit,
    onOutline: (SubtitleOutline) -> Unit,
    onDelay: () -> Unit,
    onPickExternal: () -> Unit,
    onClearExternal: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Subtitles", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggle(!enabled) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Show subtitles", color = Color.White)
                    Text(if (enabled) "On" else "Off", color = ForgeAccent)
                }
                Spacer(Modifier.height(8.dp))
                Text("Tracks", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                if (tracks.isEmpty()) {
                    Text("No embedded text tracks", color = ForgeMuted, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    tracks.forEach { track ->
                        Text(
                            text = track.label + if (track.selected) " ✓" else "",
                            color = if (track.selected) ForgeAccent else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTrack(track) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
                HorizontalDivider(color = ForgeMuted.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onPickExternal) {
                    Text("Load .srt / .vtt…", color = ForgeAccent)
                }
                if (hasExternal) {
                    TextButton(onClick = onClearExternal) {
                        Text("Clear external file", color = ForgeMuted)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Size", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SUBTITLE_SIZES.forEach { size ->
                        FilterChip(
                            selected = sizeSp == size,
                            onClick = { onSize(size) },
                            label = { Text("${size.toInt()}") },
                            colors = chipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Color", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SubtitleColor.entries.forEach { c ->
                        FilterChip(
                            selected = color == c,
                            onClick = { onColor(c) },
                            label = { Text(c.label) },
                            colors = chipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Background", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SubtitleBackground.entries.forEach { b ->
                        FilterChip(
                            selected = background == b,
                            onClick = { onBackground(b) },
                            label = { Text(b.label) },
                            colors = chipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Outline", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SubtitleOutline.entries.forEach { o ->
                        FilterChip(
                            selected = outline == o,
                            onClick = { onOutline(o) },
                            label = { Text(o.label) },
                            colors = chipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Position", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SubtitlePosition.entries.forEach { p ->
                        FilterChip(
                            selected = position == p,
                            onClick = { onPosition(p) },
                            label = { Text(p.label) },
                            colors = chipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelay) {
                    Text("Delay · ${formatDelay(delayMs)}", color = ForgeAccent)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
internal fun AudioDialog(
    tracks: List<TrackChoice>,
    onDismiss: () -> Unit,
    onSelect: (TrackChoice) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Audio track", color = Color.White) },
        text = {
            Column {
                if (tracks.isEmpty()) {
                    Text("Only one audio track", color = ForgeMuted)
                } else {
                    tracks.forEach { track ->
                        Text(
                            text = track.label + if (track.selected) " ✓" else "",
                            color = if (track.selected) ForgeAccent else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(track) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
internal fun AbLoopDialog(
    pointA: Long?,
    pointB: Long?,
    enabled: Boolean,
    positionMs: Long,
    onDismiss: () -> Unit,
    onSetA: () -> Unit,
    onSetB: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onClear: () -> Unit,
    onSeekA: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("A-B loop", color = Color.White) },
        text = {
            Column {
                Text(
                    text = "Current · ${formatDuration(positionMs)}",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A · ${pointA?.let { formatDuration(it) } ?: "—"}${if (pointB != null) "   B · ${formatDuration(pointB)}" else ""}",
                    color = Color.White,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = pointA != null,
                        onClick = onSetA,
                        label = { Text("Set A") },
                        colors = chipColors(),
                    )
                    FilterChip(
                        selected = pointB != null,
                        onClick = onSetB,
                        label = { Text("Set B") },
                        colors = chipColors(),
                    )
                    FilterChip(
                        selected = false,
                        onClick = onClear,
                        label = { Text("Clear") },
                        colors = chipColors(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Loop segment", color = Color.White)
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = ForgeAccent,
                            checkedThumbColor = Color.Black,
                        ),
                    )
                }
                if (pointA != null) {
                    TextButton(onClick = onSeekA) {
                        Text("Jump to A", color = ForgeAccent)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
internal fun EqualizerDialog(
    enabled: Boolean,
    bands: List<com.gketch.forge.player.EqBand>,
    presetName: String,
    bassOn: Boolean,
    virtOn: Boolean,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onBand: (Int, Float) -> Unit,
    onPreset: (com.gketch.forge.player.EqPreset) -> Unit,
    onBass: (Boolean) -> Unit,
    onVirt: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Equalizer", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enabled", color = Color.White)
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = ForgeAccent,
                            checkedThumbColor = Color.Black,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Preset · $presetName", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ForgeEqualizer.presets.forEach { preset ->
                        FilterChip(
                            selected = presetName == preset.name,
                            onClick = { onPreset(preset) },
                            label = { Text(preset.name) },
                            colors = chipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bass boost", color = Color.White)
                    Switch(
                        checked = bassOn,
                        onCheckedChange = onBass,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = ForgeAccent,
                            checkedThumbColor = Color.Black,
                        ),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Virtualizer", color = Color.White)
                    Switch(
                        checked = virtOn,
                        onCheckedChange = onVirt,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = ForgeAccent,
                            checkedThumbColor = Color.Black,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (bands.isEmpty()) {
                    Text(
                        "Equalizer attaches after playback starts.",
                        color = ForgeMuted,
                    )
                } else {
                    bands.forEach { band ->
                        val value = ForgeEqualizer.bandNormalized(band)
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = ForgeEqualizer.frequencyLabel(band.frequencyMilliHz),
                                color = ForgeMuted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Slider(
                                value = value,
                                onValueChange = { onBand(band.index, it) },
                                valueRange = -1f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ForgeAccent,
                                    activeTrackColor = ForgeAccent,
                                    inactiveTrackColor = ForgeMuted.copy(alpha = 0.3f),
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
internal fun QualityDialog(
    tracks: List<TrackChoice>,
    onDismiss: () -> Unit,
    onAuto: () -> Unit,
    onSelect: (TrackChoice) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Stream quality", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "HLS/DASH variants and multi-track video. Auto lets AdaptiveTrackSelection pick.",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                FilterChip(
                    selected = tracks.none { it.selected } || tracks.count { it.selected } > 1,
                    onClick = onAuto,
                    label = { Text("Auto") },
                    colors = chipColors(),
                )
                Spacer(Modifier.height(8.dp))
                if (tracks.isEmpty()) {
                    Text("No alternate video tracks for this stream.", color = ForgeMuted)
                } else {
                    tracks.forEach { choice ->
                        FilterChip(
                            selected = choice.selected,
                            onClick = { onSelect(choice) },
                            label = { Text(choice.label) },
                            colors = chipColors(),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
internal fun QueueDialog(
    items: List<ForgeMediaItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Queue · ${items.size}", color = Color.White) },
        text = {
            if (items.isEmpty()) {
                Text("Queue is empty", color = ForgeMuted)
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                ) {
                    items(items.size) { i ->
                        val item = items[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPlayIndex(i) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = if (i == currentIndex) ForgeAccent else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatDuration(item.durationMs),
                                    color = ForgeMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            IconButton(
                                onClick = { onMove(i, i - 1) },
                                enabled = i > 0,
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = if (i > 0) Color.White else ForgeMuted,
                                )
                            }
                            IconButton(
                                onClick = { onMove(i, i + 1) },
                                enabled = i < items.lastIndex,
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = if (i < items.lastIndex) Color.White else ForgeMuted,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
internal fun ChaptersDialog(
    chapters: List<MediaChapter>,
    onDismiss: () -> Unit,
    onJump: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Chapters", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (chapters.isEmpty()) {
                    Text("No timed chapters in this file.", color = ForgeMuted)
                } else {
                    chapters.forEach { ch ->
                        Text(
                            text = "${formatDuration(ch.startMs)}  ·  ${ch.title}",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJump(ch.startMs) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
internal fun LyricsDialog(
    lyrics: LyricsResult?,
    loading: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text(stringResource(R.string.lyrics), color = Color.White) },
        text = {
            when {
                loading -> Text("…", color = ForgeMuted)
                lyrics == null || lyrics.text.isBlank() -> {
                    Column {
                        Text(stringResource(R.string.lyrics_empty), color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.lyrics_empty_sub), color = ForgeMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.lyrics_source, lyrics.source),
                            color = ForgeMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            lyrics.text,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done), color = ForgeAccent)
            }
        },
    )
}
