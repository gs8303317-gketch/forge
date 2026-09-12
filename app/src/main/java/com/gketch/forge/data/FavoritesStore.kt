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

private val Context.favoritesDataStore by preferencesDataStore(name = "forge_favorites")

class FavoritesStore(context: Context) {
    private val store = context.applicationContext.favoritesDataStore

    val favorites: Flow<List<ForgeMediaItem>> = store.data.map { prefs ->
        decode(prefs[KEY] ?: "[]")
    }

    suspend fun toggle(item: ForgeMediaItem): Boolean {
        var nowFavorite = false
        store.edit { prefs ->
            val current = decode(prefs[KEY] ?: "[]").toMutableList()
            val idx = current.indexOfFirst { it.uri == item.uri }
            if (idx >= 0) {
                current.removeAt(idx)
                nowFavorite = false
            } else {
                current.add(0, item)
                nowFavorite = true
            }
            prefs[KEY] = encode(current.take(MAX_ITEMS))
        }
        return nowFavorite
    }

    suspend fun addAll(items: List<ForgeMediaItem>) {
        if (items.isEmpty()) return
        store.edit { prefs ->
            val current = decode(prefs[KEY] ?: "[]").toMutableList()
            items.asReversed().forEach { item ->
                current.removeAll { it.uri == item.uri }
                current.add(0, item)
            }
            prefs[KEY] = encode(current.take(MAX_ITEMS))
        }
    }

    suspend fun replaceAll(items: List<ForgeMediaItem>) {
        store.edit { it[KEY] = encode(items.take(MAX_ITEMS)) }
    }

    suspend fun isFavorite(uri: Uri): Boolean {
        return false // use flow; helper for sync checks via contains
    }

    fun contains(list: List<ForgeMediaItem>, uri: Uri): Boolean =
        list.any { it.uri == uri }

    companion object {
        private val KEY = stringPreferencesKey("favorites_json")
        const val MAX_ITEMS = 500

        fun encode(items: List<ForgeMediaItem>): String {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(itemToJson(item))
            }
            return arr.toString()
        }

        fun decode(raw: String): List<ForgeMediaItem> {
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        add(itemFromJson(arr.getJSONObject(i)))
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun itemToJson(item: ForgeMediaItem): JSONObject =
            JSONObject()
                .put("id", item.id)
                .put("uri", item.uri.toString())
                .put("title", item.title)
                .put("durationMs", item.durationMs)
                .put("sizeBytes", item.sizeBytes)
                .put("mimeType", item.mimeType)
                .put("kind", item.kind.name)
                .put("dateAdded", item.dateAdded)
                .put("albumArtUri", item.albumArtUri?.toString())
                .put("bucketId", item.bucketId)
                .put("bucketName", item.bucketName)
                .put("relativePath", item.relativePath)

        fun itemFromJson(o: JSONObject): ForgeMediaItem {
            val art = o.optString("albumArtUri", "").takeIf { it.isNotBlank() }
            return ForgeMediaItem(
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
                bucketId = o.optLong("bucketId"),
                bucketName = o.optString("bucketName", ""),
                relativePath = o.optString("relativePath", ""),
            )
        }
    }
}
