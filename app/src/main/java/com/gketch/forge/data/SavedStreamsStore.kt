package com.gketch.forge.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.savedStreamsDataStore by preferencesDataStore(name = "forge_saved_streams")

data class SavedStream(
    val id: String,
    val url: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toMediaItem(): ForgeMediaItem = forgeItemFromUri(Uri.parse(url), title = name)
}

class SavedStreamsStore(context: Context) {
    private val store = context.applicationContext.savedStreamsDataStore

    val streams: Flow<List<SavedStream>> = store.data.map { prefs ->
        decode(prefs[KEY] ?: "[]").sortedByDescending { it.createdAt }
    }

    suspend fun save(url: String, name: String?): SavedStream? {
        val trimmed = url.trim()
        if (!isPlayableStreamUrl(trimmed)) return null
        val stream = SavedStream(
            id = UUID.randomUUID().toString(),
            url = trimmed,
            name = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: Uri.parse(trimmed).lastPathSegment?.let { Uri.decode(it) }?.takeIf { it.isNotBlank() }
                ?: "Saved stream",
        )
        mutate { list ->
            val withoutDup = list.filterNot { it.url == trimmed }
            (listOf(stream) + withoutDup).take(MAX_ITEMS)
        }
        return stream
    }

    suspend fun rename(id: String, name: String) {
        mutate { list ->
            list.map {
                if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it
            }
        }
    }

    suspend fun remove(id: String) {
        mutate { list -> list.filterNot { it.id == id } }
    }

    private suspend fun mutate(block: (List<SavedStream>) -> List<SavedStream>) {
        store.edit { prefs ->
            val current = decode(prefs[KEY] ?: "[]")
            prefs[KEY] = encode(block(current))
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("saved_streams_json")
        const val MAX_ITEMS = 200

        fun encode(items: List<SavedStream>): String {
            val arr = JSONArray()
            items.forEach { s ->
                arr.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("url", s.url)
                        .put("name", s.name)
                        .put("createdAt", s.createdAt),
                )
            }
            return arr.toString()
        }

        fun decode(raw: String): List<SavedStream> {
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            SavedStream(
                                id = o.getString("id"),
                                url = o.getString("url"),
                                name = o.optString("name", "Stream"),
                                createdAt = o.optLong("createdAt"),
                            ),
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
