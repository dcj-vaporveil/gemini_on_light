package com.thelightphone.gemini

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStoreManager encapsulates secure API key and preference operations
 * for the Light Phone III using Android Jetpack DataStore<Preferences>.
 *
 * All values are sandboxed within the application's private storage and
 * never uploaded to Light servers or third-party loggers.
 */
class DataStoreManager(
    private val dataStore: DataStore<Preferences>
) {
    /**
     * Flow observing the stored Gemini API key in real-time.
     */
    val apiKeyFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DataStoreKeys.GEMINI_API_KEY]?.takeIf { it.isNotBlank() }
        }

    /**
     * Flow observing the auto-speak preference.
     */
    val autoSpeakFlow: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[DataStoreKeys.AUTO_SPEAK_RESPONSES] ?: true }

    /**
     * Flow observing the user's email for SMS-style forwarding.
     */
    val forwardingEmailFlow: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[DataStoreKeys.FORWARDING_EMAIL] }

    /**
     * Suspends until the current API key is read.
     */
    suspend fun getApiKey(): String? {
        val preferences = dataStore.data.first()
        return preferences[DataStoreKeys.GEMINI_API_KEY]?.trim()?.takeIf { it.isNotBlank() }
    }

    /**
     * Securely saves the user's Gemini API key into DataStore.
     */
    suspend fun saveApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.GEMINI_API_KEY] = apiKey.trim()
        }
    }

    /**
     * Clears the API key from device storage.
     */
    suspend fun clearApiKey() {
        dataStore.edit { preferences ->
            preferences.remove(DataStoreKeys.GEMINI_API_KEY)
        }
    }

    /**
     * Updates the auto-speak setting.
     */
    suspend fun setAutoSpeak(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.AUTO_SPEAK_RESPONSES] = enabled
        }
    }

    /**
     * Updates the forwarding destination email.
     */
    suspend fun setForwardingEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.FORWARDING_EMAIL] = email.trim()
        }
    }

    /**
     * Helper to mask the key for display on the Light Phone screen (e.g. AIzaSy...9aBc)
     */
    fun maskApiKey(key: String): String {
        if (key.length <= 10) return "••••••••"
        return "${key.take(6)}...${key.takeLast(4)}"
    }
}
