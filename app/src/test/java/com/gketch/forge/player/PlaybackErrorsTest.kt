package com.gketch.forge.player

import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorsTest {
    @Test
    fun userMessagePrefersCauseOverUnexpectedRuntimeLabel() {
        val msg = PlaybackErrors.userMessage(
            message = "Unexpected runtime error",
            cause = IllegalStateException("Audio sink flush failed"),
            errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
        )
        assertEquals("Audio sink flush failed", msg)
    }

    @Test
    fun autoRetryAllowsUnexpectedRuntimeWithinBudget() {
        assertTrue(
            PlaybackErrors.shouldAutoRetry(
                message = "Unexpected runtime error",
                errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                exoType = ExoPlaybackException.TYPE_UNEXPECTED,
                attemptAlready = 0,
            ),
        )
        assertTrue(
            PlaybackErrors.shouldAutoRetry(
                message = "Unexpected runtime error",
                errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                exoType = ExoPlaybackException.TYPE_UNEXPECTED,
                attemptAlready = 1,
            ),
        )
        assertFalse(
            PlaybackErrors.shouldAutoRetry(
                message = "Unexpected runtime error",
                errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                exoType = ExoPlaybackException.TYPE_UNEXPECTED,
                attemptAlready = 2,
            ),
        )
    }

    @Test
    fun balanceFaultLatchDefaultsOff() {
        ForgeBalance.resetFaultLatch()
        assertFalse(ForgeBalance.isDisabled)
    }

    @Test
    fun permanentFailuresDoNotRetry() {
        assertFalse(
            PlaybackErrors.shouldAutoRetry(
                message = "File not found",
                errorCode = PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                exoType = null,
                attemptAlready = 0,
            ),
        )
        assertFalse(
            PlaybackErrors.shouldAutoRetry(
                message = "Unexpected runtime error",
                errorCode = PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                exoType = ExoPlaybackException.TYPE_SOURCE,
                attemptAlready = 0,
            ),
        )
    }

    @Test
    fun networkAndTimeoutErrorsRetryWithinBudget() {
        assertTrue(
            PlaybackErrors.shouldAutoRetry(
                message = "timeout",
                errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                exoType = null,
                attemptAlready = 0,
            ),
        )
        assertTrue(
            PlaybackErrors.shouldAutoRetry(
                message = "timeout",
                errorCode = PlaybackException.ERROR_CODE_TIMEOUT,
                exoType = null,
                attemptAlready = 1,
            ),
        )
        assertFalse(
            PlaybackErrors.shouldAutoRetry(
                message = "timeout",
                errorCode = PlaybackException.ERROR_CODE_TIMEOUT,
                exoType = null,
                attemptAlready = 2,
            ),
        )
    }

    @Test
    fun safeSeekNeverLeavesKnownDuration() {
        assertEquals(5_000L, PlaybackErrors.safeSeekTarget(4_000L, 10_000L, 1_000L))
        assertEquals(10_000L, PlaybackErrors.safeSeekTarget(9_000L, 10_000L, 5_000L))
        assertEquals(0L, PlaybackErrors.safeSeekTarget(500L, 10_000L, -2_000L))
        assertEquals(4_000L, PlaybackErrors.safeSeekTarget(4_000L, 0L, 50_000L))
        assertEquals(4_000L, PlaybackErrors.safeSeekTarget(4_000L, -1L, 50_000L))
    }

    @Test
    fun uiFaultDetectedFromComposeCause() {
        assertTrue(
            PlaybackErrors.isLikelyUiFault(
                message = "Unexpected runtime error",
                cause = IllegalStateException("Reading a state that was created after the snapshot was taken in androidx.compose.runtime"),
            ),
        )
        assertFalse(
            PlaybackErrors.isLikelyUiFault(
                message = "Unexpected runtime error",
                cause = IllegalStateException("Audio sink flush failed"),
            ),
        )
    }
}
