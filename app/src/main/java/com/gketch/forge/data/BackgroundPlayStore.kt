package com.gketch.forge.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backgroundPlayDataStore by preferencesDataStore(name = "forge_background_play")

class BackgroundPlayStore(context: Context) {
    private val store = context.applicationContext.backgroundPlayDataStore

    val enabled: Flow<Boolean> = store.data.map { it[KEY] ?: DEFAULT }

    suspend fun setEnabled(value: Boolean) {
        store.edit { it[KEY] = value }
    }

    companion object {
        const val DEFAULT = true
        private val KEY = booleanPreferencesKey("background_play")
    }
}
