package com.gketch.forge.ui.player

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.util.TypedValue
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
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
import com.gketch.forge.data.BookmarkStore
import com.gketch.forge.data.MediaBookmark
import com.gketch.forge.playback.ForgeLoudness
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.playback.ForgeEqualizer
import com.gketch.forge.ui.library.formatDuration
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
private val SLEEP_OPTIONS = listOf(15, 30, 45, 60)
private val SUBTITLE_SIZES = listOf(16f, 20f, 24f, 28f, 32f)

private enum class AspectMode(val label: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
}

private enum class Panel { None, Speed, Aspect, Sleep, Subtitle, Audio, Equalizer, Orientation, AbLoop, MediaInfo, Bookmarks, VolumeBoost, SubDelay, AudioDelay }

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
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val scope = rememberCoroutineScope()
    val resumeStore = remember { ResumeStore(context) }
    val recentStore = remember { RecentStore(context) }
    val controller = rememberPlayerController()

    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))) }
    val current = queue.getOrNull(index)

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
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
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var externalSubtitleUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var textTracks by remember { mutableStateOf<List<TrackChoice>>(emptyList()) }
    var audioTracks by remember { mutableStateOf<List<TrackChoice>>(emptyList()) }
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
    var subtitleDelayMs by remember { mutableIntStateOf(0) }
    var audioDelayMs by remember { mutableIntStateOf(0) }
    var volumeBoostPercent by remember { mutableIntStateOf(ForgeLoudness.boostPercent) }
    var snapshotMessage by remember { mutableStateOf<String?>(null) }
    var displayedCues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var videoTrackLabels by remember { mutableStateOf<List<String>>(emptyList()) }

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
            controller?.let { applyExternalSubtitle(it, uri) }
            subtitlesEnabled = true
            enableTextTracks(controller, true)
            panel = Panel.None
        }
    }

    LaunchedEffect(Unit) {
        bookmarkStore.bookmarks.collect { allBookmarks = it }
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

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            inPip = info.isInPictureInPictureMode
            if (info.isInPictureInPictureMode) {
                controlsVisible = false
                panel = Panel.None
            }
        }
        activity?.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.updatePipParams(allowed = false)
            activity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    val currentUriState = rememberUpdatedState(current?.uri?.toString())
    DisposableEffect(controller) {
        val player = controller
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    abPointA = null
                    abPointB = null
                    abLoopEnabled = false
                    displayedCues = emptyList()
                    val newIndex = player.currentMediaItemIndex
                    if (newIndex in queue.indices) {
                        index = newIndex
                        queue.getOrNull(newIndex)?.let { item ->
                            scope.launch { recentStore.record(item) }
                        }
                    }
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                        val uri = mediaItem?.mediaId
                            ?: mediaItem?.localConfiguration?.uri?.toString()
                        if (uri != null) {
                            scope.launch {
                                val saved = resumeStore.getPosition(uri)
                                if (saved >= ResumeStore.MIN_SAVE_MS) {
                                    player.seekTo(saved)
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
                    videoTrackLabels = collectTrackDetailLabels(tracks, C.TRACK_TYPE_VIDEO)
                    val video = tracks.groups.any { group ->
                        group.type == C.TRACK_TYPE_VIDEO && group.isSelected
                    }
                    if (tracks.groups.isNotEmpty()) {
                        hasVideo = video || (current?.kind == MediaKind.VIDEO && player.videoSize.width > 0)
                    }
                }

                override fun onRepeatModeChanged(mode: Int) {
                    repeatMode = mode
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    shuffleOn = shuffleModeEnabled
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

    LaunchedEffect(controller, queue, startIndex) {
        val player = controller ?: return@LaunchedEffect
        val key = queue.joinToString("|") { it.uri.toString() } + "#$startIndex"
        val alreadySame = player.currentMediaItem?.mediaId == queue.getOrNull(startIndex)?.uri?.toString() &&
            player.mediaItemCount == queue.size
        if (alreadySame && loadedKey == key) return@LaunchedEffect
        loadedKey = key
        externalSubtitleUri = null

        val items = queue.map { item ->
            MediaItem.Builder()
                .setUri(item.uri)
                .setMediaId(item.uri.toString())
                .setMimeType(item.mimeType.takeIf { it.isNotBlank() && '*' !in it })
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist("Forge")
                        .setArtworkUri(item.albumArtUri)
                        .setIsPlayable(true)
                        .build(),
                )
                .build()
        }
        val startUri = queue.getOrNull(startIndex)?.uri?.toString()
        val resumeAt = if (startUri != null) resumeStore.getPosition(startUri) else 0L
        player.setMediaItems(items, startIndex.coerceAtLeast(0), resumeAt.coerceAtLeast(0L))
        player.prepare()
        player.setPlaybackSpeed(speed)
        player.repeatMode = repeatMode
        player.shuffleModeEnabled = shuffleOn
        player.play()
        queue.getOrNull(startIndex)?.let { recentStore.record(it) }
    }

    LaunchedEffect(hasVideo, current?.kind, inPip, orientationLock) {
        val video = hasVideo || current?.kind == MediaKind.VIDEO
        if (!inPip) {
            activity?.requestedOrientation = when (orientationLock) {
                OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                OrientationLock.AUTO -> if (video) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
        activity?.updatePipParams(allowed = video && !inPip)
    }

    LaunchedEffect(controller, abLoopEnabled, abPointA, abPointB) {
        val player = controller ?: return@LaunchedEffect
        while (isActive) {
            if (!scrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L).takeIf { it > 0 } ?: 0L
                val a = abPointA
                val b = abPointB
                if (abLoopEnabled && a != null && b != null && b > a && positionMs >= b) {
                    player.seekTo(a)
                    positionMs = a
                }
            }
            delay(100)
        }
    }

    LaunchedEffect(panel) {
        if (panel == Panel.Equalizer) {
            eqEnabled = ForgeEqualizer.enabled
            eqBands = ForgeEqualizer.bands()
            eqPreset = ForgeEqualizer.presetName
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

    LaunchedEffect(controlsVisible, isPlaying, inPip, panel, controlsLocked) {
        if (controlsLocked) {
            controlsVisible = false
            panel = Panel.None
            return@LaunchedEffect
        }
        if (controlsVisible && isPlaying && !inPip && panel == Panel.None) {
            delay(4_000)
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

    LaunchedEffect(sleepDeadlineMs) {
        if (sleepDeadlineMs <= 0L) {
            sleepRemainingSec = 0
            return@LaunchedEffect
        }
        while (isActive && sleepDeadlineMs > 0L) {
            val left = ((sleepDeadlineMs - System.currentTimeMillis()) / 1000L).toInt()
            if (left <= 0) {
                controller?.pause()
                sleepMinutes = null
                sleepDeadlineMs = 0L
                sleepRemainingSec = 0
                break
            }
            sleepRemainingSec = left
            delay(500)
        }
    }

    LaunchedEffect(subtitleSizeSp, playerViewRef, subtitlesEnabled) {
        playerViewRef?.subtitleView?.apply {
            setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSizeSp)
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.WHITE,
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    android.graphics.Color.BLACK,
                    null,
                ),
            )
        }
    }

    BackHandler {
        val player = controller
        val uri = current?.uri?.toString()
        if (player != null && uri != null) {
            scope.launch { resumeStore.savePosition(uri, player.currentPosition, player.duration) }
        }
        onBack()
    }

    val showChrome = controlsVisible && !inPip && !controlsLocked
    val isVideoSurface = hasVideo || current?.kind == MediaKind.VIDEO

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBlack),
    ) {
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
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = aspect.resizeMode
                                this.player = controller
                                playerViewRef = this
                                subtitleView?.visibility =
                                    if (subtitleDelayMs != 0) android.view.View.INVISIBLE
                                    else android.view.View.VISIBLE
                            }
                        },
                        update = {
                            it.player = controller
                            it.resizeMode = aspect.resizeMode
                            playerViewRef = it
                            it.subtitleView?.visibility =
                                if (subtitleDelayMs != 0) android.view.View.INVISIBLE
                                else android.view.View.VISIBLE
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AudioArtwork(title = current?.title.orEmpty())
                }

                if (!inPip) {
                    PlayerGestureLayer(
                        durationMs = durationMs,
                        positionMs = positionMs,
                        onSeek = { target ->
                            controller.seekTo(target)
                            positionMs = target
                        },
                        onDoubleTapSeek = { back ->
                            val delta = if (back) -10_000L else 10_000L
                            val dur = controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            val target = (controller.currentPosition + delta).coerceIn(0L, dur)
                            controller.seekTo(target)
                            positionMs = target
                            controlsVisible = true
                        },
                        onVolumeFraction = { setMusicVolume(context, it) },
                        onBrightnessFraction = { setWindowBrightness(activity, it) },
                        onTap = {
                            if (controlsLocked) return@PlayerGestureLayer
                            controlsVisible = !controlsVisible
                            if (!controlsVisible) panel = Panel.None
                        },
                        currentVolume = { musicVolumeFraction(context) },
                        currentBrightness = { windowBrightness(activity) },
                        gesturesEnabled = !controlsLocked,
                    )
                }

                // Custom subtitle overlay (supports delay)
                if (subtitlesEnabled && displayedCues.isNotEmpty() && subtitleDelayMs != 0) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (showChrome) 120.dp else 48.dp)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        displayedCues.forEach { cue ->
                            val cueText = cue.text?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                            Text(
                                text = cueText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Control lock unlock zone
                if (controlsLocked && !inPip) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 28.dp)
                            .zIndex(8f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(1.dp, ForgeAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable {
                                controlsLocked = false
                                controlsVisible = true
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
                            Text("Tap to unlock", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showChrome, modifier = Modifier.align(Alignment.TopCenter)) {
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
                onBack = {
                    val player = controller
                    val uri = current?.uri?.toString()
                    if (player != null && uri != null) {
                        scope.launch { resumeStore.savePosition(uri, player.currentPosition, player.duration) }
                    }
                    onBack()
                },
                onPip = { activity?.enterPip() },
                onSpeed = { panel = if (panel == Panel.Speed) Panel.None else Panel.Speed },
                onLock = {
                    controlsLocked = true
                    controlsVisible = false
                    panel = Panel.None
                    moreMenu = false
                },
                onMore = { moreMenu = true },
                onDismissMore = { moreMenu = false },
                onSubtitles = {
                    moreMenu = false
                    panel = Panel.Subtitle
                },
                onAudio = {
                    moreMenu = false
                    panel = Panel.Audio
                },
                onAspect = {
                    moreMenu = false
                    panel = Panel.Aspect
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
                    panel = Panel.SubDelay
                },
                onAudioDelay = {
                    moreMenu = false
                    panel = Panel.AudioDelay
                },
                onSnapshot = {
                    moreMenu = false
                    scope.launch {
                        val result = FrameCapture.captureToGallery(
                            context,
                            playerViewRef,
                            current?.title ?: "frame",
                        )
                        snapshotMessage = result.message
                    }
                },
                showAspect = isVideoSurface,
                showSnapshot = isVideoSurface,
            )
        }

        if (showChrome && panel == Panel.Speed) {
            SpeedRow(
                selected = speed,
                onSelect = { next ->
                    speed = next
                    controller?.setPlaybackSpeed(next)
                    panel = Panel.None
                },
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
                            panel = Panel.None
                        },
                        label = { Text("${mins}m") },
                        colors = chipColors(),
                    )
                }
            }
        }

        AnimatedVisibility(visible = showChrome, modifier = Modifier.align(Alignment.BottomCenter)) {
            PlayerControls(
                positionMs = positionMs,
                durationMs = durationMs,
                scrubbing = scrubbing,
                scrubValue = scrubValue,
                isPlaying = isPlaying,
                canPrev = index > 0 || shuffleOn || repeatMode != Player.REPEAT_MODE_OFF,
                canNext = index < queue.lastIndex || shuffleOn || repeatMode != Player.REPEAT_MODE_OFF,
                repeatMode = repeatMode,
                shuffleOn = shuffleOn,
                onScrub = {
                    scrubbing = true
                    scrubValue = it
                },
                onScrubEnd = {
                    val seekTo = (scrubValue * durationMs).toLong()
                    controller?.seekTo(seekTo)
                    positionMs = seekTo
                    scrubbing = false
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
            )
        }

        if (panel == Panel.Subtitle && showChrome) {
            SubtitleDialog(
                tracks = textTracks,
                enabled = subtitlesEnabled,
                sizeSp = subtitleSizeSp,
                hasExternal = externalSubtitleUri != null,
                delayMs = subtitleDelayMs,
                onDismiss = { panel = Panel.None },
                onToggle = { on ->
                    subtitlesEnabled = on
                    enableTextTracks(controller, on)
                },
                onSelectTrack = { choice ->
                    subtitlesEnabled = true
                    selectTrack(controller, C.TRACK_TYPE_TEXT, choice)
                },
                onSize = { subtitleSizeSp = it },
                onDelay = { panel = Panel.SubDelay },
                onPickExternal = {
                    subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream", "*/*"))
                },
                onClearExternal = {
                    externalSubtitleUri = null
                    // Reload current item without subtitles
                    val player = controller ?: return@SubtitleDialog
                    val item = queue.getOrNull(player.currentMediaItemIndex) ?: return@SubtitleDialog
                    val pos = player.currentPosition
                    val ready = player.playWhenReady
                    val media = MediaItem.Builder()
                        .setUri(item.uri)
                        .setMediaId(item.uri.toString())
                        .setMimeType(item.mimeType.takeIf { it.isNotBlank() && '*' !in it })
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(item.title)
                                .setArtist("Forge")
                                .setArtworkUri(item.albumArtUri)
                                .setIsPlayable(true)
                                .build(),
                        )
                        .build()
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
                supported = false,
                note = "Audio delay is not exposed by Media3 MediaController/session player without a custom AudioSink. Skipped gracefully.",
                onDismiss = { panel = Panel.None },
                onChange = { audioDelayMs = it },
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    showPip: Boolean,
    showAspect: Boolean,
    showSnapshot: Boolean,
    speed: Float,
    sleepLabel: String?,
    abLabel: String?,
    boostLabel: String?,
    moreExpanded: Boolean,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onSpeed: () -> Unit,
    onLock: () -> Unit,
    onMore: () -> Unit,
    onDismissMore: () -> Unit,
    onSubtitles: () -> Unit,
    onAudio: () -> Unit,
    onAspect: () -> Unit,
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 4.dp, vertical = 8.dp),
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
        IconButton(onClick = onSpeed) {
            Icon(Icons.Rounded.Speed, contentDescription = "Speed", tint = Color.White)
        }
        Text(
            text = formatSpeed(speed),
            style = MaterialTheme.typography.labelSmall,
            color = ForgeAccent,
            modifier = Modifier.padding(end = 4.dp),
        )
        if (showPip) {
            IconButton(onClick = onPip) {
                Icon(
                    Icons.Outlined.PictureInPictureAlt,
                    contentDescription = "Picture in picture",
                    tint = Color.White,
                )
            }
        }
        IconButton(onClick = onLock) {
            Icon(Icons.Rounded.Lock, contentDescription = "Lock controls", tint = Color.White)
        }
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
                if (showAspect) {
                    DropdownMenuItem(
                        text = { Text("Aspect ratio", color = Color.White) },
                        onClick = onAspect,
                        leadingIcon = {
                            Icon(Icons.Rounded.AspectRatio, null, tint = ForgeAccent)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Sleep timer", color = Color.White) },
                    onClick = onSleep,
                    leadingIcon = {
                        Icon(Icons.Rounded.Timer, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Equalizer", color = Color.White) },
                    onClick = onEqualizer,
                    leadingIcon = {
                        Icon(Icons.Rounded.Equalizer, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("A-B loop", color = Color.White) },
                    onClick = onAbLoop,
                    leadingIcon = {
                        Icon(Icons.Rounded.Loop, null, tint = ForgeAccent)
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
                    text = { Text("Bookmarks", color = Color.White) },
                    onClick = onBookmarks,
                    leadingIcon = {
                        Icon(Icons.Rounded.Bookmark, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Volume boost", color = Color.White) },
                    onClick = onVolumeBoost,
                    leadingIcon = {
                        Icon(Icons.Rounded.VolumeUp, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Subtitle delay", color = Color.White) },
                    onClick = onSubDelay,
                    leadingIcon = {
                        Icon(Icons.Rounded.ClosedCaption, null, tint = ForgeAccent)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Audio delay", color = Color.White) },
                    onClick = onAudioDelay,
                    leadingIcon = {
                        Icon(Icons.Rounded.Audiotrack, null, tint = ForgeAccent)
                    },
                )
                if (showSnapshot) {
                    DropdownMenuItem(
                        text = { Text("Frame snapshot", color = Color.White) },
                        onClick = onSnapshot,
                        leadingIcon = {
                            Icon(Icons.Rounded.CameraAlt, null, tint = ForgeAccent)
                        },
                    )
                }
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
private fun PlayerControls(
    positionMs: Long,
    durationMs: Long,
    scrubbing: Boolean,
    scrubValue: Float,
    isPlaying: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    repeatMode: Int,
    shuffleOn: Boolean,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForgeGraphite.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        val progress = if (durationMs > 0) {
            (if (scrubbing) scrubValue else positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        Slider(
            value = progress,
            onValueChange = onScrub,
            onValueChangeFinished = onScrubEnd,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ForgeAccent,
                activeTrackColor = ForgeAccent,
                inactiveTrackColor = ForgeMuted.copy(alpha = 0.3f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(if (scrubbing) (scrubValue * durationMs).toLong() else positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = ForgeMuted,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = ForgeMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleOn) ForgeAccent else Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            IconButton(onClick = onPrev, enabled = canPrev) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (canPrev) Color.White else ForgeMuted,
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ForgeAccent),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = onNext, enabled = canNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = if (canNext) Color.White else ForgeMuted,
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) Color.White else ForgeAccent,
                    modifier = Modifier.size(26.dp),
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
    hasExternal: Boolean,
    delayMs: Int,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSelectTrack: (TrackChoice) -> Unit,
    onSize: (Float) -> Unit,
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onBand: (Int, Float) -> Unit,
    onPreset: (com.gketch.forge.playback.EqPreset) -> Unit,
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

private fun collectTracks(tracks: Tracks, type: @C.TrackType Int): List<TrackChoice> {
    val out = mutableListOf<TrackChoice>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != type) return@forEachIndexed
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val format = group.getTrackFormat(i)
            val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
            val label = format.label?.takeIf { it.isNotBlank() }
                ?: lang
                ?: "${if (type == C.TRACK_TYPE_TEXT) "Subtitle" else "Audio"} ${out.size + 1}"
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

private fun applyExternalSubtitle(player: Player, uri: android.net.Uri) {
    val current = player.currentMediaItem ?: return
    val index = player.currentMediaItemIndex
    val position = player.currentPosition
    val ready = player.playWhenReady
    val path = (uri.lastPathSegment ?: uri.toString()).lowercase()
    val mime = when {
        path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        else -> MimeTypes.APPLICATION_SUBRIP
    }
    val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
        .setMimeType(mime)
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

private fun windowBrightness(activity: Activity?): Float {
    val window = activity?.window ?: return 0.5f
    val current = window.attributes.screenBrightness
    if (current >= 0f) return current.coerceIn(0f, 1f)
    return try {
        Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    } catch (_: Exception) {
        0.5f
    }
}

private fun setWindowBrightness(activity: Activity?, fraction: Float) {
    val window = activity?.window ?: return
    val lp = window.attributes
    lp.screenBrightness = fraction.coerceIn(0.01f, 1f)
    window.attributes = lp
}
