package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

private val Context.resumeDataStore by preferencesDataStore(name = "forge_resume")

class ResumeStore(context: Context) {
    private val store = context.applicationContext.resumeDataStore

    suspend fun getPosition(uri: String): Long {
        val key = longPreferencesKey(keyFor(uri))
        return store.data.first()[key] ?: 0L
    }

    suspend fun savePosition(uri: String, positionMs: Long, durationMs: Long) {
        when {
            positionMs < MIN_SAVE_MS -> clear(uri)
            durationMs > 0L && positionMs >= durationMs - NEAR_END_MS -> clear(uri)
            else -> {
                val key = longPreferencesKey(keyFor(uri))
                store.edit { it[key] = positionMs }
            }
        }
    }

    suspend fun clear(uri: String) {
        val key = longPreferencesKey(keyFor(uri))
        store.edit { it.remove(key) }
    }

    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    companion object {
        const val MIN_SAVE_MS = 3_000L
        const val NEAR_END_MS = 5_000L
        /** Positions above this prompt Continue vs Start over. */
        const val RESUME_PROMPT_MS = 5_000L

        fun keyFor(uri: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray())
            val hex = digest.take(12).joinToString("") { "%02x".format(it) }
            return "pos_$hex"
        }
    }
}
