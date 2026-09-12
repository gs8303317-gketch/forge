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

private val Context.playlistDataStore by preferencesDataStore(name = "forge_playlists")

data class ForgePlaylist(
    val id: String,
    val name: String,
    val items: List<ForgeMediaItem>,
    val updatedAt: Long = System.currentTimeMillis(),
)

class PlaylistStore(context: Context) {
    private val store = context.applicationContext.playlistDataStore

    val playlists: Flow<List<ForgePlaylist>> = store.data.map { prefs ->
        decode(prefs[KEY] ?: "[]").sortedByDescending { it.updatedAt }
    }

    suspend fun create(name: String): ForgePlaylist {
        val playlist = ForgePlaylist(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Playlist" },
            items = emptyList(),
        )
        mutate { list -> list + playlist }
        return playlist
    }

    suspend fun rename(id: String, name: String) {
        mutate { list ->
            list.map {
                if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }, updatedAt = System.currentTimeMillis())
                else it
            }
        }
    }

    suspend fun delete(id: String) {
        mutate { list -> list.filterNot { it.id == id } }
    }

    suspend fun addItem(playlistId: String, item: ForgeMediaItem) {
        mutate { list ->
            list.map { pl ->
                if (pl.id != playlistId) pl
                else {
                    val items = pl.items.toMutableList()
                    items.removeAll { it.uri == item.uri }
                    items += item
                    pl.copy(items = items, updatedAt = System.currentTimeMillis())
                }
            }
        }
    }

    suspend fun importParsed(parsed: ParsedM3u): ForgePlaylist {
        val playlist = ForgePlaylist(
            id = UUID.randomUUID().toString(),
            name = parsed.name.trim().ifBlank { "Imported" },
            items = parsed.items,
        )
        mutate { list -> list + playlist }
        return playlist
    }

    suspend fun removeItem(playlistId: String, uri: android.net.Uri) {
        mutate { list ->
            list.map { pl ->
                if (pl.id != playlistId) pl
                else pl.copy(
                    items = pl.items.filterNot { it.uri == uri },
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    private suspend fun mutate(block: (List<ForgePlaylist>) -> List<ForgePlaylist>) {
        store.edit { prefs ->
            val current = decode(prefs[KEY] ?: "[]")
            prefs[KEY] = encode(block(current))
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("playlists_json")

        private fun encode(list: List<ForgePlaylist>): String {
            val arr = JSONArray()
            list.forEach { pl ->
                val items = JSONArray()
                pl.items.forEach { items.put(FavoritesStore.itemToJson(it)) }
                arr.put(
                    JSONObject()
                        .put("id", pl.id)
                        .put("name", pl.name)
                        .put("updatedAt", pl.updatedAt)
                        .put("items", items),
                )
            }
            return arr.toString()
        }

        private fun decode(raw: String): List<ForgePlaylist> {
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val itemsArr = o.optJSONArray("items") ?: JSONArray()
                        val items = buildList {
                            for (j in 0 until itemsArr.length()) {
                                add(FavoritesStore.itemFromJson(itemsArr.getJSONObject(j)))
                            }
                        }
                        add(
                            ForgePlaylist(
                                id = o.getString("id"),
                                name = o.optString("name", "Playlist"),
                                items = items,
                                updatedAt = o.optLong("updatedAt"),
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
