package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.watchedDataStore by preferencesDataStore(name = "forge_watched")

class WatchedStore(context: Context) {
    private val store = context.applicationContext.watchedDataStore

    val watchedKeys: Flow<Set<String>> = store.data.map { prefs ->
        prefs[KEY] ?: emptySet()
    }

    suspend fun markWatched(uri: String) {
        val k = keyFor(uri)
        store.edit { prefs ->
            prefs[KEY] = (prefs[KEY] ?: emptySet()) + k
        }
    }

    suspend fun markUnwatched(uri: String) {
        val k = keyFor(uri)
        store.edit { prefs ->
            prefs[KEY] = (prefs[KEY] ?: emptySet()) - k
        }
    }

    suspend fun toggle(uri: String): Boolean {
        var nowWatched = false
        val k = keyFor(uri)
        store.edit { prefs ->
            val cur = (prefs[KEY] ?: emptySet()).toMutableSet()
            if (cur.contains(k)) {
                cur.remove(k)
                nowWatched = false
            } else {
                cur.add(k)
                nowWatched = true
            }
            prefs[KEY] = cur
        }
        return nowWatched
    }

    fun isWatched(keys: Set<String>, uri: String): Boolean =
        keys.contains(keyFor(uri))

    companion object {
        private val KEY = stringSetPreferencesKey("watched_uri_keys")
        const val AUTO_MARK_NEAR_END_MS = 8_000L

        fun keyFor(uri: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray())
            return digest.take(12).joinToString("") { "%02x".format(it) }
        }
    }
}

enum class WatchedFilter(val label: String) {
    ALL("All"),
    UNWATCHED("Unwatched"),
    WATCHED("Watched"),
}
