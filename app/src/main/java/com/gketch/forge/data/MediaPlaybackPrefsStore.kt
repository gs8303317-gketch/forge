package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

private val Context.mediaPlaybackPrefsDataStore by preferencesDataStore(name = "forge_media_playback_prefs")

data class MediaPlaybackPrefs(
    /** Last playback speed for this URI, or null if never set. */
    val speed: Float? = null,
    /** AspectMode.name, or null if never set. */
    val aspect: String? = null,
)

/**
 * Persist last playback speed + aspect mode per media URI.
 */
class MediaPlaybackPrefsStore(context: Context) {
    private val store = context.applicationContext.mediaPlaybackPrefsDataStore

    suspend fun get(uri: String): MediaPlaybackPrefs {
        val prefs = store.data.first()
        val hex = keyHex(uri)
        return MediaPlaybackPrefs(
            speed = prefs[floatPreferencesKey("spd_$hex")],
            aspect = prefs[stringPreferencesKey("asp_$hex")],
        )
    }

    suspend fun saveSpeed(uri: String, speed: Float) {
        val hex = keyHex(uri)
        val v = speed.coerceIn(0.25f, 3f)
        store.edit { it[floatPreferencesKey("spd_$hex")] = v }
    }

    suspend fun saveAspect(uri: String, aspectName: String) {
        val hex = keyHex(uri)
        store.edit { it[stringPreferencesKey("asp_$hex")] = aspectName.take(32) }
    }

    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    companion object {
        fun keyHex(uri: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray())
            return digest.take(12).joinToString("") { "%02x".format(it) }
        }
    }
}
