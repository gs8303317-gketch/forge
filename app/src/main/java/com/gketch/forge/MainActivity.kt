package com.gketch.forge

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gketch.forge.ui.navigation.ForgeNav
import com.gketch.forge.ui.theme.ForgeTheme

class MainActivity : ComponentActivity() {

    private var externalUri by mutableStateOf<Uri?>(null)
    private var externalMime by mutableStateOf<String?>(null)

    var pipAllowed: Boolean = false
        private set
    private var pipAspect: Rational = Rational(16, 9)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            ForgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ForgeNav(
                        externalUri = externalUri,
                        externalMime = externalMime,
                        onExternalConsumed = {
                            externalUri = null
                            externalMime = null
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && pipAllowed) {
            enterPip()
        }
    }

    fun updatePipParams(allowed: Boolean, aspect: Rational = pipAspect) {
        pipAllowed = allowed
        pipAspect = clampPipAspect(aspect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(pipAspect)
                        .setAutoEnterEnabled(allowed)
                        .build(),
                )
            } catch (_: RuntimeException) {
                // Device rejected params — ignore
            }
        }
    }

    fun enterPip(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !pipAllowed) return false
        return try {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(clampPipAspect(pipAspect))
                    .build(),
            )
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            externalUri = intent.data
            externalMime = intent.type
        }
    }

    companion object {
        fun clampPipAspect(ratio: Rational): Rational {
            val value = ratio.toFloat()
            return when {
                value < 0.418410f -> Rational(100, 239)
                value > 2.39f -> Rational(239, 100)
                else -> ratio
            }
        }
    }
}
