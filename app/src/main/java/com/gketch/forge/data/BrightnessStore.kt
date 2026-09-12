package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

private val Context.brightnessDataStore by preferencesDataStore(name = "forge_brightness")

/**
 * Per-URI video brightness fraction.
 * Range 0.01..2.0 → HUD 1%..200% where 1.0 = native (100%).
 * Values below 1 dim via black overlay; above 1 boost via ColorMatrix.
 */
class BrightnessStore(context: Context) {
    private val store = context.applicationContext.brightnessDataStore

    suspend fun get(uri: String): Float? {
        val key = floatPreferencesKey(keyFor(uri))
        return store.data.first()[key]
    }

    suspend fun save(uri: String, fraction: Float) {
        val key = floatPreferencesKey(keyFor(uri))
        store.edit { it[key] = fraction.coerceIn(MIN, MAX) }
    }

    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    companion object {
        const val MIN = 0.01f
        const val MAX = 2f
        const val DEFAULT = 1f

        fun keyFor(uri: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray())
            val hex = digest.take(12).joinToString("") { "%02x".format(it) }
            return "brit_$hex"
        }
    }
}
