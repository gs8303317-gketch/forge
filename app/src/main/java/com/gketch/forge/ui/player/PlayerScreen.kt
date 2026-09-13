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
import com.gketch.forge.playback.ForgeVideoTransform
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
import com.gketch.forge.playback.ForgeCrossfade
import com.gketch.forge.playback.ForgeLoudness
import com.gketch.forge.playback.PlaybackErrors
import com.gketch.forge.data.LyricsRepository
import com.gketch.forge.data.LyricsResult
import com.gketch.forge.playback.ForgeEngine
import com.gketch.forge.playback.ForgePlayerPrefsStore
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeBehavior
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.data.SidecarSubtitles
import com.gketch.forge.data.TrackPrefsStore
import com.gketch.forge.data.MediaPlaybackPrefsStore
import com.gketch.forge.data.SubtitleOutline
import com.gketch.forge.data.WatchedStore
import com.gketch.forge.playback.ForgeAudioFx
import com.gketch.forge.playback.ForgeBalance
import com.gketch.forge.playback.ForgeVideoColor
import com.gketch.forge.playback.SeriesEpisode
import com.gketch.forge.data.MediaRepository
import com.gketch.forge.playback.ForgeEqualizer
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

private val SPEED_PRESETS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
private val SLEEP_OPTIONS = listOf(15, 30, 45, 60)
private val SUBTITLE_SIZES = listOf(16f, 20f, 24f, 28f, 32f)

private enum class AspectMode(
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

private enum class Panel { None, Speed, Aspect, Sleep, Subtitle, Audio, Quality, Equalizer, Orientation, AbLoop, MediaInfo, Bookmarks, VolumeBoost, SubDelay, AudioDelay, Queue, Chapters, JumpToTime, VideoColor, AudioBalance, Lyrics, Transform, QuickSubDelay, QuickAudioDelay, NightFilter }

private data class MediaChapter(val title: String, val startMs: Long)

private enum class OrientationLock(val label: String) {
    AUTO("Auto"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
}

private data class TrackChoice(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean,
)

@Composable
fun PlayerScreen(
    queue: List<ForgeMediaItem>,
    startIndex: Int,
    openId: Long = 0L,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val scope = rememberCoroutineScope()
    val resumeStore = remember { ResumeStore(context) }
    val recentStore = remember { RecentStore(context) }
    val brightnessStore = remember { BrightnessStore(context) }
    val mediaRepository = remember { MediaRepository(context) }
    val watchedStore = remember { WatchedStore(context) }
    val appSettingsStore = remember { AppSettingsStore(context) }
    val trackPrefsStore = remember { TrackPrefsStore(context) }
    val mediaPlaybackPrefsStore = remember { MediaPlaybackPrefsStore(context) }
    val controller = rememberPlayerController()

    var playQueue by remember { mutableStateOf(queue) }
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))) }
    val current = playQueue.getOrNull(index)
    var appSettings by remember { mutableStateOf(AppSettings()) }
    var resumePromptMs by remember { mutableStateOf<Long?>(null) }
    var pendingResumeUri by remember { mutableStateOf<String?>(null) }
    /** Bumped on each open/retarget so in-flight resume prompts cannot race onto a new item. */
    var resumeEpoch by remember { mutableIntStateOf(0) }
    var savedSpeed by remember { mutableFloatStateOf(1f) }
    var holdBoosting by remember { mutableStateOf(false) }
    var uriSpeedLocked by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var errorRetryCount by remember { mutableIntStateOf(0) }
    var errorRetrying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFromSlider by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var surfaceBrightness by remember { mutableFloatStateOf(BrightnessStore.DEFAULT) }
    var videoZoom by remember { mutableFloatStateOf(1f) }
    var videoPan by remember { mutableStateOf(Offset.Zero) }
    var nightStrength by remember { mutableFloatStateOf(0f) }
    var seriesPrompt by remember { mutableStateOf<ForgeMediaItem?>(null) }
    var seriesHandledUri by remember { mutableStateOf<String?>(null) }
    val scrubHold = remember { ScrubHold() }
    var hasVideo by remember { mutableStateOf(current?.kind == MediaKind.VIDEO) }
    var speed by remember { mutableFloatStateOf(1f) }
    var panel by remember { mutableStateOf(Panel.None) }
    var controlsVisible by remember { mutableStateOf(true) }
    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    var loadedKey by remember { mutableStateOf<String?>(null) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var shuffleOn by remember { mutableStateOf(false) }
    var aspect by remember { mutableStateOf(AspectMode.FIT) }
    var subtitleSizeSp by remember { mutableFloatStateOf(20f) }
    var subtitleColor by remember { mutableStateOf(SubtitleColor.WHITE) }
    var subtitleBackground by remember { mutableStateOf(SubtitleBackground.SEMI) }
    var subtitlePosition by remember { mutableStateOf(SubtitlePosition.BOTTOM) }
    var subtitleOutline by remember { mutableStateOf(SubtitleOutline.OUTLINE) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var externalSubtitleUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tracksRestoredUri by remember { mutableStateOf<String?>(null) }
    var autoSidecarForUri by remember { mutableStateOf<String?>(null) }
    var textTracks by remember { mutableStateOf<List<TrackChoice>>(emptyList()) }
    var audioTracks by remember { mutableStateOf<List<TrackChoice>>(emptyList()) }
    var videoTracks by remember { mutableStateOf<List<TrackChoice>>(emptyList()) }
    var sleepMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepDeadlineMs by remember { mutableLongStateOf(0L) }
    var sleepRemainingSec by remember { mutableIntStateOf(0) }
    var moreMenu by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var orientationLock by remember { mutableStateOf(OrientationLock.AUTO) }
    var abPointA by remember { mutableStateOf<Long?>(null) }
    var abPointB by remember { mutableStateOf<Long?>(null) }
    var abLoopEnabled by remember { mutableStateOf(false) }
    var eqEnabled by remember { mutableStateOf(ForgeEqualizer.enabled) }
    var eqBands by remember { mutableStateOf(ForgeEqualizer.bands()) }
    var eqPreset by remember { mutableStateOf(ForgeEqualizer.presetName) }
    val bookmarkStore = remember { BookmarkStore(context) }
    var allBookmarks by remember { mutableStateOf<List<MediaBookmark>>(emptyList()) }
    var controlsLocked by remember { mutableStateOf(false) }
    var controlsHideToken by remember { mutableIntStateOf(0) }
    var subtitleDelayMs by remember { mutableIntStateOf(0) }
    var audioDelayMs by remember { mutableIntStateOf(ForgeEngine.audioDelayMs) }
    val enginePrefsStore = remember { ForgePlayerPrefsStore(context) }
    var volumeBoostPercent by remember { mutableIntStateOf(ForgeLoudness.boostPercent) }
    var snapshotMessage by remember { mutableStateOf<String?>(null) }
    var displayedCues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var videoTrackLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<MediaChapter>>(emptyList()) }
    var lyricsResult by remember { mutableStateOf<LyricsResult?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var bassOn by remember { mutableStateOf(ForgeAudioFx.bassEnabled) }
    var virtOn by remember { mutableStateOf(ForgeAudioFx.virtualizerEnabled) }
    var videoBrightness by remember { mutableFloatStateOf(ForgeVideoColor.current.brightness) }
    var videoContrast by remember { mutableFloatStateOf(ForgeVideoColor.current.contrast) }
    var videoSaturation by remember { mutableFloatStateOf(ForgeVideoColor.current.saturation) }
    var audioBalance by remember { mutableIntStateOf(ForgeBalance.balance) }
    var autoMarkedUri by remember { mutableStateOf<String?>(null) }
    var sleepBaseVolume by remember { mutableFloatStateOf(1f) }
    var playAsAudio by remember { mutableStateOf(false) }
    var frameStepAvailable by remember { mutableStateOf(true) }
    val progress = remember { PlayerProgressState() }
    var statsVisible by remember { mutableStateOf(false) }
    var mirrorH by remember { mutableStateOf(false) }
    var mirrorV by remember { mutableStateOf(false) }
    var rotationDeg by remember { mutableIntStateOf(0) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // Non-persistable grant is still usable for this session
            }
            externalSubtitleUri = uri
            autoSidecarForUri = null
            controller?.let { applyExternalSubtitle(it, uri) }
            subtitlesEnabled = true
            enableTextTracks(controller, true)
            current?.uri?.toString()?.let { u ->
                scope.launch { runCatching { trackPrefsStore.saveTextDisabled(u, false) } }
            }
            panel = Panel.None
        }
    }

    LaunchedEffect(Unit) {
        bookmarkStore.bookmarks.collect { allBookmarks = it }
    }

    LaunchedEffect(Unit) {
        var appliedDefaultSpeed = false
        appSettingsStore.settings.collect { s ->
            appSettings = s
            subtitleSizeSp = s.subtitleSizeSp
            subtitleColor = s.subtitleColor
            subtitleBackground = s.subtitleBackground
            subtitlePosition = s.subtitlePosition
            subtitleOutline = s.subtitleOutline
            ForgeEngine.setPauseAtEndOfMediaItems(!s.autoplayNext)
            ForgeCrossfade.setDurationSec(s.crossfade.seconds)
            ForgeLoudness.setNormalizeEnabled(s.loudnessNormalize)
            if (!appliedDefaultSpeed) {
                appliedDefaultSpeed = true
                if (!uriSpeedLocked) {
                    speed = s.defaultPlaybackSpeed
                    savedSpeed = s.defaultPlaybackSpeed
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(surfaceBrightness, playerViewRef, nightStrength) {
        applySurfaceBrightness(surfaceBrightness, playerViewRef)
        runCatching {
            ForgeVideoColor.setNightStrength(nightStrength)
            ForgeVideoColor.applyTo(playerViewRef)
        }
    }

    LaunchedEffect(playAsAudio, isPlaying) {
        val window = activity?.window ?: return@LaunchedEffect
        if (playAsAudio) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        clearWindowBrightness(activity)
        if (!playAsAudio) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val insetsController = window?.let { w ->
            runCatching {
                WindowCompat.getInsetsController(w, w.decorView).apply {
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }.getOrNull()
        }
        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            inPip = info.isInPictureInPictureMode
            if (info.isInPictureInPictureMode) {
                controlsVisible = false
                panel = Panel.None
            }
        }
        activity?.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            runCatching { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            clearWindowBrightness(activity)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.updatePipParams(allowed = false)
            activity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    val currentUriState = rememberUpdatedState(current?.uri?.toString())
    val resumeBehaviorState = rememberUpdatedState(appSettings.resumeBehavior)
    var rememberResumeChoice by remember { mutableStateOf(false) }
    val resumeIgnoreUris = remember { mutableSetOf<String>() }
    DisposableEffect(controller) {
        val player = controller
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlayerError(error: PlaybackException) {
                    val message = PlaybackErrors.userMessage(error)
                    val uiFault = PlaybackErrors.isLikelyUiFault(error)
                    if (uiFault || PlaybackErrors.shouldAutoRetry(error, errorRetryCount)) {
                        if (!uiFault) errorRetryCount += 1
                        errorRetrying = true
                        playerError = null
                        val pos = runCatching { player.currentPosition }.getOrDefault(0L)
                        scope.launch {
                            delay(250)
                            val ok = runCatching {
                                player.prepare()
                                if (pos > 0L) player.seekTo(pos)
                                player.play()
                            }.isSuccess
                            errorRetrying = false
                            // Never pin a permanent overlay for Compose/UI faults.
                            if (!ok && !uiFault) {
                                playerError = message
                                isPlaying = false
                            }
                        }
                    } else {
                        playerError = message
                        isPlaying = false
                        errorRetrying = false
                    }
                }

                override fun onPlayerErrorChanged(error: PlaybackException?) {
                    if (error == null) {
                        playerError = null
                        errorRetrying = false
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    autoMarkedUri = null
                    playerError = null
                    errorRetryCount = 0
                    errorRetrying = false
                    abPointA = null
                    abPointB = null
                    abLoopEnabled = false
                    displayedCues = emptyList()
                    chapters = emptyList()
                    lyricsResult = null
                    runCatching { ForgeCrossfade.onMediaItemTransition(player) }
                    // Keep play-as-audio mode across queue items (user toggle).
                    val newIndex = player.currentMediaItemIndex
                    if (newIndex in playQueue.indices) {
                        index = newIndex
                        playQueue.getOrNull(newIndex)?.let { item ->
                            scope.launch { recentStore.record(item) }
                        }
                    }
                    val uri = mediaItem?.mediaId
                        ?: mediaItem?.localConfiguration?.uri?.toString()
                    // Reset pinch zoom/pan on item change (session zoom is per-item).
                    videoZoom = 1f
                    videoPan = Offset.Zero
                    seriesHandledUri = null
                    seriesPrompt = null
                    if (uri != null) {
                        scope.launch {
                            runCatching {
                                surfaceBrightness = brightnessStore.get(uri) ?: BrightnessStore.DEFAULT
                                applySurfaceBrightness(surfaceBrightness, playerViewRef)
                            }
                            runCatching {
                                val prefs = mediaPlaybackPrefsStore.get(uri)
                                prefs.speed?.let { spd ->
                                    uriSpeedLocked = true
                                    if (!holdBoosting) {
                                        speed = spd
                                        savedSpeed = spd
                                        player.setPlaybackSpeed(spd)
                                    }
                                }
                                prefs.aspect?.let { name ->
                                    AspectMode.entries.find { it.name == name }?.let { aspect = it }
                                }
                            }
                        }
                    } else {
                        surfaceBrightness = BrightnessStore.DEFAULT
                        applySurfaceBrightness(surfaceBrightness, playerViewRef)
                    }
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                        if (uri != null) {
                            // Load/reopen path may seekTo(index) and apply resume itself — skip duplicate.
                            if (uri in resumeIgnoreUris) {
                                resumeIgnoreUris.remove(uri)
                            } else {
                                val transitionEpoch = resumeEpoch
                                scope.launch {
                                    applyResumePolicy(
                                        player = player,
                                        uri = uri,
                                        behavior = resumeBehaviorState.value,
                                        resumeStore = resumeStore,
                                        onPrompt = { saved ->
                                            if (transitionEpoch == resumeEpoch) {
                                                player.pause()
                                                pendingResumeUri = uri
                                                resumePromptMs = saved
                                                rememberResumeChoice = false
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        hasVideo = true
                        videoWidth = videoSize.width
                        videoHeight = videoSize.height
                        activity?.updatePipParams(
                            allowed = true,
                            aspect = Rational(videoSize.width, videoSize.height),
                        )
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    textTracks = collectTracks(tracks, C.TRACK_TYPE_TEXT)
                    audioTracks = collectTracks(tracks, C.TRACK_TYPE_AUDIO)
                    videoTracks = collectTracks(tracks, C.TRACK_TYPE_VIDEO)
                    videoTrackLabels = collectTrackDetailLabels(tracks, C.TRACK_TYPE_VIDEO)
                    val video = tracks.groups.any { group ->
                        group.type == C.TRACK_TYPE_VIDEO && group.isSelected
                    }
                    if (tracks.groups.isNotEmpty()) {
                        hasVideo = video || (current?.kind == MediaKind.VIDEO && player.videoSize.width > 0)
                    }
                    val uri = player.currentMediaItem?.mediaId
                    if (!uri.isNullOrBlank() && tracksRestoredUri != uri && tracks.groups.isNotEmpty()) {
                        tracksRestoredUri = uri
                        scope.launch {
                            runCatching {
                                val prefs = trackPrefsStore.get(uri)
                                restoreRememberedTracks(
                                    player = player,
                                    uri = uri,
                                    prefs = prefs,
                                    onSubtitlesEnabled = { subtitlesEnabled = it },
                                )
                                // Refresh track lists after restore
                                textTracks = collectTracks(player.currentTracks, C.TRACK_TYPE_TEXT)
                                audioTracks = collectTracks(player.currentTracks, C.TRACK_TYPE_AUDIO)
                            }
                        }
                    }
                }

                override fun onRepeatModeChanged(mode: Int) {
                    repeatMode = mode
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    shuffleOn = shuffleModeEnabled
                }

                override fun onMetadata(metadata: Metadata) {
                    val found = parseChapters(metadata)
                    if (found.isNotEmpty()) {
                        chapters = (chapters + found).distinctBy { it.startMs }.sortedBy { it.startMs }
                    }
                }

                override fun onCues(cueGroup: CueGroup) {
                    val offsetMs = subtitleDelayMs
                    if (!subtitlesEnabled) {
                        displayedCues = emptyList()
                        return
                    }
                    if (offsetMs <= 0) {
                        // Negative offset: true early-shift needs renderer access; show immediately
                        displayedCues = cueGroup.cues
                    } else {
                        scope.launch {
                            kotlinx.coroutines.delay(offsetMs.toLong())
                            displayedCues = cueGroup.cues
                        }
                    }
                }
            }
            player.addListener(listener)
            isPlaying = player.isPlaying
            repeatMode = player.repeatMode
            shuffleOn = player.shuffleModeEnabled
            textTracks = collectTracks(player.currentTracks, C.TRACK_TYPE_TEXT)
            audioTracks = collectTracks(player.currentTracks, C.TRACK_TYPE_AUDIO)
            onDispose {
                if (scrubHold.active) {
                    runCatching { ForgeEngine.setScrubSeek(false) }
                    runCatching { player.volume = scrubHold.volume }
                    scrubHold.active = false
                }
                val uri = currentUriState.value
                if (uri != null) {
                    scope.launch {
                        resumeStore.savePosition(uri, player.currentPosition, player.duration)
                    }
                }
                player.removeListener(listener)
            }
        }
    }

    LaunchedEffect(queue, startIndex) {
        playQueue = queue
        index = startIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(controller, playQueue, startIndex, openId) {
        val player = controller ?: return@LaunchedEffect
        val key = playQueue.joinToString("|") { it.uri.toString() } + "#$startIndex#$openId"
        val playerUris = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        val queueUris = playQueue.map { it.uri.toString() }
        if (playQueue.isEmpty()) {
            playerError = "Nothing to play"
            return@LaunchedEffect
        }
        val safeStart = startIndex.coerceIn(0, (playQueue.size - 1).coerceAtLeast(0))
        val startUri = playQueue.getOrNull(safeStart)?.uri?.toString()
        ForgeEngine.setPauseAtEndOfMediaItems(!appSettings.autoplayNext)

        // Same playlist already loaded: honor new openId / startIndex (reopen, recent, retarget)
        // without rebuilding media items. Expand-mini-player keeps the same openId → no-op.
        if (playerUris == queueUris && player.mediaItemCount > 0) {
            if (loadedKey == key) {
                index = player.currentMediaItemIndex.coerceIn(0, (playQueue.size - 1).coerceAtLeast(0))
                return@LaunchedEffect
            }
            loadedKey = key
            externalSubtitleUri = null
            autoSidecarForUri = null
            tracksRestoredUri = null
            resumeEpoch += 1
            val openEpoch = resumeEpoch
            resumePromptMs = null
            pendingResumeUri = null
            rememberResumeChoice = false
            playerError = null
            errorRetryCount = 0
            errorRetrying = false
            index = safeStart
            try {
                if (startUri != null) resumeIgnoreUris.add(startUri)
                if (player.currentMediaItemIndex != safeStart) {
                    player.seekTo(safeStart, 0L)
                } else {
                    player.seekTo(0L)
                }
                player.setPlaybackSpeed(speed)
                player.repeatMode = repeatMode
                player.shuffleModeEnabled = shuffleOn
                if (startUri != null) {
                    // If transition already consumed the ignore entry, still apply here.
                    resumeIgnoreUris.remove(startUri)
                    applyResumePolicy(
                        player = player,
                        uri = startUri,
                        behavior = appSettings.resumeBehavior,
                        resumeStore = resumeStore,
                        onPrompt = { saved ->
                            if (openEpoch == resumeEpoch) {
                                player.pause()
                                pendingResumeUri = startUri
                                resumePromptMs = saved
                                rememberResumeChoice = false
                            }
                        },
                    )
                } else {
                    player.play()
                }
            } catch (t: Throwable) {
                if (startUri != null) resumeIgnoreUris.remove(startUri)
                playerError = t.message ?: "Could not start playback"
                return@LaunchedEffect
            }
            playQueue.getOrNull(safeStart)?.let { recentStore.record(it) }
            if (startUri != null) {
                runCatching { surfaceBrightness = brightnessStore.get(startUri) ?: BrightnessStore.DEFAULT }
                runCatching {
                    val prefs = mediaPlaybackPrefsStore.get(startUri)
                    prefs.speed?.let { spd ->
                        uriSpeedLocked = true
                        speed = spd
                        savedSpeed = spd
                        player.setPlaybackSpeed(spd)
                    }
                    prefs.aspect?.let { name ->
                        AspectMode.entries.find { it.name == name }?.let { aspect = it }
                    }
                }
            }
            return@LaunchedEffect
        }

        val alreadySame = player.currentMediaItem?.mediaId == playQueue.getOrNull(startIndex)?.uri?.toString() &&
            player.mediaItemCount == playQueue.size
        if (alreadySame && loadedKey == key) return@LaunchedEffect
        loadedKey = key
        externalSubtitleUri = null
        autoSidecarForUri = null
        tracksRestoredUri = null
        resumeEpoch += 1
        val openEpoch = resumeEpoch
        resumePromptMs = null
        pendingResumeUri = null
        rememberResumeChoice = false
        playerError = null
        errorRetryCount = 0
        errorRetrying = false

        val items = withContext(Dispatchers.IO) {
            playQueue.mapNotNull { item ->
                val uri = item.uri
                if (uri == android.net.Uri.EMPTY || uri.toString().isBlank()) return@mapNotNull null
                val sidecar = runCatching { SidecarSubtitles.find(context, item) }.getOrNull()
                buildPlayerMediaItem(item, sidecar)
            }
        }
        // Reflect auto-loaded sidecar on the start item for subtitle UI state.
        runCatching {
            val startItem = playQueue.getOrNull(safeStart)
            if (startItem != null) {
                val sc = withContext(Dispatchers.IO) {
                    SidecarSubtitles.find(context, startItem)
                }
                if (sc != null) {
                    externalSubtitleUri = sc
                    autoSidecarForUri = startItem.uri.toString()
                    subtitlesEnabled = true
                } else {
                    autoSidecarForUri = null
                }
            }
        }
        if (items.isEmpty()) {
            playerError = "Invalid media URI"
            return@LaunchedEffect
        }
        val startUri2 = playQueue.getOrNull(safeStart)?.uri?.toString()
            ?: items.getOrNull(safeStart)?.mediaId
        val resumeAt = try {
            if (startUri2 != null) resumeStore.getPosition(startUri2) else 0L
        } catch (_: Exception) {
            0L
        }
        val behavior = appSettings.resumeBehavior
        val shouldPrompt = resumeAt >= ResumeStore.RESUME_PROMPT_MS && behavior == ResumeBehavior.ASK
        val startPosition = when {
            resumeAt < ResumeStore.RESUME_PROMPT_MS &&
                resumeAt >= ResumeStore.MIN_SAVE_MS &&
                behavior != ResumeBehavior.ALWAYS_START_OVER -> resumeAt
            behavior == ResumeBehavior.ALWAYS_CONTINUE && resumeAt >= ResumeStore.MIN_SAVE_MS -> resumeAt
            else -> 0L
        }
        try {
            if (shouldPrompt) {
                player.setMediaItems(items, safeStart, 0L)
                player.prepare()
                player.setPlaybackSpeed(speed)
                player.repeatMode = repeatMode
                player.shuffleModeEnabled = shuffleOn
                player.pause()
                if (openEpoch == resumeEpoch) {
                    pendingResumeUri = startUri2
                    resumePromptMs = resumeAt
                    rememberResumeChoice = false
                }
            } else {
                player.setMediaItems(items, safeStart, startPosition.coerceAtLeast(0L))
                player.prepare()
                player.setPlaybackSpeed(speed)
                player.repeatMode = repeatMode
                player.shuffleModeEnabled = shuffleOn
                player.play()
            }
        } catch (t: Throwable) {
            playerError = t.message ?: "Could not start playback"
            return@LaunchedEffect
        }
        playQueue.getOrNull(safeStart)?.let { recentStore.record(it) }
        if (startUri2 != null) {
            runCatching { surfaceBrightness = brightnessStore.get(startUri2) ?: BrightnessStore.DEFAULT }
            runCatching {
                val prefsUri = startUri2
                if (prefsUri != null) {
                    val prefs = mediaPlaybackPrefsStore.get(prefsUri)
                    prefs.speed?.let { spd ->
                        uriSpeedLocked = true
                        speed = spd
                        savedSpeed = spd
                        player.setPlaybackSpeed(spd)
                    }
                    prefs.aspect?.let { name ->
                        AspectMode.entries.find { it.name == name }?.let { aspect = it }
                    }
                }
            }
        }
    }

    LaunchedEffect(hasVideo, current?.kind, inPip, orientationLock, videoWidth, videoHeight) {
        val video = hasVideo || current?.kind == MediaKind.VIDEO
        if (!inPip) {
            activity?.requestedOrientation = when (orientationLock) {
                OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                // Auto follows the sensor / user rotation. Never force landscape —
                // portrait clips stay portrait; landscape clips can rotate freely.
                OrientationLock.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }
        activity?.updatePipParams(allowed = video && !inPip)
    }

    LaunchedEffect(controller, abLoopEnabled, abPointA, abPointB) {
        val player = controller ?: return@LaunchedEffect
        var tick = 0
        while (isActive) {
            if (!scrubbing) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L).takeIf { it > 0 } ?: 0L
                val buffered = player.bufferedPosition.coerceAtLeast(0L)
                val state = player.playbackState
                val isBuf = state == Player.STATE_BUFFERING
                // Always update isolated progress (controls/scrubber subscribe here).
                progress.updateProgress(pos, dur, buffered, isBuf, state)
                // Throttle parent position/duration reads used by dialogs (250ms).
                tick++
                if (tick % 3 == 0 || abLoopEnabled) {
                    positionMs = pos
                    durationMs = dur
                }
                val a = abPointA
                val b = abPointB
                if (abLoopEnabled && a != null && b != null && b > a && pos >= b) {
                    player.seekTo(a)
                    positionMs = a
                    progress.updateProgress(a, dur, buffered, isBuf, state)
                }
                val uri = player.currentMediaItem?.mediaId
                    ?: player.currentMediaItem?.localConfiguration?.uri?.toString()
                if (uri != null && dur > 0L &&
                    pos >= dur - WatchedStore.AUTO_MARK_NEAR_END_MS &&
                    autoMarkedUri != uri
                ) {
                    autoMarkedUri = uri
                    watchedStore.markWatched(uri)
                }
                // Series auto-next (crash-isolated): at end, if last/only queue item looks like
                // an episode, offer or auto-append the next file from the same folder.
                if (state == Player.STATE_ENDED &&
                    appSettings.seriesAutoNext &&
                    uri != null &&
                    seriesHandledUri != uri
                ) {
                    seriesHandledUri = uri
                    val atQueueEnd = index >= playQueue.lastIndex
                    val cur = playQueue.getOrNull(index)
                    if (atQueueEnd && cur != null && SeriesEpisode.looksLikeEpisode(cur.title)) {
                        scope.launch {
                            runCatching {
                                maybeSeriesAutoNext(
                                    current = cur,
                                    mediaRepository = mediaRepository,
                                    onOffer = { next -> seriesPrompt = next },
                                )
                            }
                        }
                    }
                }
                val sleepFading = sleepDeadlineMs > 0L && appSettings.sleepFadeEnabled
                runCatching { ForgeCrossfade.tick(player, pos, dur, sleepFading) }
            }
            delay(100)
        }
    }

    // Stats overlay ≤4 Hz — isolated from 10 Hz progress ticks.
    LaunchedEffect(controller, statsVisible) {
        if (!statsVisible) return@LaunchedEffect
        val player = controller ?: return@LaunchedEffect
        while (isActive) {
            runCatching {
                val vs = player.videoSize
                var fps = 0f
                var bitrate = -1
                val tracks = player.currentTracks
                for (g in tracks.groups) {
                    if (g.type != C.TRACK_TYPE_VIDEO) continue
                    for (i in 0 until g.length) {
                        if (!g.isTrackSelected(i)) continue
                        val f = g.getTrackFormat(i)
                        if (f.frameRate > 1f && f.frameRate < 240f) fps = f.frameRate
                        if (f.bitrate > 0) bitrate = f.bitrate / 1000
                    }
                }
                val dur = progress.durationMs
                val bufferedPct = if (dur > 0L) {
                    ((progress.bufferedMs * 100L) / dur).toInt().coerceIn(0, 100)
                } else 0
                progress.updateStats(
                    width = vs.width,
                    height = vs.height,
                    fps = fps,
                    bitrateKbps = bitrate,
                    bufferedPct = bufferedPct,
                    stateLabel = playbackStateLabel(player.playbackState),
                )
            }
            delay(250)
        }
    }

    // Lyrics are optional — never open MediaMetadataRetriever on the playing URI
    // unless the user opens the Lyrics panel (concurrent MMR + ExoPlayer can fault the decoder).
    LaunchedEffect(panel, index, playQueue) {
        if (panel != Panel.Lyrics) return@LaunchedEffect
        val item = playQueue.getOrNull(index)
        lyricsLoading = true
        lyricsResult = null
        val embeddedDesc = runCatching { controller?.mediaMetadata?.description }.getOrNull()
        val fromMeta = LyricsRepository.fromMedia3Description(embeddedDesc)
        val loaded = runCatching { LyricsRepository.load(context, item) }.getOrNull() ?: fromMeta
        lyricsResult = loaded
        lyricsLoading = false
    }

    LaunchedEffect(panel) {
        if (panel == Panel.Equalizer) {
            eqEnabled = ForgeEqualizer.enabled
            eqBands = ForgeEqualizer.bands()
            eqPreset = ForgeEqualizer.presetName
            bassOn = ForgeAudioFx.bassEnabled
            virtOn = ForgeAudioFx.virtualizerEnabled
        }
    }

    LaunchedEffect(controller) {
        val player = controller ?: return@LaunchedEffect
        while (isActive) {
            delay(5_000)
            val uri = player.currentMediaItem?.mediaId
                ?: player.currentMediaItem?.localConfiguration?.uri?.toString()
            if (uri != null) {
                resumeStore.savePosition(uri, player.currentPosition, player.duration)
            }
        }
    }

    LaunchedEffect(
        controlsVisible,
        isPlaying,
        inPip,
        panel,
        controlsLocked,
        moreMenu,
        scrubbing,
        resumePromptMs,
        controlsHideToken,
        appSettings.chromeHideDelay,
    ) {
        if (controlsLocked) {
            panel = Panel.None
            moreMenu = false
            val hideMs = appSettings.chromeHideDelay.delayMs
            if (hideMs != null && controlsVisible && !inPip) {
                delay(hideMs)
                controlsVisible = false
            }
            return@LaunchedEffect
        }
        val hideMs = appSettings.chromeHideDelay.delayMs ?: return@LaunchedEffect
        val overlayOpen = panel != Panel.None || moreMenu || scrubbing || resumePromptMs != null || seriesPrompt != null
        if (controlsVisible && isPlaying && !inPip && !overlayOpen) {
            delay(hideMs)
            controlsVisible = false
        }
    }

    LaunchedEffect(snapshotMessage) {
        val msg = snapshotMessage ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        snapshotMessage = null
    }

    LaunchedEffect(subtitleDelayMs, subtitlesEnabled) {
        if (!subtitlesEnabled) displayedCues = emptyList()
    }

    LaunchedEffect(sleepDeadlineMs, appSettings.sleepFadeEnabled, appSettings.sleepFadeSeconds, appSettings.sleepEndAction) {
        if (sleepDeadlineMs <= 0L) {
            sleepRemainingSec = 0
            return@LaunchedEffect
        }
        sleepBaseVolume = controller?.volume?.takeIf { it > 0f } ?: 1f
        while (isActive && sleepDeadlineMs > 0L) {
            val left = ((sleepDeadlineMs - System.currentTimeMillis()) / 1000L).toInt()
            if (left <= 0) {
                controller?.volume = sleepBaseVolume
                if (appSettings.sleepEndAction == SleepEndAction.STOP) {
                    controller?.stop()
                } else {
                    controller?.pause()
                }
                sleepMinutes = null
                sleepDeadlineMs = 0L
                sleepRemainingSec = 0
                break
            }
            val fadeSec = appSettings.sleepFadeSeconds.coerceAtLeast(1)
            if (appSettings.sleepFadeEnabled && left <= fadeSec) {
                val frac = left.toFloat() / fadeSec.toFloat()
                controller?.volume = (sleepBaseVolume * frac).coerceIn(0f, 1f)
            }
            sleepRemainingSec = left
            delay(250)
        }
    }

    LaunchedEffect(subtitleSizeSp, subtitleColor, subtitleBackground, subtitlePosition, subtitleOutline, playerViewRef, subtitlesEnabled) {
        playerViewRef?.subtitleView?.apply {
            setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSizeSp)
            val bg = when (subtitleBackground) {
                SubtitleBackground.NONE -> android.graphics.Color.TRANSPARENT
                SubtitleBackground.SEMI -> android.graphics.Color.argb(140, 0, 0, 0)
                SubtitleBackground.BLACK -> android.graphics.Color.BLACK
            }
            val edgeType = when (subtitleOutline) {
                SubtitleOutline.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                SubtitleOutline.STRONG -> CaptionStyleCompat.EDGE_TYPE_RAISED
                SubtitleOutline.SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            }
            setStyle(
                CaptionStyleCompat(
                    subtitleColor.argb,
                    bg,
                    android.graphics.Color.TRANSPARENT,
                    edgeType,
                    android.graphics.Color.BLACK,
                    null,
                ),
            )
            val frac = subtitlePosition.bottomFraction
            setBottomPaddingFraction(frac)
        }
    }

    val leavePlayer: () -> Unit = {
        val player = controller
        val uri = current?.uri?.toString()
        if (player != null && uri != null) {
            scope.launch { resumeStore.savePosition(uri, player.currentPosition, player.duration) }
        }
        onBack()
    }

    BackHandler {
        if (controlsLocked) {
            controlsLocked = false
            controlsVisible = true
            controlsHideToken++
            return@BackHandler
        }
        leavePlayer()
    }

    val showChrome = controlsVisible && !inPip && !controlsLocked
    val showLockChrome = controlsVisible && !inPip && controlsLocked
    val isVideoSurface = !playAsAudio && (hasVideo || current?.kind == MediaKind.VIDEO)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBlack),
    ) {
        // Keep the player surface mounted even on errors so Retry can re-prepare.
        // Composition / chrome bugs must not replace the whole tree with a fake "media" failure.
        if (controller == null) {
            CircularProgressIndicator(
                color = ForgeAccent,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isVideoSurface) {
                    AndroidView(
                        factory = { ctx ->
                            val pv = android.view.LayoutInflater.from(ctx)
                                .inflate(com.gketch.forge.R.layout.forge_player_view, null, false)
                                as PlayerView
                            pv.apply {
                                useController = false
                                resizeMode = aspect.resizeMode
                                this.player = controller
                                playerViewRef = this
                                subtitleView?.visibility =
                                    if (subtitleDelayMs != 0) android.view.View.INVISIBLE
                                    else android.view.View.VISIBLE
                                ForgeVideoColor.applyTo(this)
                                applyForcedAspect(this, aspect)
                            }
                        },
                        update = {
                            // Avoid re-binding / recoloring every position tick.
                            if (it.player !== controller) it.player = controller
                            if (it.resizeMode != aspect.resizeMode) it.resizeMode = aspect.resizeMode
                            playerViewRef = it
                            val subVis = if (subtitleDelayMs != 0) android.view.View.INVISIBLE
                                else android.view.View.VISIBLE
                            if (it.subtitleView?.visibility != subVis) {
                                it.subtitleView?.visibility = subVis
                            }
                            applyForcedAspect(it, aspect)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (rotationDeg != 0 || mirrorH || mirrorV || videoZoom > 1.01f) {
                                    Modifier.graphicsLayer {
                                        val sideways = rotationDeg % 180 != 0
                                        val fit = if (sideways && size.width > 0f && size.height > 0f) {
                                            maxOf(size.width / size.height, size.height / size.width)
                                        } else {
                                            1f
                                        }
                                        val z = videoZoom.coerceIn(1f, 6f)
                                        rotationZ = rotationDeg.toFloat()
                                        scaleX = (if (mirrorH) -fit else fit) * z
                                        scaleY = (if (mirrorV) -fit else fit) * z
                                        translationX = if (z > 1.01f) videoPan.x else 0f
                                        translationY = if (z > 1.01f) videoPan.y else 0f
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                } else {
                    AudioArtwork(title = current?.title.orEmpty())
                }

                // Video-only brightness: black overlay dims (0–100%); boost >100% via ColorMatrix.
                // UI chrome stays undimmed (never window screenBrightness).
                if (isVideoSurface && surfaceBrightness < 0.999f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(
                                    alpha = (1f - surfaceBrightness).coerceIn(0f, 0.99f),
                                ),
                            ),
                    )
                }

                // Buffering HUD — crash-isolated spinner over the surface.
                if (progress.buffering && !inPip) {
                    CircularProgressIndicator(
                        color = ForgeAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                    )
                }

                if (statsVisible && !inPip) {
                    StatsOverlay(
                        progress = progress,
                        speed = speed,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(start = 12.dp, top = 56.dp),
                    )
                }

                val density = LocalDensity.current
                val excludeTopPx = with(density) { if (showChrome) 56.dp.toPx() else 0f }
                val excludeBottomPx = with(density) {
                    when {
                        showChrome -> 108.dp.toPx()
                        showLockChrome -> 72.dp.toPx()
                        else -> 48.dp.toPx()
                    }
                }
                // Slider scrub: large centered timecode (gesture SeekHud covers swipe seek).
                if (scrubFromSlider && scrubbing && !inPip && isVideoSurface) {
                    val dur = progress.durationMs
                    val pos = if (dur > 0L) (scrubValue * dur).toLong() else 0L
                    ScrubTimecodeHud(elapsedMs = pos, totalMs = dur)
                }

                if (!inPip) {
                    PlayerGestureLayer(
                        durationMs = durationMs,
                        positionMs = positionMs,
                        seekSeconds = appSettings.seekSeconds,
                        onSeek = { target ->
                            finishVideoScrub(controller, target, scrubHold)
                            positionMs = target
                            progress.updateProgress(
                                target,
                                durationMs,
                                progress.bufferedMs,
                                progress.buffering,
                                progress.playbackState,
                            )
                            scrubbing = false
                            scrubFromSlider = false
                        },
                        onSeekPreview = { target ->
                            val dur = durationMs.coerceAtLeast(1L)
                            scrubbing = true
                            scrubFromSlider = false
                            scrubValue = (target.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                            startVideoScrub(controller, scrubHold)
                            previewSeekTo(controller, target, scrubHold)
                        },
                        onDoubleTapSeek = { back ->
                            val deltaMs = appSettings.seekSeconds * 1000L
                            val delta = if (back) -deltaMs else deltaMs
                            val dur = controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            val target = (controller.currentPosition + delta).coerceIn(0L, dur)
                            controller.seekTo(target)
                            positionMs = target
                            controlsVisible = true
                            controlsHideToken++
                        },
                        onVolumeFraction = { level ->
                            setCombinedVolumeLevel(context, level) { pct ->
                                volumeBoostPercent = pct
                            }
                        },
                        onBrightnessFraction = { frac ->
                            surfaceBrightness = frac.coerceIn(BrightnessStore.MIN, BrightnessStore.MAX)
                            applySurfaceBrightness(surfaceBrightness, playerViewRef)
                            val uri = current?.uri?.toString()
                            if (uri != null) {
                                scope.launch { brightnessStore.save(uri, surfaceBrightness) }
                            }
                        },
                        onTransformZoomPan = if (isVideoSurface) { { zoomChange, pan ->
                            runCatching {
                                val next = (videoZoom * zoomChange).coerceIn(1f, 6f)
                                videoZoom = next
                                if (next <= 1.01f) {
                                    videoPan = Offset.Zero
                                    videoZoom = 1f
                                } else {
                                    // Clamp pan roughly to zoomed overflow.
                                    val maxPan = 1200f * (next - 1f)
                                    videoPan = Offset(
                                        (videoPan.x + pan.x).coerceIn(-maxPan, maxPan),
                                        (videoPan.y + pan.y).coerceIn(-maxPan, maxPan),
                                    )
                                }
                            }
                        } } else null,
                        onZoomReset = {
                            videoZoom = 1f
                            videoPan = Offset.Zero
                        },
                        currentZoom = { videoZoom },
                        onHoldSpeedStart = {
                            if (holdBoosting) return@PlayerGestureLayer
                            savedSpeed = speed
                            holdBoosting = true
                            val boost = appSettings.holdToSpeed.speed
                            speed = boost
                            controller.setPlaybackSpeed(boost)
                        },
                        onHoldSpeedEnd = {
                            if (!holdBoosting) return@PlayerGestureLayer
                            holdBoosting = false
                            speed = savedSpeed
                            controller.setPlaybackSpeed(savedSpeed)
                        },
                        onTap = {
                            if (controlsLocked) {
                                controlsVisible = !controlsVisible
                                if (controlsVisible) controlsHideToken++
                                return@PlayerGestureLayer
                            }
                            controlsVisible = !controlsVisible
                            if (!controlsVisible) {
                                panel = Panel.None
                                moreMenu = false
                            } else {
                                controlsHideToken++
                            }
                        },
                        currentVolume = { combinedVolumeLevel(context) },
                        currentBrightness = { surfaceBrightness },
                        gesturesEnabled = !controlsLocked,
                        controlsVisible = showChrome,
                        excludeTopPx = excludeTopPx,
                        excludeBottomPx = excludeBottomPx,
                        sensitivityMultiplier = appSettings.gestureSensitivity.multiplier,
                        swipeGesturesEnabled = appSettings.playerGesturesEnabled,
                        invertGestureSides = appSettings.invertGestureSides,
                        holdSpeedLabel = appSettings.holdToSpeed.label,
                        swipeDownToClose = appSettings.swipeDownToClose,
                        onSwipeDownClose = {
                            runCatching { leavePlayer() }
                        },
                        doubleTapToLock = appSettings.doubleTapToLock,
                        onDoubleTapLock = {
                            controlsLocked = true
                            controlsVisible = true
                            controlsHideToken++
                        },
                    )
                }

                // Custom subtitle overlay (supports delay + style)
                if (subtitlesEnabled && displayedCues.isNotEmpty() && subtitleDelayMs != 0) {
                    val bottomPad = (subtitlePosition.bottomFraction * 400f).dp + if (showChrome) 72.dp else 0.dp
                    val bg = when (subtitleBackground) {
                        SubtitleBackground.NONE -> Color.Transparent
                        SubtitleBackground.SEMI -> Color.Black.copy(alpha = 0.55f)
                        SubtitleBackground.BLACK -> Color.Black
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomPad)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        displayedCues.forEach { cue ->
                            val cueText = cue.text?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                            val shadow = when (subtitleOutline) {
                                SubtitleOutline.OUTLINE -> androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = 2f,
                                )
                                SubtitleOutline.STRONG -> androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                    blurRadius = 4f,
                                )
                                SubtitleOutline.SHADOW -> androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                                    blurRadius = 6f,
                                )
                            }
                            Text(
                                text = cueText,
                                color = Color(subtitleColor.argb),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = subtitleSizeSp.sp,
                                    shadow = shadow,
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Unlock affordance follows the same chrome visibility/timer as other controls
                if (showLockChrome) {
                    IconButton(
                        onClick = {
                            controlsLocked = false
                            controlsVisible = true
                            controlsHideToken++
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 12.dp, bottom = 12.dp)
                            .zIndex(8f)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, ForgeAccent.copy(alpha = 0.55f), CircleShape),
                    ) {
                        Icon(
                            Icons.Rounded.LockOpen,
                            contentDescription = stringResource(R.string.unlock_controls),
                            tint = ForgeAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        if (errorRetrying && playerError == null) {
            Text(
                text = "Retrying playback…",
                color = ForgeMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(6f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        val fatalError = playerError
        if (fatalError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(7f)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Can't play this media",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = fatalError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForgeMuted,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            val p = controller
                            if (p == null) {
                                onBack()
                                return@TextButton
                            }
                            playerError = null
                            errorRetrying = true
                            val pos = runCatching { p.currentPosition }.getOrDefault(positionMs)
                            scope.launch {
                                delay(100)
                                runCatching {
                                    p.prepare()
                                    if (pos > 0L) p.seekTo(pos)
                                    p.play()
                                }.onFailure { t ->
                                    playerError = t.message ?: fatalError
                                }
                                errorRetrying = false
                            }
                        },
                    ) {
                        Text("Retry", color = ForgeAccent)
                    }
                    TextButton(onClick = onBack) {
                        Text("Go back", color = ForgeMuted)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showChrome, modifier = Modifier.align(Alignment.TopCenter).zIndex(4f)) {
            PlayerTopBar(
                title = current?.title ?: "Player",
                showPip = isVideoSurface,
                speed = speed,
                sleepLabel = sleepRemainingSec.takeIf { it > 0 }?.let { formatSleep(it) },
                abLabel = when {
                    abLoopEnabled && abPointA != null && abPointB != null -> "A-B"
                    abPointA != null -> "A set"
                    else -> null
                },
                boostLabel = volumeBoostPercent.takeIf { it > 100 }?.let { "Boost $it%" },
                moreExpanded = moreMenu,
                onInteract = { controlsHideToken++ },
                onBack = {
                    controlsHideToken++
                    val player = controller
                    val uri = current?.uri?.toString()
                    if (player != null && uri != null) {
                        scope.launch { resumeStore.savePosition(uri, player.currentPosition, player.duration) }
                    }
                    onBack()
                },
                onPip = {
                    controlsHideToken++
                    activity?.enterPip()
                },
                onSpeed = {
                    moreMenu = false
                    controlsHideToken++
                    panel = if (panel == Panel.Speed) Panel.None else Panel.Speed
                },
                onLock = {
                    controlsLocked = true
                    controlsVisible = false
                    panel = Panel.None
                    moreMenu = false
                },
                onMore = {
                    moreMenu = true
                    controlsHideToken++
                },
                onDismissMore = { moreMenu = false },
                onSubtitles = {
                    moreMenu = false
                    panel = Panel.Subtitle
                },
                onAudio = {
                    moreMenu = false
                    panel = Panel.Audio
                },
                onQuality = {
                    moreMenu = false
                    panel = Panel.Quality
                },
                onSleep = {
                    moreMenu = false
                    panel = Panel.Sleep
                },
                onEqualizer = {
                    moreMenu = false
                    panel = Panel.Equalizer
                },
                onOrientation = {
                    moreMenu = false
                    panel = Panel.Orientation
                },
                onAbLoop = {
                    moreMenu = false
                    panel = Panel.AbLoop
                },
                onMediaInfo = {
                    moreMenu = false
                    panel = Panel.MediaInfo
                },
                onBookmarks = {
                    moreMenu = false
                    panel = Panel.Bookmarks
                },
                onVolumeBoost = {
                    moreMenu = false
                    panel = Panel.VolumeBoost
                },
                onSubDelay = {
                    moreMenu = false
                    panel = Panel.QuickSubDelay
                },
                onAudioDelay = {
                    moreMenu = false
                    panel = Panel.QuickAudioDelay
                },
                onTransform = {
                    moreMenu = false
                    panel = Panel.Transform
                },
                onNightFilter = {
                    moreMenu = false
                    panel = Panel.NightFilter
                },
                nightStrength = nightStrength,
                onToggleStats = {
                    moreMenu = false
                    statsVisible = !statsVisible
                },

                onSnapshot = {
                    moreMenu = false
                    scope.launch {
                        runCatching {
                            val result = FrameCapture.captureToGallery(
                                context,
                                playerViewRef,
                                current?.title ?: "frame",
                            )
                            snapshotMessage = result.message
                            if (result.ok && result.uri != null) {
                                shareSnapshotUri(context, result.uri)
                            }
                        }.onFailure { e ->
                            snapshotMessage = e.message ?: "Snapshot failed"
                        }
                    }
                },
                onShare = {
                    moreMenu = false
                    shareCurrentMedia(context, current)
                },
                onQueue = {
                    moreMenu = false
                    panel = Panel.Queue
                },
                onChapters = {
                    moreMenu = false
                    panel = Panel.Chapters
                },
                onJumpToTime = {
                    moreMenu = false
                    panel = Panel.JumpToTime
                },
                onVideoColor = {
                    moreMenu = false
                    panel = Panel.VideoColor
                },
                onAudioBalance = {
                    moreMenu = false
                    panel = Panel.AudioBalance
                },
                onLyrics = {
                    moreMenu = false
                    panel = Panel.Lyrics
                },
                onPlayAsAudio = {
                    moreMenu = false
                    val next = !playAsAudio
                    playAsAudio = next
                    controller?.let { p ->
                        p.trackSelectionParameters = p.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, next)
                            .build()
                    }
                    val window = activity?.window
                    if (next) {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                },
                playAsAudio = playAsAudio,
                showPlayAsAudio = current?.kind == MediaKind.VIDEO || hasVideo,
                showSnapshot = isVideoSurface,
                showChapters = chapters.isNotEmpty(),
                statsVisible = statsVisible,
            )
        }

        if (panel == Panel.Lyrics) {
            LyricsDialog(
                lyrics = lyricsResult,
                loading = lyricsLoading,
                onDismiss = { panel = Panel.None },
            )
        }

        if (panel == Panel.JumpToTime) {
            JumpToTimeDialog(
                durationMs = durationMs,
                positionMs = positionMs,
                onDismiss = { panel = Panel.None },
                onSeek = { ms ->
                    controller?.seekTo(ms)
                    positionMs = ms
                },
            )
        }

        if (showChrome && panel == Panel.Speed) {
            SpeedFinePanel(
                selected = speed,
                onChange = { next ->
                    speed = next.coerceIn(0.25f, 3f)
                    savedSpeed = speed
                    controller?.setPlaybackSpeed(speed)
                    current?.uri?.toString()?.let { u ->
                        scope.launch { runCatching { mediaPlaybackPrefsStore.saveSpeed(u, speed) } }
                    }
                    controlsHideToken++
                },
                onDone = { panel = Panel.None },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            )
        }

        if (showChrome && panel == Panel.Transform) {
            TransformPanel(
                mirrorH = mirrorH,
                mirrorV = mirrorV,
                rotationDeg = rotationDeg,
                zoomLabel = if (videoZoom > 1.01f) String.format(java.util.Locale.US, "%.1f×", videoZoom) else null,
                onMirrorH = {
                    mirrorH = !mirrorH
                    ForgeVideoTransform.set(mirrorH, mirrorV, rotationDeg)
                    controlsHideToken++
                },
                onMirrorV = {
                    mirrorV = !mirrorV
                    ForgeVideoTransform.set(mirrorH, mirrorV, rotationDeg)
                    controlsHideToken++
                },
                onRotate = { delta ->
                    rotationDeg = ((rotationDeg + delta) % 360 + 360) % 360
                    ForgeVideoTransform.set(mirrorH, mirrorV, rotationDeg)
                    controlsHideToken++
                },
                onResetZoom = {
                    videoZoom = 1f
                    videoPan = Offset.Zero
                    controlsHideToken++
                },
                onReset = {
                    mirrorH = false
                    mirrorV = false
                    rotationDeg = 0
                    videoZoom = 1f
                    videoPan = Offset.Zero
                    ForgeVideoTransform.reset()
                    controlsHideToken++
                },
                onDone = { panel = Panel.None },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            )
        }

        if (showChrome && panel == Panel.NightFilter) {
            ChipRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            ) {
                listOf(0f to "Off", 0.35f to "Low", 0.65f to "Med", 1f to "High").forEach { (v, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(nightStrength - v) < 0.05f,
                        onClick = {
                            nightStrength = v
                            runCatching {
                                ForgeVideoColor.setNightStrength(v)
                                ForgeVideoColor.applyTo(playerViewRef)
                            }
                            panel = Panel.None
                            controlsHideToken++
                        },
                        label = { Text(label) },
                        colors = chipColors(),
                    )
                }
            }
        }

        if (showChrome && panel == Panel.QuickSubDelay) {
            QuickDelayBar(
                title = "Sub",
                delayMs = subtitleDelayMs,
                onAdjust = { delta ->
                    subtitleDelayMs = (subtitleDelayMs + delta).coerceIn(-5000, 5000)
                    controlsHideToken++
                },
                onOpenFull = { panel = Panel.SubDelay },
                onDone = { panel = Panel.None },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            )
        }

        if (showChrome && panel == Panel.QuickAudioDelay) {
            QuickDelayBar(
                title = "Audio",
                delayMs = audioDelayMs,
                onAdjust = { delta ->
                    val ms = (audioDelayMs + delta).coerceIn(ForgeEngine.MIN_DELAY_MS, ForgeEngine.MAX_DELAY_MS)
                    audioDelayMs = ms
                    ForgeEngine.setAudioDelayMs(ms)
                    scope.launch { enginePrefsStore.setAudioDelayMs(ms) }
                    controlsHideToken++
                },
                onOpenFull = { panel = Panel.AudioDelay },
                onDone = { panel = Panel.None },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            )
        }

        if (showChrome && panel == Panel.Aspect) {
            ChipRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            ) {
                AspectMode.entries.forEach { mode ->
                    FilterChip(
                        selected = aspect == mode,
                        onClick = {
                            aspect = mode
                            current?.uri?.toString()?.let { u ->
                                scope.launch { runCatching { mediaPlaybackPrefsStore.saveAspect(u, mode.name) } }
                            }
                            panel = Panel.None
                        },
                        label = { Text(mode.label) },
                        colors = chipColors(),
                    )
                }
            }
        }

        if (showChrome && panel == Panel.Sleep) {
            ChipRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            ) {
                FilterChip(
                    selected = sleepMinutes == null,
                    onClick = {
                        sleepMinutes = null
                        sleepDeadlineMs = 0L
                        controller?.volume = sleepBaseVolume
                        panel = Panel.None
                    },
                    label = { Text("Off") },
                    colors = chipColors(),
                )
                SLEEP_OPTIONS.forEach { mins ->
                    FilterChip(
                        selected = sleepMinutes == mins,
                        onClick = {
                            sleepMinutes = mins
                            sleepDeadlineMs = System.currentTimeMillis() + mins * 60_000L
                            sleepBaseVolume = controller?.volume?.takeIf { it > 0f } ?: 1f
                            panel = Panel.None
                        },
                        label = { Text("${mins}m") },
                        colors = chipColors(),
                    )
                }
                FilterChip(
                    selected = appSettings.sleepFadeEnabled,
                    onClick = {
                        scope.launch { appSettingsStore.setSleepFadeEnabled(!appSettings.sleepFadeEnabled) }
                    },
                    label = { Text(if (appSettings.sleepFadeEnabled) "Fade ${appSettings.sleepFadeSeconds}s" else "Fade off") },
                    colors = chipColors(),
                )
                FilterChip(
                    selected = appSettings.sleepEndAction == SleepEndAction.STOP,
                    onClick = {
                        val next = if (appSettings.sleepEndAction == SleepEndAction.STOP) SleepEndAction.PAUSE else SleepEndAction.STOP
                        scope.launch { appSettingsStore.setSleepEndAction(next) }
                    },
                    label = { Text("End · ${appSettings.sleepEndAction.label}") },
                    colors = chipColors(),
                )
            }
        }

        AnimatedVisibility(visible = showChrome, modifier = Modifier.align(Alignment.BottomCenter).zIndex(4f)) {
            PlayerControls(
                progress = progress,
                scrubbing = scrubbing,
                scrubValue = scrubValue,
                isPlaying = isPlaying,
                canPrev = index > 0 || shuffleOn || repeatMode != Player.REPEAT_MODE_OFF,
                canNext = index < playQueue.lastIndex || shuffleOn || repeatMode != Player.REPEAT_MODE_OFF,
                repeatMode = repeatMode,
                shuffleOn = shuffleOn,
                showFrameStep = !isPlaying && isVideoSurface && frameStepAvailable,
                showAspect = isVideoSurface,
                aspectLabel = aspect.label,
                showOrientToggle = isVideoSurface,
                orientLabel = when (orientationLock) {
                    OrientationLock.PORTRAIT -> "Port"
                    OrientationLock.LANDSCAPE -> "Land"
                    OrientationLock.AUTO -> "Auto"
                },
                showChapters = chapters.isNotEmpty(),
                showSkipIntro = appSettings.skipIntroSeconds.seconds > 0,
                skipIntroSeconds = appSettings.skipIntroSeconds.seconds,
                onSkipIntro = {
                    val player = controller ?: return@PlayerControls
                    val skipMs = appSettings.skipIntroSeconds.seconds * 1000L
                    if (skipMs <= 0L) return@PlayerControls
                    runCatching {
                        val dur = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        val pos = player.currentPosition
                        val target = if (pos < skipMs) {
                            skipMs.coerceAtMost(dur)
                        } else {
                            (pos + skipMs).coerceAtMost(dur)
                        }
                        player.seekTo(target)
                        positionMs = target
                        progress.updateProgress(
                            target,
                            progress.durationMs,
                            progress.bufferedMs,
                            progress.buffering,
                            progress.playbackState,
                        )
                    }
                    controlsHideToken++
                },
                onInteract = { controlsHideToken++ },
                showRemainingTime = appSettings.showRemainingTime,
                onToggleRemainingTime = {
                    val next = !appSettings.showRemainingTime
                    scope.launch { appSettingsStore.setShowRemainingTime(next) }
                    controlsHideToken++
                },
                onCycleAspect = {
                    val modes = AspectMode.entries
                    aspect = modes[(aspect.ordinal + 1) % modes.size]
                    current?.uri?.toString()?.let { u ->
                        scope.launch { runCatching { mediaPlaybackPrefsStore.saveAspect(u, aspect.name) } }
                    }
                    Toast.makeText(context, aspect.label, Toast.LENGTH_SHORT).show()
                    controlsHideToken++
                },
                onLongAspect = {
                    panel = if (panel == Panel.Aspect) Panel.None else Panel.Aspect
                    controlsHideToken++
                },
                onToggleOrient = {
                    orientationLock = if (orientationLock == OrientationLock.LANDSCAPE) {
                        OrientationLock.PORTRAIT
                    } else {
                        OrientationLock.LANDSCAPE
                    }
                    Toast.makeText(context, orientationLock.label, Toast.LENGTH_SHORT).show()
                    controlsHideToken++
                },
                onQueue = {
                    panel = Panel.Queue
                    controlsHideToken++
                },
                onChapterPrev = {
                    val pos = progress.positionMs
                    val prev = chapters.lastOrNull { it.startMs < pos - 500L }
                    val target = prev?.startMs ?: 0L
                    controller?.seekTo(target)
                    positionMs = target
                    progress.updateProgress(target, progress.durationMs, progress.bufferedMs, progress.buffering, progress.playbackState)
                    controlsHideToken++
                },
                onChapterNext = {
                    val pos = progress.positionMs
                    val nextCh = chapters.firstOrNull { it.startMs > pos + 500L }
                    if (nextCh != null) {
                        controller?.seekTo(nextCh.startMs)
                        positionMs = nextCh.startMs
                        progress.updateProgress(nextCh.startMs, progress.durationMs, progress.bufferedMs, progress.buffering, progress.playbackState)
                    }
                    controlsHideToken++
                },
                onFrameStep = { forward ->
                    val player = controller ?: return@PlayerControls
                    val step = estimateFrameStepMs(player)
                    if (step <= 0L) {
                        frameStepAvailable = false
                        return@PlayerControls
                    }
                    val dur = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                    val target = (player.currentPosition + if (forward) step else -step).coerceIn(0L, dur)
                    player.seekTo(target)
                    positionMs = target
                    progress.updateProgress(target, dur.takeIf { it != Long.MAX_VALUE } ?: progress.durationMs, progress.bufferedMs, false, progress.playbackState)
                },
                onScrub = {
                    scrubbing = true
                    scrubFromSlider = true
                    scrubValue = it
                    controlsHideToken++
                    val dur = progress.durationMs
                    if (dur > 0L) {
                        startVideoScrub(controller, scrubHold)
                        previewSeekTo(controller, (it * dur).toLong(), scrubHold)
                    }
                },
                onScrubEnd = {
                    val dur = progress.durationMs
                    val seekTo = (scrubValue * dur).toLong()
                    finishVideoScrub(controller, seekTo, scrubHold)
                    positionMs = seekTo
                    progress.updateProgress(seekTo, dur, progress.bufferedMs, progress.buffering, progress.playbackState)
                    scrubbing = false
                    scrubFromSlider = false
                },
                onPrev = {
                    controller?.seekToPreviousMediaItem()
                },
                onPlayPause = {
                    val player = controller ?: return@PlayerControls
                    if (player.isPlaying) player.pause() else player.play()
                },
                onNext = {
                    controller?.seekToNextMediaItem()
                },
                onCycleRepeat = {
                    val next = when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                    repeatMode = next
                    controller?.repeatMode = next
                },
                onToggleShuffle = {
                    shuffleOn = !shuffleOn
                    controller?.shuffleModeEnabled = shuffleOn
                },
            )
        }

        if (showChrome && panel == Panel.Orientation) {
            ChipRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            ) {
                OrientationLock.entries.forEach { mode ->
                    FilterChip(
                        selected = orientationLock == mode,
                        onClick = {
                            orientationLock = mode
                            panel = Panel.None
                        },
                        label = { Text(mode.label) },
                        colors = chipColors(),
                    )
                }
            }
        }

        if (panel == Panel.AbLoop && showChrome) {
            AbLoopDialog(
                pointA = abPointA,
                pointB = abPointB,
                enabled = abLoopEnabled,
                positionMs = positionMs,
                onDismiss = { panel = Panel.None },
                onSetA = {
                    abPointA = positionMs
                    val b = abPointB
                    if (b != null && b <= positionMs) abPointB = null
                    abLoopEnabled = abPointA != null && abPointB != null
                },
                onSetB = {
                    val a = abPointA
                    if (a != null && positionMs > a) {
                        abPointB = positionMs
                        abLoopEnabled = true
                    } else if (a == null) {
                        abPointA = 0L
                        abPointB = positionMs
                        abLoopEnabled = true
                    }
                },
                onToggle = { on ->
                    abLoopEnabled = on && abPointA != null && abPointB != null
                },
                onClear = {
                    abPointA = null
                    abPointB = null
                    abLoopEnabled = false
                },
                onSeekA = {
                    abPointA?.let {
                        controller?.seekTo(it)
                        positionMs = it
                    }
                },
            )
        }

        if (panel == Panel.Equalizer && showChrome) {
            EqualizerDialog(
                enabled = eqEnabled,
                bands = eqBands,
                presetName = eqPreset,
                bassOn = bassOn,
                virtOn = virtOn,
                onDismiss = { panel = Panel.None },
                onToggle = { on ->
                    ForgeEqualizer.setEnabled(on)
                    eqEnabled = on
                },
                onBand = { index, value ->
                    ForgeEqualizer.setBandNormalized(index, value)
                    eqBands = ForgeEqualizer.bands()
                    eqPreset = ForgeEqualizer.presetName
                    eqEnabled = true
                },
                onPreset = { preset ->
                    ForgeEqualizer.applyPreset(preset)
                    eqEnabled = true
                    eqBands = ForgeEqualizer.bands()
                    eqPreset = ForgeEqualizer.presetName
                },
                onBass = { on ->
                    ForgeAudioFx.setBassEnabled(on)
                    bassOn = on
                },
                onVirt = { on ->
                    ForgeAudioFx.setVirtualizerEnabled(on)
                    virtOn = on
                },
            )
        }

        if (panel == Panel.VideoColor && showChrome) {
            VideoColorDialog(
                brightness = videoBrightness,
                contrast = videoContrast,
                saturation = videoSaturation,
                onDismiss = { panel = Panel.None },
                onBrightness = {
                    videoBrightness = it
                    ForgeVideoColor.set(videoBrightness, videoContrast, videoSaturation)
                    ForgeVideoColor.applyTo(playerViewRef)
                },
                onContrast = {
                    videoContrast = it
                    ForgeVideoColor.set(videoBrightness, videoContrast, videoSaturation)
                    ForgeVideoColor.applyTo(playerViewRef)
                },
                onSaturation = {
                    videoSaturation = it
                    ForgeVideoColor.set(videoBrightness, videoContrast, videoSaturation)
                    ForgeVideoColor.applyTo(playerViewRef)
                },
                onReset = {
                    videoBrightness = 0f
                    videoContrast = 1f
                    videoSaturation = 1f
                    ForgeVideoColor.set(0f, 1f, 1f)
                    applySurfaceBrightness(surfaceBrightness, playerViewRef)
                    runCatching {
                        ForgeVideoColor.setNightStrength(nightStrength)
                        ForgeVideoColor.applyTo(playerViewRef)
                    }
                },
            )
        }

        if (panel == Panel.AudioBalance && showChrome) {
            AudioBalanceDialog(
                balance = audioBalance,
                onDismiss = { panel = Panel.None },
                onChange = {
                    audioBalance = it
                    ForgeBalance.setBalance(it)
                },
            )
        }


        if (panel == Panel.Chapters && showChrome) {
            ChaptersDialog(
                chapters = chapters,
                onDismiss = { panel = Panel.None },
                onJump = { ms ->
                    controller?.seekTo(ms)
                    positionMs = ms
                    panel = Panel.None
                },
            )
        }

        if (panel == Panel.Subtitle && showChrome) {
            SubtitleDialog(
                tracks = textTracks,
                enabled = subtitlesEnabled,
                sizeSp = subtitleSizeSp,
                color = subtitleColor,
                background = subtitleBackground,
                position = subtitlePosition,
                outline = subtitleOutline,
                hasExternal = externalSubtitleUri != null,
                delayMs = subtitleDelayMs,
                onDismiss = { panel = Panel.None },
                onToggle = { on ->
                    subtitlesEnabled = on
                    enableTextTracks(controller, on)
                    current?.uri?.toString()?.let { u ->
                        scope.launch { runCatching { trackPrefsStore.saveTextDisabled(u, !on) } }
                    }
                },
                onSelectTrack = { choice ->
                    subtitlesEnabled = true
                    selectTrack(controller, C.TRACK_TYPE_TEXT, choice)
                    current?.uri?.toString()?.let { u ->
                        scope.launch { runCatching { trackPrefsStore.saveText(u, choice.label) } }
                    }
                },
                onSize = {
                    subtitleSizeSp = it
                    scope.launch { appSettingsStore.setSubtitleSizeSp(it) }
                },
                onColor = {
                    subtitleColor = it
                    scope.launch { appSettingsStore.setSubtitleColor(it) }
                },
                onBackground = {
                    subtitleBackground = it
                    scope.launch { appSettingsStore.setSubtitleBackground(it) }
                },
                onPosition = {
                    subtitlePosition = it
                    scope.launch { appSettingsStore.setSubtitlePosition(it) }
                },
                onOutline = {
                    subtitleOutline = it
                    scope.launch { appSettingsStore.setSubtitleOutline(it) }
                },
                onDelay = { panel = Panel.SubDelay },
                onPickExternal = {
                    subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream", "*/*"))
                },
                onClearExternal = {
                    externalSubtitleUri = null
                    autoSidecarForUri = null
                    // Reload current item; re-apply auto sidecar if present
                    val player = controller ?: return@SubtitleDialog
                    val item = playQueue.getOrNull(player.currentMediaItemIndex) ?: return@SubtitleDialog
                    val pos = player.currentPosition
                    val ready = player.playWhenReady
                    val sidecar = runCatching { SidecarSubtitles.find(context, item) }.getOrNull()
                    if (sidecar != null) {
                        externalSubtitleUri = sidecar
                        autoSidecarForUri = item.uri.toString()
                    }
                    val media = buildPlayerMediaItem(item, sidecar)
                    player.replaceMediaItem(player.currentMediaItemIndex, media)
                    player.seekTo(player.currentMediaItemIndex, pos)
                    player.prepare()
                    player.playWhenReady = ready
                },
            )
        }

        if (panel == Panel.Audio && showChrome) {
            AudioDialog(
                tracks = audioTracks,
                onDismiss = { panel = Panel.None },
                onSelect = { choice ->
                    selectTrack(controller, C.TRACK_TYPE_AUDIO, choice)
                    current?.uri?.toString()?.let { u ->
                        scope.launch { runCatching { trackPrefsStore.saveAudio(u, choice.label) } }
                    }
                    panel = Panel.None
                },
            )
        }

        if (panel == Panel.MediaInfo && showChrome) {
            val item = current
            val res = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}×${videoHeight}" else "—"
            val dur = durationMs.takeIf { it > 0 } ?: item?.durationMs ?: 0L
            MediaInfoDialog(
                info = MediaInfoSnapshot(
                    title = item?.title ?: "—",
                    resolution = res,
                    durationLabel = formatDuration(dur),
                    sizeLabel = formatBytes(item?.sizeBytes ?: 0L),
                    mime = item?.mimeType?.takeIf { it.isNotBlank() } ?: "—",
                    container = guessContainer(item?.mimeType.orEmpty(), item?.title.orEmpty()),
                    videoTracks = videoTrackLabels,
                    audioTracks = audioTracks.map { it.label },
                    textTracks = textTracks.map { it.label },
                ),
                onDismiss = { panel = Panel.None },
            )
        }

        if (panel == Panel.Bookmarks && showChrome) {
            val uri = current?.uri?.toString().orEmpty()
            val mediaBookmarks = bookmarkStore.forMedia(allBookmarks, uri)
            BookmarksDialog(
                bookmarks = mediaBookmarks,
                positionMs = positionMs,
                onDismiss = { panel = Panel.None },
                onAdd = { name ->
                    if (uri.isNotEmpty()) {
                        scope.launch { bookmarkStore.add(uri, positionMs, name) }
                    }
                },
                onRemove = { id -> scope.launch { bookmarkStore.remove(id) } },
                onJump = { pos ->
                    controller?.seekTo(pos)
                    positionMs = pos
                    panel = Panel.None
                },
            )
        }

        if (panel == Panel.VolumeBoost && showChrome) {
            VolumeBoostDialog(
                boostPercent = volumeBoostPercent,
                onDismiss = { panel = Panel.None },
                onChange = { p ->
                    volumeBoostPercent = p
                    ForgeLoudness.setBoostPercent(p)
                },
            )
        }

        if (panel == Panel.SubDelay && showChrome) {
            DelayDialog(
                title = "Subtitle delay",
                delayMs = subtitleDelayMs,
                supported = true,
                note = "Positive delays subtitle display. Negative shows immediately (full early-shift needs renderer access).",
                onDismiss = { panel = Panel.None },
                onChange = { subtitleDelayMs = it },
            )
        }

        if (panel == Panel.AudioDelay && showChrome) {
            DelayDialog(
                title = "Audio delay",
                delayMs = audioDelayMs,
                supported = true,
                note = "Shifts video presentation timestamps so audio leads/lags (Media3 video PTS adjustment). Positive = delay audio vs video.",
                onDismiss = { panel = Panel.None },
                onChange = { ms ->
                    audioDelayMs = ms
                    ForgeEngine.setAudioDelayMs(ms)
                    scope.launch { enginePrefsStore.setAudioDelayMs(ms) }
                },
            )
        }

        if (panel == Panel.Quality && showChrome) {
            QualityDialog(
                tracks = videoTracks,
                onDismiss = { panel = Panel.None },
                onAuto = {
                    val p = controller ?: return@QualityDialog
                    p.trackSelectionParameters = p.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                        .build()
                    panel = Panel.None
                },
                onSelect = { choice ->
                    selectTrack(controller, C.TRACK_TYPE_VIDEO, choice)
                    panel = Panel.None
                },
            )
        }

        if (panel == Panel.Queue) {
            QueueSheet(
                items = playQueue,
                currentIndex = index,
                onDismiss = { panel = Panel.None },
                onPlayIndex = { i ->
                    controller?.seekToDefaultPosition(i)
                    index = i
                    panel = Panel.None
                },
                onMove = { from, to ->
                    if (from !in playQueue.indices || to !in playQueue.indices) return@QueueSheet
                    val mutable = playQueue.toMutableList()
                    val item = mutable.removeAt(from)
                    mutable.add(to, item)
                    playQueue = mutable
                    controller?.moveMediaItem(from, to)
                    index = controller?.currentMediaItemIndex ?: index
                },
            )
        }

        resumePromptMs?.let { saved ->
            AlertDialog(
                onDismissRequest = { },
                containerColor = ForgeGraphite,
                title = { Text(stringResource(R.string.resume_playback_title), color = Color.White) },
                text = {
                    Column {
                        Text(
                            "Continue from ${formatDuration(saved)}?",
                            color = ForgeMuted,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rememberResumeChoice = !rememberResumeChoice }
                                .padding(vertical = 4.dp),
                        ) {
                            Checkbox(
                                checked = rememberResumeChoice,
                                onCheckedChange = { rememberResumeChoice = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ForgeAccent,
                                    uncheckedColor = ForgeMuted,
                                    checkmarkColor = Color.Black,
                                ),
                            )
                            Text(
                                stringResource(R.string.resume_remember),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val player = controller
                            if (rememberResumeChoice) {
                                scope.launch {
                                    appSettingsStore.setResumeBehavior(ResumeBehavior.ALWAYS_CONTINUE)
                                }
                            }
                            if (player != null) {
                                player.seekTo(saved)
                                player.play()
                            }
                            resumePromptMs = null
                            pendingResumeUri = null
                            rememberResumeChoice = false
                        },
                    ) { Text("Continue", color = ForgeAccent) }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val uri = pendingResumeUri
                            val player = controller
                            if (rememberResumeChoice) {
                                scope.launch {
                                    appSettingsStore.setResumeBehavior(ResumeBehavior.ALWAYS_START_OVER)
                                }
                            }
                            if (uri != null) {
                                scope.launch { resumeStore.clear(uri) }
                            }
                            player?.seekTo(0L)
                            player?.play()
                            resumePromptMs = null
                            pendingResumeUri = null
                            rememberResumeChoice = false
                        },
                    ) { Text("Start over", color = Color.White) }
                },
            )
        }

        seriesPrompt?.let { nextEp ->
            // End card overlay (crash-isolated) — Play next / Stop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { /* consume */ },
                    )
                    .zIndex(8f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ForgeGraphite)
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                        .fillMaxWidth(0.88f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.end_card_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = nextEp.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForgeMuted,
                        maxLines = 3,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = {
                                seriesPrompt = null
                                runCatching { controller?.pause() }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.end_card_stop), color = Color.White)
                        }
                        Button(
                            onClick = {
                                val next = seriesPrompt
                                seriesPrompt = null
                                val player = controller
                                if (next != null && player != null) {
                                    runCatching {
                                        val mutable = playQueue.toMutableList()
                                        mutable.add(next)
                                        playQueue = mutable
                                        val sidecar = runCatching { SidecarSubtitles.find(context, next) }.getOrNull()
                                        val media = buildPlayerMediaItem(next, sidecar)
                                        if (sidecar != null) {
                                            externalSubtitleUri = sidecar
                                            autoSidecarForUri = next.uri.toString()
                                        }
                                        tracksRestoredUri = null
                                        player.addMediaItem(media)
                                        player.seekToNextMediaItem()
                                        player.play()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeAccent, contentColor = Color.Black),
                        ) {
                            Text(stringResource(R.string.play_next))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
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
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
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
private fun SpeedRow(
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
private fun ChipRow(
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
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = Color.Black,
    containerColor = ForgeGraphite,
    labelColor = Color.White,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PlayerControls(
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
                trackHeight = if (landscape) 4.dp else 5.dp,
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
                style = MaterialTheme.typography.labelSmall,
                color = ForgeMuted,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = ForgeMuted,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (landscape) Arrangement.SpaceBetween else Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleOn) ForgeAccent else Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onQueue) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.queue),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (showChapters && !showFrameStep) {
                IconButton(onClick = onChapterPrev) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.chapter_prev),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (showAspect) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = onCycleAspect,
                            onLongClick = onLongAspect,
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Rounded.AspectRatio,
                        contentDescription = stringResource(R.string.aspect_cycle, aspectLabel),
                        tint = ForgeAccent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = aspectLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (showOrientToggle) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleOrient)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Rounded.ScreenRotation,
                        contentDescription = stringResource(R.string.orient_toggle, orientLabel),
                        tint = if (orientLabel == "Auto") Color.White else ForgeAccent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = orientLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (showSkipIntro && skipIntroSeconds > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSkipIntro)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = stringResource(R.string.skip_intro),
                        tint = ForgeAccent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "${skipIntroSeconds}s",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (showFrameStep) {
                IconButton(onClick = { onFrameStep(false) }) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous frame",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            IconButton(onClick = onPrev, enabled = canPrev) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (canPrev) Color.White else ForgeMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ForgeAccent),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp),
                )
            }
            IconButton(onClick = onNext, enabled = canNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = if (canNext) Color.White else ForgeMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (showFrameStep) {
                IconButton(onClick = { onFrameStep(true) }) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Next frame",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            if (showChapters && !showFrameStep) {
                IconButton(onClick = onChapterNext) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.chapter_next),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) Color.White else ForgeAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun SubtitleDialog(
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
private fun AudioDialog(
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
private fun AbLoopDialog(
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
private fun EqualizerDialog(
    enabled: Boolean,
    bands: List<com.gketch.forge.playback.EqBand>,
    presetName: String,
    bassOn: Boolean,
    virtOn: Boolean,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onBand: (Int, Float) -> Unit,
    onPreset: (com.gketch.forge.playback.EqPreset) -> Unit,
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
private fun QualityDialog(
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
private fun AudioArtwork(title: String) {
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


private fun collectTrackDetailLabels(tracks: Tracks, type: @C.TrackType Int): List<String> {
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


/** Apply saved-position policy for an already-prepared current item. */
private suspend fun applyResumePolicy(
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

private fun collectTracks(tracks: Tracks, type: @C.TrackType Int): List<TrackChoice> {
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

private fun selectTrack(player: Player?, type: @C.TrackType Int, choice: TrackChoice) {
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

private fun enableTextTracks(player: Player?, enabled: Boolean) {
    val p = player ?: return
    p.trackSelectionParameters = p.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
        .build()
}


private fun subtitleMimeForUri(uri: android.net.Uri): String {
    val path = (uri.lastPathSegment ?: uri.toString()).lowercase()
    return if (path.endsWith(".vtt")) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
}

private fun buildPlayerMediaItem(
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

private fun restoreRememberedTracks(
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

private fun applyExternalSubtitle(player: Player, uri: android.net.Uri) {
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


@Composable
private fun QueueDialog(
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (items.isEmpty()) {
                    Text("Queue is empty", color = ForgeMuted)
                } else {
                    items.forEachIndexed { i, item ->
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

private fun shareCurrentMedia(context: android.content.Context, item: ForgeMediaItem?) {
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

@Composable
private fun ChaptersDialog(
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

private fun parseChapters(metadata: Metadata): List<MediaChapter> {
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



private fun applyForcedAspect(playerView: PlayerView, aspect: AspectMode) {
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

private fun estimateFrameStepMs(player: Player): Long {
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

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}×"
    } else {
        "${speed}×"
    }
}

private fun formatSleep(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private fun musicVolumeFraction(context: Context): Float {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
}

private fun setMusicVolume(context: Context, fraction: Float) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val value = (fraction.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
}

/** 0..1 = system volume; 1..2 = max system + loudness boost (HUD 100%..200%). */
private fun combinedVolumeLevel(context: Context): Float {
    val boost = ForgeLoudness.boostPercent
    return if (boost > 100) {
        1f + ((boost - 100).toFloat() / (ForgeLoudness.MAX_BOOST_PERCENT - 100).toFloat())
            .coerceIn(0f, 1f)
    } else {
        musicVolumeFraction(context).coerceIn(0f, 1f)
    }
}

private fun setCombinedVolumeLevel(context: Context, level: Float, onBoostPercent: (Int) -> Unit) {
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

private fun shareSnapshotUri(context: Context, uri: android.net.Uri) {
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
private fun applySurfaceBrightness(fraction: Float, playerView: PlayerView?) {
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
private suspend fun maybeSeriesAutoNext(
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

/** Undo any leftover 1.18 window dim so chrome stays at system brightness. */
@Composable
private fun ScrubTimecodeHud(elapsedMs: Long, totalMs: Long) {
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

private fun clearWindowBrightness(activity: Activity?) {
    val window = activity?.window ?: return
    val lp = window.attributes
    if (lp.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = lp
    }
}

private class ScrubHold {
    var active: Boolean = false
    var wasPlaying: Boolean = false
    var volume: Float = 1f
    var lastSeekMs: Long = -1L
    var lastSeekAt: Long = 0L
    var pendingMs: Long = -1L
}

/** ~120–150ms throttle + keyframe seeks while dragging; EXACT settle on release. */
private const val SCRUB_THROTTLE_MS = 130L
private const val SCRUB_MIN_DELTA_MS = 350L
private const val SCRUB_FORCE_DELTA_MS = 2_000L

private fun startVideoScrub(player: Player?, hold: ScrubHold) {
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

private fun previewSeekTo(player: Player?, target: Long, hold: ScrubHold) {
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

private fun finishVideoScrub(player: Player?, target: Long, hold: ScrubHold) {
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


@Composable
private fun LyricsDialog(
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
