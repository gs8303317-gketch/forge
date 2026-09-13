package com.gketch.forge.ui.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.gketch.forge.ui.player.rememberPlayerController
import com.gketch.forge.ui.player.stopCompletely
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.thumb.ForgeThumbnailUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun MiniPlayerBar(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onStopped: () -> Unit = {},
) {
    val controller = rememberPlayerController()
    var visible by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var artUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(true) }
    var hasNext by remember { mutableStateOf(false) }

    fun sync() {
        val p = controller
        if (p == null || p.mediaItemCount == 0 || p.currentMediaItem == null) {
            visible = false
            hasNext = false
            return
        }
        visible = true
        hasNext = runCatching { p.hasNextMediaItem() }.getOrDefault(false)
        title = p.mediaMetadata.title?.toString()
            ?: p.currentMediaItem?.mediaMetadata?.title?.toString()
            ?: "Now playing"
        playing = p.isPlaying
        val dur = p.duration
        progress = if (dur > 0) (p.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        val meta = p.mediaMetadata
        artUri = meta.artworkUri
            ?: p.currentMediaItem?.mediaMetadata?.artworkUri
            ?: p.currentMediaItem?.localConfiguration?.uri
        val mime = p.currentMediaItem?.localConfiguration?.mimeType.orEmpty()
        isVideo = !mime.startsWith("audio")
    }

    DisposableEffect(controller) {
        val p = controller
        if (p == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    sync()
                }
            }
            p.addListener(listener)
            sync()
            onDispose { p.removeListener(listener) }
        }
    }

    LaunchedEffect(controller, visible, playing) {
        if (controller == null || !visible || !playing) return@LaunchedEffect
        while (isActive) {
            sync()
            delay(1000)
        }
    }

    if (!visible) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ForgeGraphite)
            .clickable(onClick = onExpand),
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = ForgeAccent,
            trackColor = Color.White.copy(alpha = 0.12f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ForgeThumbnailUri(
                uri = artUri,
                isVideo = isVideo,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val p = controller ?: return@IconButton
                    if (p.isPlaying) p.pause() else p.play()
                },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                )
            }
            IconButton(
                onClick = {
                    val p = controller ?: return@IconButton
                    runCatching {
                        if (p.hasNextMediaItem()) p.seekToNextMediaItem()
                    }
                },
                enabled = hasNext,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = if (hasNext) 0.92f else 0.32f),
                )
            }
            IconButton(
                onClick = {
                    controller?.stopCompletely()
                    visible = false
                    onStopped()
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Stop",
                    tint = Color.White.copy(alpha = 0.72f),
                )
            }
        }
    }
}
