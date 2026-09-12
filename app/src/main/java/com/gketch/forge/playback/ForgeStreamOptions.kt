package com.gketch.forge.playback

/**
 * Process-local stream network options (User-Agent + timeouts) for ExoPlayer HTTP.
 * Updated from AppSettings; applied when the player is created / media sources built.
 */
object ForgeStreamOptions {
    const val DEFAULT_UA = "Forge/1.13.0 (Android; Media3)"
    const val DEFAULT_TIMEOUT_MS = 20_000

    @Volatile
    var userAgent: String = DEFAULT_UA
        private set

    @Volatile
    var connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS
        private set

    @Volatile
    var readTimeoutMs: Int = DEFAULT_TIMEOUT_MS
        private set

    fun update(userAgentRaw: String, timeoutSec: Int) {
        userAgent = userAgentRaw.trim().ifBlank { DEFAULT_UA }
        val ms = (timeoutSec.coerceIn(5, 120) * 1000)
        connectTimeoutMs = ms
        readTimeoutMs = ms
    }
}
