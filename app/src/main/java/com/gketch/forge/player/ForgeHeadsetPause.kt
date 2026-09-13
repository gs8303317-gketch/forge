package com.gketch.forge.player

/**
 * Live snapshot for headset / BT unplug pause (AUDIO_BECOMING_NOISY).
 * Default on; Settings can disable without recreating ExoPlayer.
 */
object ForgeHeadsetPause {
    @Volatile
    var enabled: Boolean = true
}
