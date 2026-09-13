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

internal fun collectTrackDetailLabels(tracks: Tracks, type: @C.TrackType Int): List<String> {
    val out = mutableListOf<String>()
    tracks.groups.forEach { group ->
        if (group.type != type) return@forEach
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val format = group.getTrackFormat(i)
            val parts = mutableListOf<String>()
            format.sampleMimeType?.let { parts += it.substringAfter('/') }
            if (format.width > 0 && format.height > 0) parts += "${format.width}×${format.height}"
            if (format.frameRate > 0) parts += "%.0f fps".format(format.frameRate)
            if (format.bitrate > 0) parts += "${format.bitrate / 1000} kbps"
            if (format.channelCount > 0) parts += "${format.channelCount} ch"
            if (format.sampleRate > 0) parts += "${format.sampleRate} Hz"
            format.language?.takeIf { it.isNotBlank() && it != "und" }?.let { parts += it }
            format.label?.takeIf { it.isNotBlank() }?.let { parts.add(0, it) }
            out += parts.joinToString(" · ").ifBlank { "Track ${out.size + 1}" }
        }
    }
    return out
}

internal fun collectTracks(tracks: Tracks, type: @C.TrackType Int): List<TrackChoice> {
    val out = mutableListOf<TrackChoice>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != type) return@forEachIndexed
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val format = group.getTrackFormat(i)
            val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
            val label = when (type) {
                C.TRACK_TYPE_VIDEO -> {
                    val parts = mutableListOf<String>()
                    format.label?.takeIf { it.isNotBlank() }?.let { parts += it }
                    if (format.height > 0) parts += "${format.height}p"
                    else if (format.width > 0 && format.height > 0) parts += "${format.width}×${format.height}"
                    if (format.bitrate > 0) parts += "${format.bitrate / 1000} kbps"
                    if (format.frameRate > 0) parts += "%.0f fps".format(format.frameRate)
                    parts.joinToString(" · ").ifBlank { "Video ${out.size + 1}" }
                }
                C.TRACK_TYPE_TEXT -> format.label?.takeIf { it.isNotBlank() } ?: lang ?: "Subtitle ${out.size + 1}"
                else -> format.label?.takeIf { it.isNotBlank() } ?: lang ?: "Audio ${out.size + 1}"
            }
            out += TrackChoice(
                groupIndex = groupIndex,
                trackIndex = i,
                label = label,
                selected = group.isTrackSelected(i),
            )
        }
    }
    return out
}

internal fun selectTrack(player: Player?, type: @C.TrackType Int, choice: TrackChoice) {
    val p = player ?: return
    val groups = p.currentTracks.groups
    if (choice.groupIndex !in groups.indices) return
    val group = groups[choice.groupIndex]
    if (group.type != type) return
    p.trackSelectionParameters = p.trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(type)
        .setTrackTypeDisabled(type, false)
        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, choice.trackIndex))
        .build()
}

internal fun enableTextTracks(player: Player?, enabled: Boolean) {
    val p = player ?: return
    p.trackSelectionParameters = p.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
        .build()
}

internal fun subtitleMimeForUri(uri: android.net.Uri): String {
    val path = (uri.lastPathSegment ?: uri.toString()).lowercase()
    return if (path.endsWith(".vtt")) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
}

internal fun buildPlayerMediaItem(
    item: ForgeMediaItem,
    subtitleUri: android.net.Uri? = null,
): MediaItem {
    val builder = MediaItem.Builder()
        .setUri(item.uri)
        .setMediaId(item.uri.toString())
        .setMimeType(item.mimeType.takeIf { it.isNotBlank() && '*' !in it })
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(item.title.ifBlank { "Media" })
                .setArtist(item.artist.ifBlank { "Forge" })
                .setArtworkUri(item.albumArtUri)
                .setIsPlayable(true)
                .build(),
        )
    if (subtitleUri != null) {
        val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(subtitleMimeForUri(subtitleUri))
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        builder.setSubtitleConfigurations(listOf(subtitle))
    }
    return builder.build()
}

internal fun restoreRememberedTracks(
    player: Player,
    uri: String,
    prefs: com.gketch.forge.data.TrackPrefs,
    onSubtitlesEnabled: (Boolean) -> Unit,
) {
    runCatching {
        if (prefs.textDisabled) {
            onSubtitlesEnabled(false)
            enableTextTracks(player, false)
        } else {
            onSubtitlesEnabled(true)
            enableTextTracks(player, true)
            val text = collectTracks(player.currentTracks, C.TRACK_TYPE_TEXT)
            val want = prefs.textLabel
            if (!want.isNullOrBlank()) {
                val match = text.firstOrNull { it.label.equals(want, ignoreCase = true) }
                    ?: text.firstOrNull { it.label.contains(want, ignoreCase = true) }
                if (match != null) selectTrack(player, C.TRACK_TYPE_TEXT, match)
            }
        }
        val audio = collectTracks(player.currentTracks, C.TRACK_TYPE_AUDIO)
        val wantAudio = prefs.audioLabel
        if (!wantAudio.isNullOrBlank()) {
            val match = audio.firstOrNull { it.label.equals(wantAudio, ignoreCase = true) }
                ?: audio.firstOrNull { it.label.contains(wantAudio, ignoreCase = true) }
            if (match != null) selectTrack(player, C.TRACK_TYPE_AUDIO, match)
        }
    }
}

internal fun applyExternalSubtitle(player: Player, uri: android.net.Uri) {
    val current = player.currentMediaItem ?: return
    val index = player.currentMediaItemIndex
    val position = player.currentPosition
    val ready = player.playWhenReady
    val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
        .setMimeType(subtitleMimeForUri(uri))
        .setLanguage("und")
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .build()
    val updated = current.buildUpon()
        .setSubtitleConfigurations(listOf(subtitle))
        .build()
    player.replaceMediaItem(index, updated)
    player.seekTo(index, position)
    player.prepare()
    player.playWhenReady = ready
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .build()
}
