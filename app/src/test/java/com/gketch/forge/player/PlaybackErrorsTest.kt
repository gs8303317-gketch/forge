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
