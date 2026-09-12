package com.gketch.forge.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player

/**
 * Progress / buffer / stats state owned outside heavy PlayerScreen reads.
 * Leaf composables (controls, stats, scrubber) subscribe; the parent screen
 * should avoid reading [positionMs] in its body so 10 Hz ticks don't rebuild
 * the whole player tree.
 */
class PlayerProgressState {
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var bufferedMs by mutableLongStateOf(0L)
        private set
    var buffering by mutableStateOf(false)
        private set
    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set

    // Stats (updated ≤4 Hz from a separate loop)
    var statsWidth by mutableIntStateOf(0)
        private set
    var statsHeight by mutableIntStateOf(0)
        private set
    var statsFps by mutableFloatStateOf(0f)
        private set
    var statsBitrateKbps by mutableIntStateOf(-1)
        private set
    var statsBufferedPct by mutableIntStateOf(0)
        private set
    var statsStateLabel by mutableStateOf("Idle")
        private set

    fun updateProgress(
        position: Long,
        duration: Long,
        buffered: Long,
        isBuffering: Boolean,
        state: Int,
    ) {
        positionMs = position
        durationMs = duration
        bufferedMs = buffered
        buffering = isBuffering
        playbackState = state
    }

    fun updateStats(
        width: Int,
        height: Int,
        fps: Float,
        bitrateKbps: Int,
        bufferedPct: Int,
        stateLabel: String,
    ) {
        statsWidth = width
        statsHeight = height
        statsFps = fps
        statsBitrateKbps = bitrateKbps
        statsBufferedPct = bufferedPct
        statsStateLabel = stateLabel
    }
}

fun playbackStateLabel(state: Int): String = when (state) {
    Player.STATE_IDLE -> "Idle"
    Player.STATE_BUFFERING -> "Buffering"
    Player.STATE_READY -> "Ready"
    Player.STATE_ENDED -> "Ended"
    else -> "State $state"
}
