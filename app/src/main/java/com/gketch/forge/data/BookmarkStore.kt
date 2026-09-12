package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.bookmarkDataStore by preferencesDataStore(name = "forge_bookmarks")

data class MediaBookmark(
    val id: String,
    val mediaUri: String,
    val positionMs: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

class BookmarkStore(context: Context) {
    private val store = context.applicationContext.bookmarkDataStore

    val bookmarks: Flow<List<MediaBookmark>> = store.data.map { prefs ->
        decode(prefs[KEY] ?: "[]")
    }

    fun forMedia(all: List<MediaBookmark>, mediaUri: String): List<MediaBookmark> =
        all.filter { it.mediaUri == mediaUri }.sortedBy { it.positionMs }

    suspend fun add(mediaUri: String, positionMs: Long, name: String?): MediaBookmark {
        val bookmark = MediaBookmark(
            id = UUID.randomUUID().toString(),
            mediaUri = mediaUri,
            positionMs = positionMs.coerceAtLeast(0L),
            name = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: formatTimestamp(positionMs),
        )
        mutate { list -> (list + bookmark).takeLast(MAX_ITEMS) }
        return bookmark
    }

    suspend fun remove(id: String) {
        mutate { list -> list.filterNot { it.id == id } }
    }

    suspend fun removeForMedia(mediaUri: String) {
        mutate { list -> list.filterNot { it.mediaUri == mediaUri } }
    }

    private suspend fun mutate(block: (List<MediaBookmark>) -> List<MediaBookmark>) {
        store.edit { prefs ->
            val current = decode(prefs[KEY] ?: "[]")
            prefs[KEY] = encode(block(current))
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("bookmarks_json")
        const val MAX_ITEMS = 500

        fun encode(items: List<MediaBookmark>): String {
            val arr = JSONArray()
            items.forEach { b ->
                arr.put(
                    JSONObject()
                        .put("id", b.id)
                        .put("mediaUri", b.mediaUri)
                        .put("positionMs", b.positionMs)
                        .put("name", b.name)
                        .put("createdAt", b.createdAt),
                )
            }
            return arr.toString()
        }

        fun decode(raw: String): List<MediaBookmark> {
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            MediaBookmark(
                                id = o.getString("id"),
                                mediaUri = o.getString("mediaUri"),
                                positionMs = o.optLong("positionMs"),
                                name = o.optString("name", formatTimestamp(o.optLong("positionMs"))),
                                createdAt = o.optLong("createdAt"),
                            ),
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun formatTimestamp(ms: Long): String {
            val totalSec = (ms / 1000L).coerceAtLeast(0L)
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        }
    }
}
