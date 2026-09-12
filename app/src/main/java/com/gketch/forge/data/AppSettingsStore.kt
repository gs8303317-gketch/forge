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

enum class LibrarySort(val label: String) {
    NAME("Name"),
    DATE("Date"),
    SIZE("Size"),
    DURATION("Duration"),
}

enum class SubtitleColor(val label: String, val argb: Int) {
    WHITE("White", android.graphics.Color.WHITE),
    YELLOW("Yellow", android.graphics.Color.YELLOW),
    CYAN("Cyan", android.graphics.Color.CYAN),
    GREEN("Green", android.graphics.Color.GREEN),
    RED("Red", 0xFFFF5252.toInt()),
}

enum class SubtitleBackground(val label: String) {
    NONE("None"),
    SEMI("Semi"),
    BLACK("Black"),
}

enum class SubtitlePosition(val label: String, val bottomFraction: Float) {
    BOTTOM("Bottom", 0.08f),
    MID("Mid", 0.22f),
    HIGH("High", 0.40f),
}

enum class AccentPreset(val label: String, val color: Long, val soft: Long) {
    EMBER("Ember", 0xFFFF6B35, 0xFFFF8F66),
    AMBER("Amber", 0xFFFFB020, 0xFFFFC85A),
    LIME("Lime", 0xFFB8E63B, 0xFFD4F06A),
    TEAL("Teal", 0xFF2EC4B6, 0xFF7EE0D6),
    SKY("Sky", 0xFF4DA3FF, 0xFF8AC4FF),
    VIOLET("Violet", 0xFFB388FF, 0xFFD0B3FF),
    ROSE("Rose", 0xFFFF5D8F, 0xFFFF8FB0),
}

enum class SleepEndAction(val label: String) {
    PAUSE("Pause"),
    STOP("Stop"),
}

enum class GestureSensitivity(val label: String, val multiplier: Float) {
    LOW("Low", 0.55f),
    NORMAL("Normal", 1.0f),
    HIGH("High", 1.65f),
}

data class AppSettings(
    val seekSeconds: Int = 10,
    val autoplayNext: Boolean = true,
    val librarySort: LibrarySort = LibrarySort.NAME,
    val subtitleColor: SubtitleColor = SubtitleColor.WHITE,
    val subtitleBackground: SubtitleBackground = SubtitleBackground.SEMI,
    val subtitlePosition: SubtitlePosition = SubtitlePosition.BOTTOM,
    val subtitleSizeSp: Float = 20f,
    val accentPreset: AccentPreset = AccentPreset.EMBER,
    val dynamicColor: Boolean = false,
    val sleepFadeEnabled: Boolean = false,
    val sleepFadeSeconds: Int = 10,
    val sleepEndAction: SleepEndAction = SleepEndAction.PAUSE,
    val defaultPlaybackSpeed: Float = 1.0f,
    val lastLibraryTab: String = "VIDEO",
    val gestureSensitivity: GestureSensitivity = GestureSensitivity.NORMAL,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("seekSeconds", seekSeconds)
        .put("autoplayNext", autoplayNext)
        .put("librarySort", librarySort.name)
        .put("subtitleColor", subtitleColor.name)
        .put("subtitleBackground", subtitleBackground.name)
        .put("subtitlePosition", subtitlePosition.name)
        .put("subtitleSizeSp", subtitleSizeSp.toDouble())
        .put("accentPreset", accentPreset.name)
        .put("dynamicColor", dynamicColor)
        .put("sleepFadeEnabled", sleepFadeEnabled)
        .put("sleepFadeSeconds", sleepFadeSeconds)
        .put("sleepEndAction", sleepEndAction.name)
        .put("defaultPlaybackSpeed", defaultPlaybackSpeed.toDouble())
        .put("lastLibraryTab", lastLibraryTab)
        .put("gestureSensitivity", gestureSensitivity.name)
}

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
        val SEEK_OPTIONS = listOf(5, 10, 15, 30)
        val FADE_OPTIONS = listOf(5, 10, 15, 30)
        val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    }
}
