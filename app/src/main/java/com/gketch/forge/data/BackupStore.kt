package com.gketch.forge.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupStore(private val context: Context) {
    private val app = context.applicationContext
    private val settingsStore = AppSettingsStore(app)
    private val playlistStore = PlaylistStore(app)
    private val favoritesStore = FavoritesStore(app)
    private val streamsStore = SavedStreamsStore(app)
    private val bookmarkStore = BookmarkStore(app)
    private val hiddenStore = HiddenFoldersStore(app)
    private val safStore = SafFoldersStore(app)

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val settings = settingsStore.settings.first()
        val playlists = playlistStore.playlists.first()
        val favorites = favoritesStore.favorites.first()
        val streams = streamsStore.streams.first()
        val bookmarks = bookmarkStore.bookmarks.first()
        val hidden = hiddenStore.snapshot()
        val saf = safStore.snapshot()
        JSONObject()
            .put("app", "forge")
            .put("version", 2)
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", settings.toJson())
            .put("playlists", JSONArray().also { arr ->
                playlists.forEach { pl ->
                    arr.put(
                        JSONObject()
                            .put("id", pl.id)
                            .put("name", pl.name)
                            .put("updatedAt", pl.updatedAt)
                            .put("items", JSONArray().also { items ->
                                pl.items.forEach { items.put(FavoritesStore.itemToJson(it)) }
                            }),
                    )
                }
            })
            .put("favorites", JSONArray().also { arr ->
                favorites.forEach { arr.put(FavoritesStore.itemToJson(it)) }
            })
            .put("streams", JSONArray().also { arr ->
                streams.forEach { s ->
                    arr.put(
                        JSONObject()
                            .put("id", s.id)
                            .put("url", s.url)
                            .put("name", s.name)
                            .put("createdAt", s.createdAt),
                    )
                }
            })
            .put("bookmarks", JSONArray().also { arr ->
                bookmarks.forEach { b ->
                    arr.put(
                        JSONObject()
                            .put("id", b.id)
                            .put("mediaUri", b.mediaUri)
                            .put("positionMs", b.positionMs)
                            .put("name", b.name)
                            .put("createdAt", b.createdAt),
                    )
                }
            })
            .put("hiddenFolders", JSONArray().also { arr ->
                hidden.forEach { f ->
                    arr.put(JSONObject().put("bucketId", f.bucketId).put("name", f.name))
                }
            })
            .put("safFolders", JSONArray().also { arr ->
                saf.forEach { f ->
                    arr.put(
                        JSONObject()
                            .put("uri", f.uri)
                            .put("name", f.name)
                            .put("addedAt", f.addedAt),
                    )
                }
            })
            .toString(2)
    }

    suspend fun writeExport(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = exportJson()
            app.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Could not write file")
        }
    }

    suspend fun importFrom(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("Could not read file")
            importJson(raw)
        }
    }

    suspend fun importJson(raw: String): String {
        val root = JSONObject(raw)
        if (root.optString("app") != "forge" && !root.has("settings") && !root.has("playlists")) {
            error("Not a Forge backup")
        }
        var imported = 0
        root.optJSONObject("settings")?.let {
            settingsStore.replaceFromJson(it)
            imported++
        }
        root.optJSONArray("playlists")?.let { arr ->
            playlistStore.replaceAll(decodePlaylists(arr))
            imported++
        }
        root.optJSONArray("favorites")?.let { arr ->
            favoritesStore.replaceAll(decodeItems(arr))
            imported++
        }
        root.optJSONArray("streams")?.let { arr ->
            streamsStore.replaceAll(decodeStreams(arr))
            imported++
        }
        root.optJSONArray("bookmarks")?.let { arr ->
            bookmarkStore.replaceAll(decodeBookmarks(arr))
            imported++
        }
        root.optJSONArray("hiddenFolders")?.let { arr ->
            hiddenStore.replaceAll(HiddenFoldersStore.decode(arr.toString()))
        }
        root.optJSONArray("safFolders")?.let { arr ->
            safStore.replaceAll(SafFoldersStore.decode(arr.toString()))
        }
        if (imported == 0) error("Backup contained no recognizable data")
        return "Restored backup"
    }

    private fun decodePlaylists(arr: JSONArray): List<ForgePlaylist> = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            add(
                ForgePlaylist(
                    id = o.optString("id"),
                    name = o.optString("name", "Playlist"),
                    items = decodeItems(itemsArr),
                    updatedAt = o.optLong("updatedAt"),
                ),
            )
        }
    }

    private fun decodeItems(arr: JSONArray): List<ForgeMediaItem> = buildList {
        for (i in 0 until arr.length()) {
            add(FavoritesStore.itemFromJson(arr.getJSONObject(i)))
        }
    }

    private fun decodeStreams(arr: JSONArray): List<SavedStream> = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(
                SavedStream(
                    id = o.optString("id"),
                    url = o.optString("url"),
                    name = o.optString("name", "Stream"),
                    createdAt = o.optLong("createdAt"),
                ),
            )
        }
    }

    private fun decodeBookmarks(arr: JSONArray): List<MediaBookmark> = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(
                MediaBookmark(
                    id = o.optString("id"),
                    mediaUri = o.optString("mediaUri"),
                    positionMs = o.optLong("positionMs"),
                    name = o.optString("name", ""),
                    createdAt = o.optLong("createdAt"),
                ),
            )
        }
    }
}
