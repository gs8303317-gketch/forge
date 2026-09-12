package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.hiddenFoldersDataStore by preferencesDataStore(name = "forge_hidden_folders")

data class HiddenFolder(
    val bucketId: Long,
    val name: String,
)

class HiddenFoldersStore(context: Context) {
    private val store = context.applicationContext.hiddenFoldersDataStore

    val hidden: Flow<List<HiddenFolder>> = store.data.map { prefs ->
        decode(prefs[KEY] ?: "[]")
    }

    suspend fun snapshot(): List<HiddenFolder> = hidden.first()

    suspend fun hide(folder: MediaFolder) {
        mutate { list ->
            if (list.any { it.bucketId == folder.bucketId }) list
            else list + HiddenFolder(folder.bucketId, folder.name)
        }
    }

    suspend fun hide(bucketId: Long, name: String) {
        mutate { list ->
            if (list.any { it.bucketId == bucketId }) list
            else list + HiddenFolder(bucketId, name)
        }
    }

    suspend fun unhide(bucketId: Long) {
        mutate { list -> list.filterNot { it.bucketId == bucketId } }
    }

    suspend fun replaceAll(folders: List<HiddenFolder>) {
        store.edit { it[KEY] = encode(folders) }
    }

    private suspend fun mutate(block: (List<HiddenFolder>) -> List<HiddenFolder>) {
        store.edit { prefs ->
            prefs[KEY] = encode(block(decode(prefs[KEY] ?: "[]")))
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("hidden_folders_json")

        fun encode(items: List<HiddenFolder>): String {
            val arr = JSONArray()
            items.forEach { f ->
                arr.put(JSONObject().put("bucketId", f.bucketId).put("name", f.name))
            }
            return arr.toString()
        }

        fun decode(raw: String): List<HiddenFolder> = try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(HiddenFolder(o.optLong("bucketId"), o.optString("name", "Folder")))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
