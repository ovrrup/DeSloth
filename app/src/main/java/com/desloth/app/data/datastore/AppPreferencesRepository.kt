package com.desloth.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "desloth_preferences")

data class BypassState(
    val packageName: String?,
    val expiryMillis: Long,
    val remainingSeconds: Long,
    val isActive: Boolean
)

class AppPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_BYPASS_PACKAGE = stringPreferencesKey("bypass_target_package")
        val KEY_BYPASS_EXPIRY_MILLIS = longPreferencesKey("bypass_expiry_epoch_millis")
    }

    val bypassStateFlow: Flow<BypassState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val pkg = preferences[PreferencesKeys.KEY_BYPASS_PACKAGE]
            val expiry = preferences[PreferencesKeys.KEY_BYPASS_EXPIRY_MILLIS] ?: 0L
            val now = System.currentTimeMillis()
            val isActive = !pkg.isNullOrBlank() && now < expiry
            val remainingSecs = if (isActive) (expiry - now) / 1000L else 0L

            BypassState(
                packageName = if (isActive) pkg else null,
                expiryMillis = expiry,
                remainingSeconds = remainingSecs,
                isActive = isActive
            )
        }

    /**
     * Grants a 10-minute bypass timer for a specific package.
     */
    suspend fun setBypass(packageName: String, durationMinutes: Long = 10L) {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_BYPASS_PACKAGE] = packageName
            preferences[PreferencesKeys.KEY_BYPASS_EXPIRY_MILLIS] = expiry
        }
    }

    /**
     * Checks if the specified package has an unexpired bypass window.
     */
    suspend fun isBypassActive(packageName: String): Boolean {
        val preferences = context.dataStore.data.first()
        val pkg = preferences[PreferencesKeys.KEY_BYPASS_PACKAGE] ?: return false
        val expiry = preferences[PreferencesKeys.KEY_BYPASS_EXPIRY_MILLIS] ?: return false
        val now = System.currentTimeMillis()
        return pkg == packageName && now < expiry
    }

    /**
     * Revokes any active bypass window.
     */
    suspend fun clearBypass() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.KEY_BYPASS_PACKAGE)
            preferences.remove(PreferencesKeys.KEY_BYPASS_EXPIRY_MILLIS)
        }
    }
}
