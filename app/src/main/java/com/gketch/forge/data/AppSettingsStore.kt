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

data class AppSettings(
    val seekSeconds: Int = 10,
    val autoplayNext: Boolean = true,
    val librarySort: LibrarySort = LibrarySort.NAME,
    val subtitleColor: SubtitleColor = SubtitleColor.WHITE,
    val subtitleBackground: SubtitleBackground = SubtitleBackground.SEMI,
    val subtitlePosition: SubtitlePosition = SubtitlePosition.BOTTOM,
    val subtitleSizeSp: Float = 20f,
)

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

    companion object {
        private val KEY_SEEK = intPreferencesKey("seek_seconds")
        private val KEY_AUTOPLAY = booleanPreferencesKey("autoplay_next")
        private val KEY_SORT = stringPreferencesKey("library_sort")
        private val KEY_SUB_COLOR = stringPreferencesKey("sub_color")
        private val KEY_SUB_BG = stringPreferencesKey("sub_bg")
        private val KEY_SUB_POS = stringPreferencesKey("sub_pos")
        private val KEY_SUB_SIZE = floatPreferencesKey("sub_size_sp")
        val SEEK_OPTIONS = listOf(5, 10, 15, 30)
    }
}
