package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

private val Context.trackPrefsDataStore by preferencesDataStore(name = "forge_track_prefs")

data class TrackPrefs(
    /** Last selected audio track label (or null = unset / auto). */
    val audioLabel: String? = null,
    /** Last selected text/subtitle track label (or null when unset). */
    val textLabel: String? = null,
    /** User explicitly disabled subtitles for this URI. */
    val textDisabled: Boolean = false,
)

/**
 * Persist last selected audio + text track per media URI (matched by label on reopen).
 */
class TrackPrefsStore(context: Context) {
    private val store = context.applicationContext.trackPrefsDataStore

    suspend fun get(uri: String): TrackPrefs {
        val prefs = store.data.first()
        val hex = keyHex(uri)
        return TrackPrefs(
            audioLabel = prefs[stringPreferencesKey("aud_$hex")],
            textLabel = prefs[stringPreferencesKey("txt_$hex")],
            textDisabled = prefs[booleanPreferencesKey("toff_$hex")] ?: false,
        )
    }

    suspend fun saveAudio(uri: String, label: String) {
        val hex = keyHex(uri)
        store.edit { it[stringPreferencesKey("aud_$hex")] = label.take(120) }
    }

    suspend fun saveText(uri: String, label: String) {
        val hex = keyHex(uri)
        store.edit {
            it[stringPreferencesKey("txt_$hex")] = label.take(120)
            it[booleanPreferencesKey("toff_$hex")] = false
        }
    }

    suspend fun saveTextDisabled(uri: String, disabled: Boolean) {
        val hex = keyHex(uri)
        store.edit {
            it[booleanPreferencesKey("toff_$hex")] = disabled
            if (disabled) it.remove(stringPreferencesKey("txt_$hex"))
        }
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
