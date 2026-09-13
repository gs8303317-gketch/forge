package com.gketch.forge

import android.app.PictureInPictureParams
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import com.gketch.forge.data.AppLanguage
import com.gketch.forge.data.AppSettings
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.isPlayableStreamUrl
import com.gketch.forge.player.ForgeStreamOptions
import com.gketch.forge.ui.navigation.ForgeNav
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeTheme
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private var externalUri by mutableStateOf<Uri?>(null)
    private var externalMime by mutableStateOf<String?>(null)

    var pipAllowed: Boolean = false
        private set
    private var pipAspect: Rational = Rational(16, 9)

    override fun onCreate(savedInstanceState: Bundle?) {
        // AppCompatActivity requires Theme.AppCompat (1.14.0 used platform Material → crash).
        setTheme(R.style.Theme_Forge)
        super.onCreate(savedInstanceState)
        // Dark system bars: time / battery / network stay in the system strip
        // and never paint over titles or list rows (portrait or landscape).
        val barColor = AndroidColor.parseColor("#FF121212")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(barColor),
            navigationBarStyle = SystemBarStyle.dark(barColor),
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        consumeIntent(intent)
        setContent {
            val appStore = remember { AppSettingsStore(this@MainActivity) }
            // null until DataStore emits — never apply AppSettings() defaults (SYSTEM),
            // which previously fought the persisted language after Activity recreate
            // and caused an infinite setApplicationLocales → recreate loop (hang).
            var appSettings by remember { mutableStateOf<AppSettings?>(null) }
            LaunchedEffect(appStore) {
                appStore.settings.collect { settings ->
                    applyAppLanguage(settings.appLanguage)
                    appSettings = settings
                }
            }
            val settings = appSettings
            if (settings == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ForgeBlack),
                )
                return@setContent
            }
            LaunchedEffect(settings.streamUserAgent, settings.streamTimeoutSec) {
                ForgeStreamOptions.update(settings.streamUserAgent, settings.streamTimeoutSec)
            }
            ForgeTheme(
                accentPreset = settings.accentPreset,
                dynamicColor = settings.dynamicColor,
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
        val current = AppCompatDelegate.getApplicationLocales()
        if (locales.toLanguageTags() == current.toLanguageTags()) return
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
