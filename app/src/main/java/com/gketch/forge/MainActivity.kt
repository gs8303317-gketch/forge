package com.gketch.forge

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gketch.forge.data.AppLanguage
import com.gketch.forge.data.AppSettings
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.isPlayableStreamUrl
import com.gketch.forge.playback.ForgeStreamOptions
import com.gketch.forge.ui.navigation.ForgeNav
import com.gketch.forge.ui.theme.ForgeTheme
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {

    private var externalUri by mutableStateOf<Uri?>(null)
    private var externalMime by mutableStateOf<String?>(null)

    var pipAllowed: Boolean = false
        private set
    private var pipAspect: Rational = Rational(16, 9)
    private var appliedLanguage: AppLanguage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            val appStore = remember { AppSettingsStore(this) }
            val appSettings by appStore.settings.collectAsState(initial = AppSettings())
            LaunchedEffect(appSettings.streamUserAgent, appSettings.streamTimeoutSec) {
                ForgeStreamOptions.update(appSettings.streamUserAgent, appSettings.streamTimeoutSec)
            }
            LaunchedEffect(appSettings.appLanguage) {
                if (appliedLanguage != appSettings.appLanguage) {
                    appliedLanguage = appSettings.appLanguage
                    applyAppLanguage(appSettings.appLanguage)
                }
            }
            ForgeTheme(
                accentPreset = appSettings.accentPreset,
                dynamicColor = appSettings.dynamicColor,
            ) {
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

    private fun applyAppLanguage(lang: AppLanguage) {
        val locales = when (lang) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.HINDI -> LocaleListCompat.forLanguageTags("hi")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data != null) {
                    val scheme = data.scheme?.lowercase()
                    if (scheme == "http" || scheme == "https" || scheme == "rtsp" || scheme == "rtsps" ||
                        scheme == "content" || scheme == "file"
                    ) {
                        externalUri = data
                        externalMime = intent.type
                        return
                    }
                }
                // text/plain VIEW body may carry a URL
                extractUrlFromText(intent)?.let { url ->
                    externalUri = Uri.parse(url)
                    externalMime = intent.type ?: "text/plain"
                }
            }
            Intent.ACTION_SEND -> {
                extractUrlFromText(intent)?.let { url ->
                    externalUri = Uri.parse(url)
                    externalMime = intent.type ?: "text/plain"
                }
            }
        }
    }

    private fun extractUrlFromText(intent: Intent): String? {
        val candidates = listOfNotNull(
            intent.getStringExtra(Intent.EXTRA_TEXT),
            intent.getStringExtra(Intent.EXTRA_SUBJECT),
            intent.dataString,
        )
        for (raw in candidates) {
            val found = findPlayableUrl(raw) ?: continue
            return found
        }
        return null
    }

    companion object {
        private val URL_PATTERN = Pattern.compile(
            "(https?://\\S+|rtsp[s]?://\\S+)",
            Pattern.CASE_INSENSITIVE,
        )

        fun findPlayableUrl(raw: String): String? {
            val trimmed = raw.trim().trim('"')
            if (isPlayableStreamUrl(trimmed)) return trimmed
            val matcher = URL_PATTERN.matcher(trimmed)
            while (matcher.find()) {
                val candidate = matcher.group(1)?.trim()?.trimEnd('.', ',', ')', ']', '"') ?: continue
                if (isPlayableStreamUrl(candidate)) return candidate
            }
            return null
        }

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
