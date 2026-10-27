package com.thelightphone.gemini

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Type-safe DataStore preference keys for the Light Phone Gemini Tool.
 * Stored locally in the tool's sandboxed DEFAULT_DATASTORE.
 */
object DataStoreKeys {
    /** The BYOK Gemini API key entered by the user */
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")

    /** Whether to enforce ultra-concise answers (1-2 sentences) */
    val CONCISE_MODE = booleanPreferencesKey("concise_mode")

    /** Whether to automatically speak answers aloud upon completion */
    val AUTO_SPEAK_RESPONSES = booleanPreferencesKey("auto_speak_responses")

    /** Default user email for press-and-hold response forwarding */
    val FORWARDING_EMAIL = stringPreferencesKey("forwarding_email")

    /** Selected Gemini model identifier (default: gemini-3.8-flash) */
    val MODEL_NAME = stringPreferencesKey("model_name")
}
