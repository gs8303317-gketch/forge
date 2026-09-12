package com.gketch.forge.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.MediaKind
import com.gketch.forge.ui.library.formatDuration
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerScreen(
    queue: List<ForgeMediaItem>,
    startIndex: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var index by remember { mutableStateOf(startIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))) }
    val current = queue.getOrNull(index)

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && index < queue.lastIndex) {
                    index += 1
                }
            }
        }
        player.addListener(listener)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Restore portrait when leaving player if we forced landscape
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(index, current?.uri) {
        val item = current ?: return@LaunchedEffect
        if (item.kind == MediaKind.VIDEO) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        player.setMediaItem(MediaItem.fromUri(item.uri))
        player.prepare()
        player.play()
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (!scrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L).takeIf { it > 0 } ?: 0L
            }
            delay(200)
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBlack),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = current?.title ?: "Player",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (current?.kind == MediaKind.VIDEO) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.player = player
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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
                        text = current?.title ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForgeGraphite)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            val progress = if (durationMs > 0) {
                (if (scrubbing) scrubValue else positionMs.toFloat() / durationMs.toFloat())
                    .coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progress,
                onValueChange = {
                    scrubbing = true
                    scrubValue = it
                },
                onValueChangeFinished = {
                    val seekTo = (scrubValue * durationMs).toLong()
                    player.seekTo(seekTo)
                    positionMs = seekTo
                    scrubbing = false
                },
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
                IconButton(
                    onClick = { if (index > 0) index -= 1 },
                    enabled = index > 0,
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (index > 0) Color.White else ForgeMuted,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(
                    onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
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
                IconButton(
                    onClick = { if (index < queue.lastIndex) index += 1 },
                    enabled = index < queue.lastIndex,
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = if (index < queue.lastIndex) Color.White else ForgeMuted,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}
