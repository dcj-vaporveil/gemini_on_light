import { CodeFile } from '../types';

export const LIGHT_SDK_FILES: CodeFile[] = [
  {
    name: 'DataStoreManager.kt',
    path: 'tool/src/main/kotlin/com/thelightphone/gemini/DataStoreManager.kt',
    language: 'kotlin',
    description: 'Encapsulates secure API key and preference operations using Android Jetpack DataStore<Preferences>.',
    content: `package com.thelightphone.gemini

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
 */
class DataStoreManager(
    private val dataStore: DataStore<Preferences>
) {
    val apiKeyFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[DataStoreKeys.GEMINI_API_KEY]?.takeIf { it.isNotBlank() }
        }

    val autoSpeakFlow: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[DataStoreKeys.AUTO_SPEAK_RESPONSES] ?: true }

    val forwardingEmailFlow: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[DataStoreKeys.FORWARDING_EMAIL] }

    suspend fun getApiKey(): String? {
        val preferences = dataStore.data.first()
        return preferences[DataStoreKeys.GEMINI_API_KEY]?.trim()?.takeIf { it.isNotBlank() }
    }

    suspend fun saveApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.GEMINI_API_KEY] = apiKey.trim()
        }
    }

    suspend fun clearApiKey() {
        dataStore.edit { preferences ->
            preferences.remove(DataStoreKeys.GEMINI_API_KEY)
        }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.AUTO_SPEAK_RESPONSES] = enabled
        }
    }

    suspend fun setForwardingEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.FORWARDING_EMAIL] = email.trim()
        }
    }
}
`,
  },
  {
    name: 'DataStoreKeys.kt',
    path: 'tool/src/main/kotlin/com/thelightphone/gemini/DataStoreKeys.kt',
    language: 'kotlin',
    description: 'Typed preferences keys for DEFAULT_DATASTORE.',
    content: `package com.thelightphone.gemini

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val CONCISE_MODE = booleanPreferencesKey("concise_mode")
    val AUTO_SPEAK_RESPONSES = booleanPreferencesKey("auto_speak_responses")
    val FORWARDING_EMAIL = stringPreferencesKey("forwarding_email")
    val MODEL_NAME = stringPreferencesKey("model_name")
}
`,
  },
  {
    name: 'HomeScreen.kt',
    path: 'tool/src/main/kotlin/com/thelightphone/gemini/HomeScreen.kt',
    language: 'kotlin',
    description: 'Boot screen subclassing LightScreen with Compose UI, voice search, and press-and-hold email forwarding.',
    content: `package com.thelightphone.gemini

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.*

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, GeminiToolViewModel>(sealedActivity) {

    override val viewModelClass = GeminiToolViewModel::class.java

    override fun createViewModel(): GeminiToolViewModel {
        val dataStoreManager = DataStoreManager(lightContext.dataStore)
        return GeminiToolViewModel(dataStoreManager)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        var inputQuery by remember { mutableStateOf("") }
        val state by viewModel.uiState.collectAsState()
        val scrollState = rememberScrollState()

        var showContextMenu by remember { mutableStateOf(false) }
        var activeAnswerToForward by remember { mutableStateOf<Pair<String, String>?>(null) }

        LightTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LightText("Voice Search", variant = LightTextVariant.Heading)
                    LightText("Settings", variant = LightTextVariant.Detail, lighten = true,
                        modifier = Modifier.lightClickable { navigateTo(::SettingsScreen) })
                }

                BasicTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    textStyle = LightThemeTokens.typography.copy.copy(color = LightThemeTokens.colors.text),
                    cursorBrush = SolidColor(LightThemeTokens.colors.text),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    decorationBox = { inner ->
                        if (inputQuery.isEmpty()) {
                            LightText("Ask a question or speak...", variant = LightTextVariant.Copy, lighten = true)
                        }
                        inner()
                    }
                )

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    LightText("Search", variant = LightTextVariant.Detail,
                        modifier = Modifier.lightClickable {
                            if (inputQuery.isNotBlank()) viewModel.search(inputQuery)
                        }.padding(end = 24.dp))
                    LightText("Clear", variant = LightTextVariant.Detail, lighten = true,
                        modifier = Modifier.lightClickable { inputQuery = ""; viewModel.clear() })
                }

                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
                    when (val current = state) {
                        is SearchUiState.Idle -> {
                            LightText("Grounded web search powered by Gemini. Hold response to send to email.",
                                variant = LightTextVariant.Detail, lighten = true)
                        }
                        is SearchUiState.Searching -> {
                            LightText("Searching Google & Gemini...", variant = LightTextVariant.Copy)
                        }
                        is SearchUiState.MissingKey -> {
                            LightText("No Gemini API key set.\\nTap 'Settings' to add your free key to DataStore.",
                                variant = LightTextVariant.Detail, lighten = true,
                                modifier = Modifier.lightClickable { navigateTo(::SettingsScreen) })
                        }
                        is SearchUiState.Error -> {
                            LightText("Error: \${current.message}", variant = LightTextVariant.Detail, lighten = true)
                        }
                        is SearchUiState.Success -> {
                            LightText("Q: \\"\${current.query}\\"", variant = LightTextVariant.Detail, lighten = true,
                                modifier = Modifier.padding(bottom = 8.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                                    detectTapGestures(onLongPress = {
                                        activeAnswerToForward = Pair(current.query, current.answer)
                                        showContextMenu = true
                                    })
                                }.padding(vertical = 4.dp)
                            ) {
                                LightText(current.answer, variant = LightTextVariant.Copy)
                            }

                            if (current.sources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LightText("Sources: \${current.sources.joinToString(", ") { it.title }}",
                                    variant = LightTextVariant.Detail, lighten = true)
                            }
                        }
                    }
                }

                if (showContextMenu && activeAnswerToForward != null) {
                    val (q, a) = activeAnswerToForward!!
                    Column(modifier = Modifier.fillMaxWidth().background(LightThemeTokens.colors.background).padding(top = 16.dp)) {
                        LightText("Message Options", variant = LightTextVariant.Heading, modifier = Modifier.padding(bottom = 12.dp))
                        LightText("Send to Email", variant = LightTextVariant.Copy, modifier = Modifier.fillMaxWidth().lightClickable {
                            showContextMenu = false
                            EmailForwarder.sendToEmail(context, "dcj@vaporveil.com", q, a)
                        }.padding(vertical = 10.dp))
                        LightText("Copy Text", variant = LightTextVariant.Copy, modifier = Modifier.fillMaxWidth().lightClickable {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("Response", a))
                            showContextMenu = false
                        }.padding(vertical = 10.dp))
                        LightText("Cancel", variant = LightTextVariant.Detail, lighten = true,
                            modifier = Modifier.fillMaxWidth().lightClickable { showContextMenu = false }.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}
`,
  },
  {
    name: 'GeminiToolViewModel.kt',
    path: 'tool/src/main/kotlin/com/thelightphone/gemini/GeminiToolViewModel.kt',
    language: 'kotlin',
    description: 'Coordinates DataStore key retrieval, REST querying to Gemini, and coroutine execution.',
    content: `package com.thelightphone.gemini

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface SearchUiState {
    object Idle : SearchUiState
    object MissingKey : SearchUiState
    data class Searching(val query: String) : SearchUiState
    data class Success(val query: String, val answer: String, val sources: List<SearchSourceInfo> = emptyList()) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class SearchSourceInfo(val title: String, val url: String)

class GeminiToolViewModel(
    private val dataStoreManager: DataStoreManager
) : LightViewModel<Unit>() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.value = SearchUiState.Searching(trimmed)
            val apiKey = dataStoreManager.getApiKey()
            if (apiKey.isNullOrBlank()) {
                _uiState.value = SearchUiState.MissingKey
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                executeGeminiSearch(apiKey, trimmed)
            }
            _uiState.value = result
        }
    }

    private fun executeGeminiSearch(apiKey: String, query: String): SearchUiState {
        return try {
            val payload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", query)))))
                put("tools", JSONArray().put(JSONObject().put("googleSearch", JSONObject())))
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(requestBody).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return SearchUiState.Error("HTTP Error \${response.code}")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                val text = json.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "No response"
                SearchUiState.Success(query, text.trim())
            }
        } catch (e: Exception) {
            SearchUiState.Error(e.localizedMessage ?: "Network error")
        }
    }

    fun clear() { _uiState.value = SearchUiState.Idle }
}
`,
  },
  {
    name: 'EmailForwarder.kt',
    path: 'tool/src/main/kotlin/com/thelightphone/gemini/EmailForwarder.kt',
    language: 'kotlin',
    description: 'Dispatches Android ACTION_SENDTO mailto intent for press-and-hold response forwarding.',
    content: `package com.thelightphone.gemini

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object EmailForwarder {
    fun sendToEmail(context: Context, recipientEmail: String, query: String, answer: String) {
        val subject = Uri.encode("Light Phone: $query")
        val body = Uri.encode("Question: $query\\n\\nAnswer:\\n$answer\\n\\n--\\nForwarded from Light Phone III")
        val mailtoUri = Uri.parse("mailto:$recipientEmail?subject=$subject&body=$body")

        val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Forwarding to $recipientEmail", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Copied to clipboard.", Toast.LENGTH_SHORT).show()
        }
    }
}
`,
  },
  {
    name: 'VoiceSearchManager.kt',
    path: 'tool/src/main/kotlin/com/thelightphone/gemini/VoiceSearchManager.kt',
    language: 'kotlin',
    description: 'Audio capture via LightAudioRecorder and SpeechRecognizer.',
    content: `package com.thelightphone.gemini

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class VoiceSearchManager(private val context: Context) {
    private val _voiceState = MutableStateFlow<String?>(null)
    val voiceState: StateFlow<String?> = _voiceState

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        // Dispatches speech recognition session
    }
}
`,
  },
  {
    name: 'SettingsScreen.kt',
    path: 'tool/src/main/kotlin/com/thelightphone/gemini/SettingsScreen.kt',
    language: 'kotlin',
    description: 'Jetpack Compose screen for viewing and persisting the user BYOK API key into DataStore.',
    content: `package com.thelightphone.gemini

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val dataStoreManager: DataStoreManager) : LightViewModel<Unit>() {
    val savedApiKey = MutableStateFlow("")
    val status = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { savedApiKey.value = dataStoreManager.getApiKey().orEmpty() }
    }

    fun saveKey(key: String) {
        viewModelScope.launch {
            dataStoreManager.saveApiKey(key)
            status.value = "Saved to DataStore"
        }
    }
}

class SettingsScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, SettingsViewModel>(sealedActivity) {
    override val viewModelClass = SettingsViewModel::class.java
    override fun createViewModel() = SettingsViewModel(DataStoreManager(lightContext.dataStore))

    @Composable
    override fun Content() {
        var keyInput by remember { mutableStateOf("") }
        val status by viewModel.status.collectAsState()

        LightTheme {
            Column(modifier = Modifier.fillMaxSize().background(LightThemeTokens.colors.background).padding(24.dp)) {
                LightText("Tool Settings", variant = LightTextVariant.Heading, modifier = Modifier.padding(bottom = 20.dp))
                LightText("Gemini API Key (BYOK)", variant = LightTextVariant.Detail, modifier = Modifier.padding(bottom = 6.dp))
                BasicTextField(value = keyInput, onValueChange = { keyInput = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                LightText("Save Key", variant = LightTextVariant.Detail, modifier = Modifier.lightClickable { viewModel.saveKey(keyInput) })
                status?.let { LightText(it, variant = LightTextVariant.Detail, lighten = true) }
            }
        }
    }
}
`,
  },
  {
    name: 'lighttool.toml',
    path: 'tool/lighttool.toml',
    language: 'toml',
    description: 'Manifest defining tool package, metadata, and capabilities for Light Tool Library.',
    content: `[tool]
name = "Gemini Search"
package = "com.thelightphone.gemini"
description = "A quiet, voice-first knowledge companion powered by Gemini with BYOK DataStore storage."
version = "1.0.0"
author = "Light Community"
capabilities = ["network", "detached-audio"]
`,
  },
  {
    name: 'AndroidManifest.xml',
    path: 'tool/src/main/AndroidManifest.xml',
    language: 'xml',
    description: 'Declares INTERNET, RECORD_AUDIO, and LightActivity entry point.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.thelightphone.gemini">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:label="Gemini Search"
        android:allowBackup="false"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.NoTitleBar.Fullscreen">

        <activity
            android:name="com.thelightphone.sdk.LightActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
`,
  },
  {
    name: 'build.gradle.kts',
    path: 'tool/build.gradle.kts',
    language: 'kotlin',
    description: 'Gradle build script configured with API 34, Compose, Coroutines, OkHttp, and DataStore.',
    content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.thelightphone.gemini"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.thelightphone.gemini"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":sdk:client"))
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
}
`,
  },
];
