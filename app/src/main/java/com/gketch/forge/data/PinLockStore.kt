package com.gketch.forge.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.pinLockDataStore by preferencesDataStore(name = "forge_pin_lock")

data class PinLockState(
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val hasPin: Boolean = false,
)

class PinLockStore(context: Context) {
    private val appContext = context.applicationContext
    private val store = appContext.pinLockDataStore

    val state: Flow<PinLockState> = store.data.map { p ->
        val hash = p[KEY_HASH].orEmpty()
        PinLockState(
            enabled = (p[KEY_ENABLED] ?: false) && hash.isNotBlank(),
            biometricEnabled = p[KEY_BIOMETRIC] ?: false,
            hasPin = hash.isNotBlank(),
        )
    }

    suspend fun isEnabled(): Boolean = state.first().enabled

    suspend fun setBiometricEnabled(enabled: Boolean) {
        store.edit { it[KEY_BIOMETRIC] = enabled }
    }

    suspend fun setPin(pin: String): Boolean {
        val cleaned = pin.filter { it.isDigit() }
        if (cleaned.length !in 4..8) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(cleaned, salt)
        store.edit {
            it[KEY_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
            it[KEY_HASH] = hash
            it[KEY_ENABLED] = true
        }
        return true
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = store.data.first()
        val saltB64 = prefs[KEY_SALT] ?: return false
        val expected = prefs[KEY_HASH] ?: return false
        val salt = runCatching { Base64.decode(saltB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val cleaned = pin.filter { it.isDigit() }
        return hashPin(cleaned, salt) == expected
    }

    suspend fun disable() {
        store.edit {
            it[KEY_ENABLED] = false
            it.remove(KEY_HASH)
            it.remove(KEY_SALT)
            it[KEY_BIOMETRIC] = false
        }
        sessionUnlocked = false
    }

    companion object {
        /**
         * In-process unlock latch. Survives Activity recreate (e.g. language switch)
         * so Settings locale change does not feel like a process kill / re-lock.
         * Cleared when the process dies or PIN is disabled.
         */
        @Volatile
        var sessionUnlocked: Boolean = false

        private val KEY_ENABLED = booleanPreferencesKey("pin_enabled")
        private val KEY_BIOMETRIC = booleanPreferencesKey("pin_biometric")
        private val KEY_HASH = stringPreferencesKey("pin_hash")
        private val KEY_SALT = stringPreferencesKey("pin_salt")

        private fun hashPin(pin: String, salt: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(salt)
            md.update(pin.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(md.digest(), Base64.NO_WRAP)
        }
    }
}
