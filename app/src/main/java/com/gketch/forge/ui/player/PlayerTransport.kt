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

/** VLC-style transport: times beside seek, large orange play, aspect-first tools. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun VlcPlayerControls(
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
) = PlayerControls(
    progress, scrubbing, scrubValue, isPlaying, canPrev, canNext, repeatMode, shuffleOn,
    showFrameStep, showAspect, aspectLabel, showOrientToggle, orientLabel, showChapters,
    showSkipIntro, skipIntroSeconds, onSkipIntro, onInteract, onFrameStep, onCycleAspect,
    onLongAspect, onToggleOrient, onQueue, onChapterPrev, onChapterNext, showRemainingTime,
    onToggleRemainingTime, onScrub, onScrubEnd, onPrev, onPlayPause, onNext, onCycleRepeat,
    onToggleShuffle,
)
