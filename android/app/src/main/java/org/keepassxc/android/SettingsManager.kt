package org.keepassxc.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val AUTOFILL_ENABLED = booleanPreferencesKey("autofill_enabled")
        val CLIPBOARD_TIMEOUT = intPreferencesKey("clipboard_timeout")
        val BIOMETRIC_UNLOCK = booleanPreferencesKey("biometric_unlock")
        val LAST_DATABASE_URI = stringPreferencesKey("last_database_uri")
        val ENCRYPTED_PASSWORD = stringPreferencesKey("encrypted_password")
        val ENCRYPTED_PASSWORD_IV = stringPreferencesKey("encrypted_password_iv")
    }

    val autofillEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTOFILL_ENABLED] ?: false
    }

    suspend fun setAutofillEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTOFILL_ENABLED] = enabled
        }
    }

    val clipboardTimeout: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CLIPBOARD_TIMEOUT] ?: 10 // default 10 seconds
    }

    suspend fun setClipboardTimeout(timeout: Int) {
        context.dataStore.edit { preferences ->
            preferences[CLIPBOARD_TIMEOUT] = timeout
        }
    }

    val biometricUnlockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_UNLOCK] ?: false
    }

    suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_UNLOCK] = enabled
        }
    }

    val lastDatabaseUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_DATABASE_URI]
    }

    suspend fun setLastDatabaseUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_DATABASE_URI] = uri
        }
    }

    val encryptedPassword: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ENCRYPTED_PASSWORD]
    }

    val encryptedPasswordIv: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ENCRYPTED_PASSWORD_IV]
    }

    suspend fun saveEncryptedPassword(password: String, iv: String) {
        context.dataStore.edit { preferences ->
            preferences[ENCRYPTED_PASSWORD] = password
            preferences[ENCRYPTED_PASSWORD_IV] = iv
        }
    }
    
    suspend fun clearEncryptedPassword() {
        context.dataStore.edit { preferences ->
            preferences.remove(ENCRYPTED_PASSWORD)
            preferences.remove(ENCRYPTED_PASSWORD_IV)
        }
    }
}
