package com.gketch.forge.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.safFoldersDataStore by preferencesDataStore(name = "forge_saf_folders")

data class SafFolder(
    val uri: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun treeUri(): Uri = Uri.parse(uri)
}

class SafFoldersStore(context: Context) {
    private val store = context.applicationContext.safFoldersDataStore

    val folders: Flow<List<SafFolder>> = store.data.map { prefs ->
        decode(prefs[KEY] ?: "[]")
    }

    suspend fun snapshot(): List<SafFolder> = folders.first()

    suspend fun add(uri: Uri, name: String): SafFolder {
        val folder = SafFolder(
            uri = uri.toString(),
            name = name.trim().ifBlank { uri.lastPathSegment ?: "Folder" },
        )
        mutate { list ->
            val without = list.filterNot { it.uri == folder.uri }
            without + folder
        }
        return folder
    }

    suspend fun remove(uri: String) {
        mutate { list -> list.filterNot { it.uri == uri } }
    }

    suspend fun replaceAll(folders: List<SafFolder>) {
        store.edit { it[KEY] = encode(folders) }
    }

    private suspend fun mutate(block: (List<SafFolder>) -> List<SafFolder>) {
        store.edit { prefs ->
            prefs[KEY] = encode(block(decode(prefs[KEY] ?: "[]")))
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("saf_folders_json")

        fun encode(items: List<SafFolder>): String {
            val arr = JSONArray()
            items.forEach { f ->
                arr.put(
                    JSONObject()
                        .put("uri", f.uri)
                        .put("name", f.name)
                        .put("addedAt", f.addedAt),
                )
            }
            return arr.toString()
        }

        fun decode(raw: String): List<SafFolder> = try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SafFolder(
                            uri = o.getString("uri"),
                            name = o.optString("name", "Folder"),
                            addedAt = o.optLong("addedAt"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
