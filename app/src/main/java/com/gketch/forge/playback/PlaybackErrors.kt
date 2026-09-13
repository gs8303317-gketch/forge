package com.gketch.forge.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException

/**
 * Maps Media3 [PlaybackException] to user-facing text and retry policy.
 *
 * ExoPlayer uses message **"Unexpected runtime error"** for [ExoPlaybackException.TYPE_UNEXPECTED]
 * (a RuntimeException on the playback thread). The useful detail is always in [Throwable.cause].
 */
@UnstableApi
object PlaybackErrors {
    const val MAX_AUTO_RETRIES = 2

    fun userMessage(error: PlaybackException): String =
        userMessage(error.message, error.cause, error.errorCode)

    /** Pure helper (unit-testable without Android SystemClock). */
    fun userMessage(message: String?, cause: Throwable?, errorCode: Int): String {
        val root = deepestCause(cause ?: Throwable())
        val causeText = when {
            cause == null -> null
            else -> root.message?.takeIf { it.isNotBlank() }
                ?: root.javaClass.simpleName.takeIf {
                    it.isNotBlank() && it != "PlaybackException" && it != "ExoPlaybackException" && it != "Throwable"
                }
        }
        val top = message?.takeIf { it.isNotBlank() }
        return when {
            top == "Unexpected runtime error" && causeText != null -> causeText
            top != null && top != "Unexpected runtime error" -> top
            causeText != null -> causeText
            else -> "Playback failed (code $errorCode)"
        }
    }

    fun shouldAutoRetry(error: PlaybackException, attemptAlready: Int): Boolean {
        val exo = error as? ExoPlaybackException
        return shouldAutoRetry(
            message = error.message,
            errorCode = error.errorCode,
            exoType = exo?.type,
            attemptAlready = attemptAlready,
        )
    }

    /** Pure helper (unit-testable without Android SystemClock). */
    fun shouldAutoRetry(
        message: String?,
        errorCode: Int,
        exoType: Int?,
        attemptAlready: Int,
    ): Boolean {
        if (attemptAlready >= MAX_AUTO_RETRIES) return false
        if (errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) return true
        if (errorCode == PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK) return true
        if (message == "Unexpected runtime error") return true
        if (exoType == ExoPlaybackException.TYPE_UNEXPECTED) return true
        return errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
    }


    /**
     * True when the failure looks like a Compose/UI/runtime fault rather than media decode/IO.
     * Player should auto-retry silently and avoid a permanent "Can't play" overlay.
     */
    fun isLikelyUiFault(error: PlaybackException): Boolean =
        isLikelyUiFault(error.message, error.cause)

    fun isLikelyUiFault(message: String?, cause: Throwable?): Boolean {
        val root = deepestCause(cause ?: Throwable())
        val hay = listOfNotNull(message, cause?.message, root.message, root.javaClass.name)
            .joinToString(" ")
            .lowercase()
        return listOf(
            "compose",
            "snapshot",
            "layoutnode",
            "androidx.compose",
            "recompose",
            "modifier.nod",
            "semantics",
        ).any { it in hay }
    }

    private fun deepestCause(t: Throwable): Throwable {
        var cur = t
        var guard = 0
        while (cur.cause != null && cur.cause !== cur && guard++ < 8) {
            cur = cur.cause!!
        }
        return cur
    }
}
