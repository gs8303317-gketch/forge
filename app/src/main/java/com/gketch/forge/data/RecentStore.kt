package com.gketch.forge.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.recentDataStore by preferencesDataStore(name = "forge_recent")

class RecentStore(context: Context) {
    private val store = context.applicationContext.recentDataStore

    val recent: Flow<List<ForgeMediaItem>> = store.data.map { prefs ->
        decode(prefs[KEY] ?: "[]")
    }

    suspend fun record(item: ForgeMediaItem) {
        store.edit { prefs ->
            val current = decode(prefs[KEY] ?: "[]").toMutableList()
            current.removeAll { it.uri == item.uri }
            current.add(0, item.copy(dateAdded = System.currentTimeMillis() / 1000))
            prefs[KEY] = encode(current.take(MAX_ITEMS))
        }
    }

    suspend fun clear() {
        store.edit { it.remove(KEY) }
    }

    companion object {
        private val KEY = stringPreferencesKey("recent_json")
        const val MAX_ITEMS = 20

        private fun encode(items: List<ForgeMediaItem>): String {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("uri", item.uri.toString())
                        .put("title", item.title)
                        .put("durationMs", item.durationMs)
                        .put("sizeBytes", item.sizeBytes)
                        .put("mimeType", item.mimeType)
                        .put("kind", item.kind.name)
                        .put("dateAdded", item.dateAdded)
                        .put("albumArtUri", item.albumArtUri?.toString()),
                )
            }
            return arr.toString()
        }

        private fun decode(raw: String): List<ForgeMediaItem> {
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val art = o.optString("albumArtUri", "").takeIf { it.isNotBlank() }
                        add(
                            ForgeMediaItem(
                                id = o.optLong("id"),
                                uri = Uri.parse(o.getString("uri")),
                                title = o.optString("title", "Media"),
                                durationMs = o.optLong("durationMs"),
                                sizeBytes = o.optLong("sizeBytes"),
                                mimeType = o.optString("mimeType", "video/*"),
                                kind = runCatching {
                                    MediaKind.valueOf(o.optString("kind", MediaKind.VIDEO.name))
                                }.getOrDefault(MediaKind.VIDEO),
                                dateAdded = o.optLong("dateAdded"),
                                albumArtUri = art?.let { Uri.parse(it) },
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
