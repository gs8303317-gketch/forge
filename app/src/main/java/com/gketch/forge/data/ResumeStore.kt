package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

private val Context.resumeDataStore by preferencesDataStore(name = "forge_resume")

data class ContinueWatchItem(
    val item: ForgeMediaItem,
    val positionMs: Long,
    val progress: Float,
)

class ResumeStore(context: Context) {
    private val store = context.applicationContext.resumeDataStore

    suspend fun getPosition(uri: String): Long {
        val key = longPreferencesKey(keyFor(uri))
        return store.data.first()[key] ?: 0L
    }

    suspend fun positionSnapshot(): Map<String, Long> {
        val prefs = store.data.first()
        val out = LinkedHashMap<String, Long>()
        for ((key, value) in prefs.asMap()) {
            if (key.name.startsWith("pos_") && value is Long) {
                out[key.name] = value
            }
        }
        return out
    }

    fun positionOf(snapshot: Map<String, Long>, uri: String): Long =
        snapshot[keyFor(uri)] ?: 0L

    fun continueWatching(
        candidates: List<ForgeMediaItem>,
        snapshot: Map<String, Long>,
        videosOnly: Boolean = true,
    ): List<ContinueWatchItem> {
        val pool = if (videosOnly) candidates.filter { it.isVideo } else candidates
        return pool.mapNotNull { item ->
            val pos = positionOf(snapshot, item.uri.toString())
            if (pos < MIN_SAVE_MS) return@mapNotNull null
            val dur = item.durationMs
            if (dur > 0L && pos >= dur - NEAR_END_MS) return@mapNotNull null
            val progress = if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 0.99f) else 0.15f
            ContinueWatchItem(item = item, positionMs = pos, progress = progress)
        }.sortedByDescending { it.positionMs }.take(8)
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
        const val RESUME_PROMPT_MS = 5_000L

        fun keyFor(uri: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray())
            val hex = digest.take(12).joinToString("") { "%02x".format(it) }
            return "pos_$hex"
        }
    }
}
