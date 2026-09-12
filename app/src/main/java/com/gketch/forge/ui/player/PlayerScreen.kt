package com.gketch.forge.ui.player

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gketch.forge.MainActivity
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.MediaKind
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
    var showSpeeds by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    var loadedKey by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

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
                showSpeeds = false
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
                    if (newIndex in queue.indices) index = newIndex
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

                override fun onEvents(p: Player, events: Player.Events) {
                    val video = p.currentTracks.groups.any { group ->
                        group.type == C.TRACK_TYPE_VIDEO && group.isSelected
                    }
                    if (p.currentTracks.groups.isNotEmpty()) {
                        hasVideo = video || current?.kind == MediaKind.VIDEO && p.videoSize.width > 0
                    }
                }
            }
            player.addListener(listener)
            isPlaying = player.isPlaying
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
        player.play()
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

    LaunchedEffect(controlsVisible, isPlaying, inPip) {
        if (controlsVisible && isPlaying && !inPip) {
            delay(4_000)
            controlsVisible = false
            showSpeeds = false
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
                if (hasVideo || current?.kind == MediaKind.VIDEO) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                this.player = controller
                            }
                        },
                        update = { it.player = controller },
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
                        onVolumeFraction = { setMusicVolume(context, it) },
                        onBrightnessFraction = { setWindowBrightness(activity, it) },
                        onTap = {
                            controlsVisible = !controlsVisible
                            if (!controlsVisible) showSpeeds = false
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
                showPip = hasVideo || current?.kind == MediaKind.VIDEO,
                onBack = {
                    val player = controller
                    val uri = current?.uri?.toString()
                    if (player != null && uri != null) {
                        scope.launch { resumeStore.savePosition(uri, player.currentPosition, player.duration) }
                    }
                    onBack()
                },
                onPip = { activity?.enterPip() },
                onSpeed = { showSpeeds = !showSpeeds },
                speed = speed,
            )
        }

        if (showChrome && showSpeeds) {
            SpeedRow(
                selected = speed,
                onSelect = { next ->
                    speed = next
                    controller?.setPlaybackSpeed(next)
                    showSpeeds = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            )
        }

        AnimatedVisibility(visible = showChrome, modifier = Modifier.align(Alignment.BottomCenter)) {
            PlayerControls(
                positionMs = positionMs,
                durationMs = durationMs,
                scrubbing = scrubbing,
                scrubValue = scrubValue,
                isPlaying = isPlaying,
                canPrev = index > 0,
                canNext = index < queue.lastIndex,
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
                    if (index > 0) {
                        index -= 1
                        controller?.seekToPreviousMediaItem()
                    }
                },
                onPlayPause = {
                    val player = controller ?: return@PlayerControls
                    if (player.isPlaying) player.pause() else player.play()
                },
                onNext = {
                    if (index < queue.lastIndex) {
                        index += 1
                        controller?.seekToNextMediaItem()
                    }
                },
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    showPip: Boolean,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onSpeed: () -> Unit,
    speed: Float,
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
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSpeed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, contentDescription = "Speed", tint = Color.White)
            }
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
    }
}

@Composable
private fun SpeedRow(
    selected: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SPEED_PRESETS.forEach { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                label = { Text(formatSpeed(preset)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForgeAccent,
                    selectedLabelColor = Color.Black,
                    containerColor = ForgeGraphite,
                    labelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun PlayerControls(
    positionMs: Long,
    durationMs: Long,
    scrubbing: Boolean,
    scrubValue: Float,
    isPlaying: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
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
        }
    }
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

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}×"
    } else {
        "${speed}×"
    }
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
