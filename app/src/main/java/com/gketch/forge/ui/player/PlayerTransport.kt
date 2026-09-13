package com.gketch.forge.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.gketch.forge.R
import com.gketch.forge.ui.library.formatDuration
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeMuted

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
    val vPad = if (landscape) 4.dp else 8.dp
    val scrubTouch = if (landscape) 28.dp else 32.dp
    val secondaryIcon = if (landscape) 20.dp else 22.dp
    val playSize = if (landscape) 56.dp else 68.dp
    val playIcon = if (landscape) 32.dp else 40.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onInteract,
            )
            .padding(horizontal = if (landscape) 16.dp else 10.dp, vertical = vPad),
    ) {
        val playProgress = if (durationMs > 0) {
            (if (scrubbing) scrubValue else positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val bufferedFrac = if (durationMs > 0) {
            (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val displayPos = if (scrubbing) (scrubValue * durationMs).toLong() else positionMs
        val remainingMs = (durationMs - displayPos).coerceAtLeast(0L)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (showRemainingTime) "−${formatDuration(remainingMs)}" else formatDuration(displayPos),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.width(52.dp).clip(RoundedCornerShape(4.dp)).clickable {
                    onToggleRemainingTime()
                    onInteract()
                },
            )
            Box(modifier = Modifier.weight(1f).height(scrubTouch).padding(horizontal = 8.dp)) {
                BufferedProgressTrack(
                    progress = playProgress,
                    buffered = bufferedFrac,
                    trackHeight = if (landscape) 3.dp else 4.dp,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
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
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.width(52.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = if (landscape) 2.dp else 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFrameStep) {
                IconButton(onClick = { onFrameStep(false) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous frame", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
                }
            }
            IconButton(onClick = onPrev, enabled = canPrev, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = if (canPrev) Color.White else ForgeMuted, modifier = Modifier.size(34.dp))
            }
            Box(
                modifier = Modifier.size(playSize).clip(CircleShape).background(ForgeAccent).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(playIcon),
                )
            }
            IconButton(onClick = onNext, enabled = canNext, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = if (canNext) Color.White else ForgeMuted, modifier = Modifier.size(34.dp))
            }
            if (showFrameStep) {
                IconButton(onClick = { onFrameStep(true) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next frame", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = if (landscape) 0.dp else 2.dp, bottom = if (landscape) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showAspect) {
                Box(
                    modifier = Modifier.size(40.dp).combinedClickable(onClick = onCycleAspect, onLongClick = onLongAspect),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AspectRatio, contentDescription = stringResource(R.string.aspect_cycle, aspectLabel), tint = ForgeAccent, modifier = Modifier.size(secondaryIcon))
                }
            }
            IconButton(onClick = onQueue, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = stringResource(R.string.queue), tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(secondaryIcon))
            }
            if (showChapters) {
                IconButton(onClick = onChapterPrev, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.chapter_prev), tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(secondaryIcon))
                }
            }
            if (showOrientToggle) {
                IconButton(onClick = onToggleOrient, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ScreenRotation, contentDescription = stringResource(R.string.orient_toggle, orientLabel), tint = if (orientLabel == "Auto") Color.White.copy(alpha = 0.85f) else ForgeAccent, modifier = Modifier.size(secondaryIcon))
                }
            }
            if (showSkipIntro && skipIntroSeconds > 0) {
                IconButton(onClick = onSkipIntro, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.FastForward, contentDescription = stringResource(R.string.skip_intro) + " ${skipIntroSeconds}s", tint = ForgeAccent.copy(alpha = 0.95f), modifier = Modifier.size(secondaryIcon))
                }
            }
            if (showChapters) {
                IconButton(onClick = onChapterNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.chapter_next), tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(secondaryIcon))
                }
            }
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleOn) ForgeAccent else Color.White.copy(alpha = 0.92f), modifier = Modifier.size(secondaryIcon))
            }
            IconButton(onClick = onCycleRepeat, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.85f) else ForgeAccent,
                    modifier = Modifier.size(secondaryIcon),
                )
            }
        }
    }
}
