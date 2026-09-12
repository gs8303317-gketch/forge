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
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeStore
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

private enum class Panel { None, Speed, Aspect, Sleep, Subtitle, Audio }

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
                        activity?.updatePipParams(
                            allowed = true,
                            aspect = Rational(videoSize.width, videoSize.height),
                        )
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    textTracks = collectTracks(tracks, C.TRACK_TYPE_TEXT)
                    audioTracks = collectTracks(tracks, C.TRACK_TYPE_AUDIO)
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

    LaunchedEffect(hasVideo, current?.kind, inPip) {
        val video = hasVideo || current?.kind == MediaKind.VIDEO
        if (video && !inPip) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (!video) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        activity?.updatePipParams(allowed = video && !inPip)
    }

    LaunchedEffect(controller) {
        val player = controller ?: return@LaunchedEffect
        while (isActive) {
            if (!scrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L).takeIf { it > 0 } ?: 0L
            }
            delay(200)
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

    LaunchedEffect(controlsVisible, isPlaying, inPip, panel) {
        if (controlsVisible && isPlaying && !inPip && panel == Panel.None) {
            delay(4_000)
            controlsVisible = false
        }
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

    val showChrome = controlsVisible && !inPip
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
                            }
                        },
                        update = {
                            it.player = controller
                            it.resizeMode = aspect.resizeMode
                            playerViewRef = it
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
                            controlsVisible = !controlsVisible
                            if (!controlsVisible) panel = Panel.None
                        },
                        currentVolume = { musicVolumeFraction(context) },
                        currentBrightness = { windowBrightness(activity) },
                    )
                }
            }
        }

        AnimatedVisibility(visible = showChrome, modifier = Modifier.align(Alignment.TopCenter)) {
            PlayerTopBar(
                title = current?.title ?: "Player",
                showPip = isVideoSurface,
                speed = speed,
                sleepLabel = sleepRemainingSec.takeIf { it > 0 }?.let { formatSleep(it) },
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
                showAspect = isVideoSurface,
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

        if (panel == Panel.Subtitle && showChrome) {
            SubtitleDialog(
                tracks = textTracks,
                enabled = subtitlesEnabled,
                sizeSp = subtitleSizeSp,
                hasExternal = externalSubtitleUri != null,
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
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    showPip: Boolean,
    showAspect: Boolean,
    speed: Float,
    sleepLabel: String?,
    moreExpanded: Boolean,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onSpeed: () -> Unit,
    onMore: () -> Unit,
    onDismissMore: () -> Unit,
    onSubtitles: () -> Unit,
    onAudio: () -> Unit,
    onAspect: () -> Unit,
    onSleep: () -> Unit,
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
            if (sleepLabel != null) {
                Text(
                    text = "Sleep · $sleepLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForgeAccent,
                )
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
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSelectTrack: (TrackChoice) -> Unit,
    onSize: (Float) -> Unit,
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
