package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.appSettingsDataStore by preferencesDataStore(name = "forge_app_settings")

typealias LibrarySort = com.gketch.forge.domain.LibrarySort
typealias SubtitleColor = com.gketch.forge.domain.SubtitleColor
typealias SubtitleBackground = com.gketch.forge.domain.SubtitleBackground
typealias SubtitlePosition = com.gketch.forge.domain.SubtitlePosition
typealias AccentPreset = com.gketch.forge.domain.AccentPreset
typealias SleepEndAction = com.gketch.forge.domain.SleepEndAction
typealias GestureSensitivity = com.gketch.forge.domain.GestureSensitivity
typealias AppLanguage = com.gketch.forge.domain.AppLanguage
typealias MinClipLength = com.gketch.forge.domain.MinClipLength
typealias CrossfadeDuration = com.gketch.forge.domain.CrossfadeDuration
typealias ResumeBehavior = com.gketch.forge.domain.ResumeBehavior
typealias SkipIntroSeconds = com.gketch.forge.domain.SkipIntroSeconds
typealias HoldToSpeed = com.gketch.forge.domain.HoldToSpeed
typealias SubtitleOutline = com.gketch.forge.domain.SubtitleOutline
typealias AudioFocusBehavior = com.gketch.forge.domain.AudioFocusBehavior
typealias ChromeHideDelay = com.gketch.forge.domain.ChromeHideDelay
typealias AppSettings = com.gketch.forge.domain.AppSettings

class AppSettingsStore(context: Context) {
    private val store = context.applicationContext.appSettingsDataStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            seekSeconds = (p[KEY_SEEK] ?: 10).coerceIn(5, 30).let { s ->
                when (s) {
                    5, 10, 15, 30 -> s
                    else -> 10
                }
            },
            autoplayNext = p[KEY_AUTOPLAY] ?: true,
            librarySort = runCatching {
                LibrarySort.valueOf(p[KEY_SORT] ?: LibrarySort.NAME.name)
            }.getOrDefault(LibrarySort.NAME),
            subtitleColor = runCatching {
                SubtitleColor.valueOf(p[KEY_SUB_COLOR] ?: SubtitleColor.WHITE.name)
            }.getOrDefault(SubtitleColor.WHITE),
            subtitleBackground = runCatching {
                SubtitleBackground.valueOf(p[KEY_SUB_BG] ?: SubtitleBackground.SEMI.name)
            }.getOrDefault(SubtitleBackground.SEMI),
            subtitlePosition = runCatching {
                SubtitlePosition.valueOf(p[KEY_SUB_POS] ?: SubtitlePosition.BOTTOM.name)
            }.getOrDefault(SubtitlePosition.BOTTOM),
            subtitleSizeSp = p[KEY_SUB_SIZE] ?: 20f,
            accentPreset = runCatching {
                AccentPreset.valueOf(p[KEY_ACCENT] ?: AccentPreset.EMBER.name)
            }.getOrDefault(AccentPreset.EMBER),
            dynamicColor = p[KEY_DYNAMIC] ?: false,
            sleepFadeEnabled = p[KEY_SLEEP_FADE] ?: false,
            sleepFadeSeconds = (p[KEY_SLEEP_FADE_SEC] ?: 10).let { s ->
                when (s) {
                    5, 10, 15, 30 -> s
                    else -> 10
                }
            },
            sleepEndAction = runCatching {
                SleepEndAction.valueOf(p[KEY_SLEEP_END] ?: SleepEndAction.PAUSE.name)
            }.getOrDefault(SleepEndAction.PAUSE),
            defaultPlaybackSpeed = (p[KEY_DEFAULT_SPEED] ?: 1.0f).let { s ->
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).minBy { kotlin.math.abs(it - s) }
            },
            lastLibraryTab = p[KEY_LAST_TAB] ?: "VIDEO",
            gestureSensitivity = runCatching {
                GestureSensitivity.valueOf(p[KEY_GESTURE_SENS] ?: GestureSensitivity.NORMAL.name)
            }.getOrDefault(GestureSensitivity.NORMAL),
            minClipLength = runCatching {
                MinClipLength.valueOf(p[KEY_MIN_CLIP] ?: MinClipLength.OFF.name)
            }.getOrDefault(MinClipLength.OFF),
            appLanguage = runCatching {
                AppLanguage.valueOf(p[KEY_LANG] ?: AppLanguage.SYSTEM.name)
            }.getOrDefault(AppLanguage.SYSTEM),
            streamUserAgent = p[KEY_STREAM_UA] ?: "",
            streamTimeoutSec = (p[KEY_STREAM_TIMEOUT] ?: 20).let { t ->
                when (t) {
                    10, 20, 30, 60 -> t
                    else -> 20
                }
            },
            gaplessPlayback = p[KEY_GAPLESS] ?: true,
            crossfade = runCatching {
                CrossfadeDuration.valueOf(p[KEY_CROSSFADE] ?: CrossfadeDuration.OFF.name)
            }.getOrDefault(CrossfadeDuration.OFF),
            loudnessNormalize = p[KEY_LOUDNESS_NORM] ?: false,
            chromeHideDelay = runCatching {
                ChromeHideDelay.valueOf(p[KEY_CHROME_HIDE_DELAY] ?: "")
            }.getOrElse {
                // Migrate 1.16 boolean toggle → delay picker
                if (p[KEY_CONTROLS_AUTO_HIDE] == false) ChromeHideDelay.NEVER
                else ChromeHideDelay.SEC_5_5
            },
            resumeBehavior = runCatching {
                ResumeBehavior.valueOf(p[KEY_RESUME_BEHAVIOR] ?: ResumeBehavior.ASK.name)
            }.getOrDefault(ResumeBehavior.ASK),
            seriesAutoNext = p[KEY_SERIES_AUTO_NEXT] ?: false,
            skipIntroSeconds = runCatching {
                SkipIntroSeconds.valueOf(p[KEY_SKIP_INTRO] ?: SkipIntroSeconds.OFF.name)
            }.getOrDefault(SkipIntroSeconds.OFF),
            pauseOnHeadsetUnplug = p[KEY_PAUSE_HEADSET] ?: true,
            showRemainingTime = p[KEY_SHOW_REMAINING] ?: false,
            holdToSpeed = runCatching {
                HoldToSpeed.valueOf(p[KEY_HOLD_SPEED] ?: HoldToSpeed.X2.name)
            }.getOrDefault(HoldToSpeed.X2),
            playerGesturesEnabled = p[KEY_PLAYER_GESTURES] ?: true,
            invertGestureSides = p[KEY_INVERT_GESTURE] ?: false,
            subtitleOutline = runCatching {
                SubtitleOutline.valueOf(p[KEY_SUB_OUTLINE] ?: SubtitleOutline.OUTLINE.name)
            }.getOrDefault(SubtitleOutline.OUTLINE),
            swipeDownToClose = p[KEY_SWIPE_DOWN_CLOSE] ?: true,
            doubleTapToLock = p[KEY_DOUBLE_TAP_LOCK] ?: false,
            audioFocusBehavior = runCatching {
                AudioFocusBehavior.valueOf(p[KEY_AUDIO_FOCUS] ?: AudioFocusBehavior.PAUSE.name)
            }.getOrDefault(AudioFocusBehavior.PAUSE),
        )
    }

    suspend fun setSeekSeconds(value: Int) {
        val v = when (value) {
            5, 10, 15, 30 -> value
            else -> 10
        }
        store.edit { it[KEY_SEEK] = v }
    }

    suspend fun setAutoplayNext(value: Boolean) {
        store.edit { it[KEY_AUTOPLAY] = value }
    }

    suspend fun setLibrarySort(value: LibrarySort) {
        store.edit { it[KEY_SORT] = value.name }
    }

    suspend fun setSubtitleColor(value: SubtitleColor) {
        store.edit { it[KEY_SUB_COLOR] = value.name }
    }

    suspend fun setSubtitleBackground(value: SubtitleBackground) {
        store.edit { it[KEY_SUB_BG] = value.name }
    }

    suspend fun setSubtitlePosition(value: SubtitlePosition) {
        store.edit { it[KEY_SUB_POS] = value.name }
    }

    suspend fun setSubtitleSizeSp(value: Float) {
        store.edit { it[KEY_SUB_SIZE] = value }
    }

    suspend fun setAccentPreset(value: AccentPreset) {
        store.edit { it[KEY_ACCENT] = value.name }
    }

    suspend fun setDynamicColor(value: Boolean) {
        store.edit { it[KEY_DYNAMIC] = value }
    }

    suspend fun setSleepFadeEnabled(value: Boolean) {
        store.edit { it[KEY_SLEEP_FADE] = value }
    }

    suspend fun setSleepFadeSeconds(value: Int) {
        val v = when (value) {
            5, 10, 15, 30 -> value
            else -> 10
        }
        store.edit { it[KEY_SLEEP_FADE_SEC] = v }
    }

    suspend fun setSleepEndAction(value: SleepEndAction) {
        store.edit { it[KEY_SLEEP_END] = value.name }
    }

    suspend fun setDefaultPlaybackSpeed(value: Float) {
        val v = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).minBy { kotlin.math.abs(it - value) }
        store.edit { it[KEY_DEFAULT_SPEED] = v }
    }

    suspend fun setLastLibraryTab(value: String) {
        store.edit { it[KEY_LAST_TAB] = value }
    }

    suspend fun setGestureSensitivity(value: GestureSensitivity) {
        store.edit { it[KEY_GESTURE_SENS] = value.name }
    }

    suspend fun setMinClipLength(value: MinClipLength) {
        store.edit { it[KEY_MIN_CLIP] = value.name }
    }

    suspend fun setAppLanguage(value: AppLanguage) {
        store.edit { it[KEY_LANG] = value.name }
    }

    suspend fun setStreamUserAgent(value: String) {
        store.edit { it[KEY_STREAM_UA] = value.trim().take(256) }
    }

    suspend fun setStreamTimeoutSec(value: Int) {
        val v = when (value) {
            10, 20, 30, 60 -> value
            else -> 20
        }
        store.edit { it[KEY_STREAM_TIMEOUT] = v }
    }

    suspend fun setGaplessPlayback(value: Boolean) {
        store.edit { it[KEY_GAPLESS] = value }
    }

    suspend fun setCrossfade(value: CrossfadeDuration) {
        store.edit { it[KEY_CROSSFADE] = value.name }
    }

    suspend fun setLoudnessNormalize(value: Boolean) {
        store.edit { it[KEY_LOUDNESS_NORM] = value }
    }

    suspend fun setChromeHideDelay(value: ChromeHideDelay) {
        store.edit { it[KEY_CHROME_HIDE_DELAY] = value.name }
    }

    suspend fun setResumeBehavior(value: ResumeBehavior) {
        store.edit { it[KEY_RESUME_BEHAVIOR] = value.name }
    }

    suspend fun setSeriesAutoNext(value: Boolean) {
        store.edit { it[KEY_SERIES_AUTO_NEXT] = value }
    }

    suspend fun setSkipIntroSeconds(value: SkipIntroSeconds) {
        store.edit { it[KEY_SKIP_INTRO] = value.name }
    }

    suspend fun setPauseOnHeadsetUnplug(value: Boolean) {
        store.edit { it[KEY_PAUSE_HEADSET] = value }
    }

    suspend fun setShowRemainingTime(value: Boolean) {
        store.edit { it[KEY_SHOW_REMAINING] = value }
    }

    suspend fun setHoldToSpeed(value: HoldToSpeed) {
        store.edit { it[KEY_HOLD_SPEED] = value.name }
    }

    suspend fun setPlayerGesturesEnabled(value: Boolean) {
        store.edit { it[KEY_PLAYER_GESTURES] = value }
    }

    suspend fun setInvertGestureSides(value: Boolean) {
        store.edit { it[KEY_INVERT_GESTURE] = value }
    }

    suspend fun setSubtitleOutline(value: SubtitleOutline) {
        store.edit { it[KEY_SUB_OUTLINE] = value.name }
    }

    suspend fun setSwipeDownToClose(value: Boolean) {
        store.edit { it[KEY_SWIPE_DOWN_CLOSE] = value }
    }

    suspend fun setDoubleTapToLock(value: Boolean) {
        store.edit { it[KEY_DOUBLE_TAP_LOCK] = value }
    }

    suspend fun setAudioFocusBehavior(value: AudioFocusBehavior) {
        store.edit { it[KEY_AUDIO_FOCUS] = value.name }
    }

    suspend fun replaceFromJson(o: JSONObject) {
        store.edit { p ->
            if (o.has("seekSeconds")) p[KEY_SEEK] = o.optInt("seekSeconds", 10)
            if (o.has("autoplayNext")) p[KEY_AUTOPLAY] = o.optBoolean("autoplayNext", true)
            if (o.has("librarySort")) p[KEY_SORT] = o.optString("librarySort", LibrarySort.NAME.name)
            if (o.has("subtitleColor")) p[KEY_SUB_COLOR] = o.optString("subtitleColor")
            if (o.has("subtitleBackground")) p[KEY_SUB_BG] = o.optString("subtitleBackground")
            if (o.has("subtitlePosition")) p[KEY_SUB_POS] = o.optString("subtitlePosition")
            if (o.has("subtitleSizeSp")) p[KEY_SUB_SIZE] = o.optDouble("subtitleSizeSp", 20.0).toFloat()
            if (o.has("accentPreset")) p[KEY_ACCENT] = o.optString("accentPreset")
            if (o.has("dynamicColor")) p[KEY_DYNAMIC] = o.optBoolean("dynamicColor", false)
            if (o.has("sleepFadeEnabled")) p[KEY_SLEEP_FADE] = o.optBoolean("sleepFadeEnabled", false)
            if (o.has("sleepFadeSeconds")) p[KEY_SLEEP_FADE_SEC] = o.optInt("sleepFadeSeconds", 10)
            if (o.has("sleepEndAction")) p[KEY_SLEEP_END] = o.optString("sleepEndAction")
            if (o.has("defaultPlaybackSpeed")) p[KEY_DEFAULT_SPEED] = o.optDouble("defaultPlaybackSpeed", 1.0).toFloat()
            if (o.has("lastLibraryTab")) p[KEY_LAST_TAB] = o.optString("lastLibraryTab", "VIDEO")
            if (o.has("gestureSensitivity")) p[KEY_GESTURE_SENS] = o.optString("gestureSensitivity")
            if (o.has("minClipLength")) p[KEY_MIN_CLIP] = o.optString("minClipLength")
            if (o.has("appLanguage")) p[KEY_LANG] = o.optString("appLanguage")
            if (o.has("streamUserAgent")) p[KEY_STREAM_UA] = o.optString("streamUserAgent")
            if (o.has("streamTimeoutSec")) p[KEY_STREAM_TIMEOUT] = o.optInt("streamTimeoutSec", 20)
            if (o.has("gaplessPlayback")) p[KEY_GAPLESS] = o.optBoolean("gaplessPlayback", true)
            if (o.has("crossfade")) p[KEY_CROSSFADE] = o.optString("crossfade")
            if (o.has("loudnessNormalize")) p[KEY_LOUDNESS_NORM] = o.optBoolean("loudnessNormalize", false)
            if (o.has("chromeHideDelay")) {
                p[KEY_CHROME_HIDE_DELAY] = o.optString("chromeHideDelay", ChromeHideDelay.SEC_5_5.name)
            } else if (o.has("controlsAutoHide")) {
                p[KEY_CHROME_HIDE_DELAY] = if (o.optBoolean("controlsAutoHide", true)) {
                    ChromeHideDelay.SEC_5_5.name
                } else {
                    ChromeHideDelay.NEVER.name
                }
            }
            if (o.has("resumeBehavior")) p[KEY_RESUME_BEHAVIOR] = o.optString("resumeBehavior")
            if (o.has("seriesAutoNext")) p[KEY_SERIES_AUTO_NEXT] = o.optBoolean("seriesAutoNext", false)
            if (o.has("skipIntroSeconds")) p[KEY_SKIP_INTRO] = o.optString("skipIntroSeconds", SkipIntroSeconds.OFF.name)
            if (o.has("pauseOnHeadsetUnplug")) p[KEY_PAUSE_HEADSET] = o.optBoolean("pauseOnHeadsetUnplug", true)
            if (o.has("showRemainingTime")) p[KEY_SHOW_REMAINING] = o.optBoolean("showRemainingTime", false)
            if (o.has("holdToSpeed")) p[KEY_HOLD_SPEED] = o.optString("holdToSpeed", HoldToSpeed.X2.name)
            if (o.has("playerGesturesEnabled")) p[KEY_PLAYER_GESTURES] = o.optBoolean("playerGesturesEnabled", true)
            if (o.has("invertGestureSides")) p[KEY_INVERT_GESTURE] = o.optBoolean("invertGestureSides", false)
            if (o.has("subtitleOutline")) p[KEY_SUB_OUTLINE] = o.optString("subtitleOutline", SubtitleOutline.OUTLINE.name)
            if (o.has("swipeDownToClose")) p[KEY_SWIPE_DOWN_CLOSE] = o.optBoolean("swipeDownToClose", true)
            if (o.has("doubleTapToLock")) p[KEY_DOUBLE_TAP_LOCK] = o.optBoolean("doubleTapToLock", false)
            if (o.has("audioFocusBehavior")) p[KEY_AUDIO_FOCUS] = o.optString("audioFocusBehavior", AudioFocusBehavior.PAUSE.name)
        }
    }

    companion object {
        private val KEY_SEEK = intPreferencesKey("seek_seconds")
        private val KEY_AUTOPLAY = booleanPreferencesKey("autoplay_next")
        private val KEY_SORT = stringPreferencesKey("library_sort")
        private val KEY_SUB_COLOR = stringPreferencesKey("sub_color")
        private val KEY_SUB_BG = stringPreferencesKey("sub_bg")
        private val KEY_SUB_POS = stringPreferencesKey("sub_pos")
        private val KEY_SUB_SIZE = floatPreferencesKey("sub_size_sp")
        private val KEY_ACCENT = stringPreferencesKey("accent_preset")
        private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val KEY_SLEEP_FADE = booleanPreferencesKey("sleep_fade")
        private val KEY_SLEEP_FADE_SEC = intPreferencesKey("sleep_fade_sec")
        private val KEY_SLEEP_END = stringPreferencesKey("sleep_end_action")
        private val KEY_DEFAULT_SPEED = floatPreferencesKey("default_playback_speed")
        private val KEY_LAST_TAB = stringPreferencesKey("last_library_tab")
        private val KEY_GESTURE_SENS = stringPreferencesKey("gesture_sensitivity")
        private val KEY_MIN_CLIP = stringPreferencesKey("min_clip_length")
        private val KEY_LANG = stringPreferencesKey("app_language")
        private val KEY_STREAM_UA = stringPreferencesKey("stream_user_agent")
        private val KEY_STREAM_TIMEOUT = intPreferencesKey("stream_timeout_sec")
        private val KEY_GAPLESS = booleanPreferencesKey("gapless_playback")
        private val KEY_CROSSFADE = stringPreferencesKey("crossfade")
        private val KEY_LOUDNESS_NORM = booleanPreferencesKey("loudness_normalize")
        private val KEY_CONTROLS_AUTO_HIDE = booleanPreferencesKey("controls_auto_hide") // legacy migrate
        private val KEY_CHROME_HIDE_DELAY = stringPreferencesKey("chrome_hide_delay")
        private val KEY_RESUME_BEHAVIOR = stringPreferencesKey("resume_behavior")
        private val KEY_SERIES_AUTO_NEXT = booleanPreferencesKey("series_auto_next")
        private val KEY_SKIP_INTRO = stringPreferencesKey("skip_intro_seconds")
        private val KEY_PAUSE_HEADSET = booleanPreferencesKey("pause_on_headset_unplug")
        private val KEY_SHOW_REMAINING = booleanPreferencesKey("show_remaining_time")
        private val KEY_HOLD_SPEED = stringPreferencesKey("hold_to_speed")
        private val KEY_PLAYER_GESTURES = booleanPreferencesKey("player_gestures_enabled")
        private val KEY_INVERT_GESTURE = booleanPreferencesKey("invert_gesture_sides")
        private val KEY_SUB_OUTLINE = stringPreferencesKey("subtitle_outline")
        private val KEY_SWIPE_DOWN_CLOSE = booleanPreferencesKey("swipe_down_to_close")
        private val KEY_DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_to_lock")
        private val KEY_AUDIO_FOCUS = stringPreferencesKey("audio_focus_behavior")
        val SEEK_OPTIONS = listOf(5, 10, 15, 30)
        val TIMEOUT_OPTIONS = listOf(10, 20, 30, 60)
        val FADE_OPTIONS = listOf(5, 10, 15, 30)
        val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    }
}
