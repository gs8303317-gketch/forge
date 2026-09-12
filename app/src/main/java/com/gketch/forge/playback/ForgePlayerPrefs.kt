package com.gketch.forge.playback

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.forgePlayerDataStore by preferencesDataStore(name = "forge_player_engine")

enum class DecoderPreference(val label: String) {
    AUTO("Auto"),
    HARDWARE("Hardware"),
    SOFTWARE("Software"),
}

enum class BufferPreset(val label: String, val minMs: Int, val maxMs: Int, val playbackMs: Int, val rebufferMs: Int) {
    STANDARD("Standard", 15_000, 50_000, 2_500, 5_000),
    LARGE("Large (network)", 30_000, 120_000, 3_500, 8_000),
    EXTRA("Extra", 50_000, 180_000, 5_000, 10_000),
}

data class EnginePrefs(
    val decoder: DecoderPreference = DecoderPreference.AUTO,
    val buffer: BufferPreset = BufferPreset.LARGE,
    val preciseSeek: Boolean = true,
    val skipSilence: Boolean = false,
    val audioDelayMs: Int = 0,
)

object ForgePlayerPrefs {
    @Volatile
    var snapshot: EnginePrefs = EnginePrefs()
        private set

    fun updateSnapshot(prefs: EnginePrefs) {
        snapshot = prefs
    }
}

class ForgePlayerPrefsStore(context: Context) {
    private val store = context.applicationContext.forgePlayerDataStore

    val prefs: Flow<EnginePrefs> = store.data.map { p ->
        val decoded = EnginePrefs(
            decoder = runCatching {
                DecoderPreference.valueOf(p[KEY_DECODER] ?: DecoderPreference.AUTO.name)
            }.getOrDefault(DecoderPreference.AUTO),
            buffer = runCatching {
                BufferPreset.valueOf(p[KEY_BUFFER] ?: BufferPreset.LARGE.name)
            }.getOrDefault(BufferPreset.LARGE),
            preciseSeek = p[KEY_PRECISE_SEEK] ?: true,
            skipSilence = p[KEY_SKIP_SILENCE] ?: false,
            audioDelayMs = (p[KEY_AUDIO_DELAY] ?: 0).coerceIn(ForgeEngine.MIN_DELAY_MS, ForgeEngine.MAX_DELAY_MS),
        )
        ForgePlayerPrefs.updateSnapshot(decoded)
        decoded
    }

    suspend fun setDecoder(value: DecoderPreference) {
        store.edit { it[KEY_DECODER] = value.name }
    }

    suspend fun setBuffer(value: BufferPreset) {
        store.edit { it[KEY_BUFFER] = value.name }
    }

    suspend fun setPreciseSeek(value: Boolean) {
        store.edit { it[KEY_PRECISE_SEEK] = value }
    }

    suspend fun setSkipSilence(value: Boolean) {
        store.edit { it[KEY_SKIP_SILENCE] = value }
    }

    suspend fun setAudioDelayMs(value: Int) {
        store.edit {
            it[KEY_AUDIO_DELAY] = value.coerceIn(ForgeEngine.MIN_DELAY_MS, ForgeEngine.MAX_DELAY_MS)
        }
    }

    companion object {
        private val KEY_DECODER = stringPreferencesKey("decoder")
        private val KEY_BUFFER = stringPreferencesKey("buffer")
        private val KEY_PRECISE_SEEK = booleanPreferencesKey("precise_seek")
        private val KEY_SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        private val KEY_AUDIO_DELAY = intPreferencesKey("audio_delay_ms")
    }
}
