package com.thelightphone.gemini

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
    data class Success(
        val query: String,
        val answer: String,
        val sources: List<SearchSourceInfo> = emptyList()
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class SearchSourceInfo(
    val title: String,
    val url: String
)

/**
 * GeminiToolViewModel coordinates DataStore key retrieval, REST querying to Gemini,
 * and state management for the Light Phone III.
 */
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

            // 1. Fetch user's Gemini key from on-device DataStore
            val apiKey = dataStoreManager.getApiKey()
            if (apiKey.isNullOrBlank()) {
                _uiState.value = SearchUiState.MissingKey
                return@launch
            }

            // 2. Perform network request off the main thread
            val result = withContext(Dispatchers.IO) {
                executeGeminiSearch(apiKey, trimmed)
            }

            _uiState.value = result
        }
    }

    private fun executeGeminiSearch(apiKey: String, query: String): SearchUiState {
        return try {
            val systemInstruction =
                "You are the Voice Search companion for the Light Phone III. " +
                "The user is on a minimalist, distraction-free device. " +
                "Provide direct, factual, calm, concise answers (1 to 2 short paragraphs max). " +
                "Never use markdown headers, bold bullet lists, emojis, greetings, or conversational filler."

            // Build request with Google Search grounding
            val payload = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", query)
                    ))
                ))
                // Enable Google Search grounding
                put("tools", JSONArray().put(
                    JSONObject().put("googleSearch", JSONObject())
                ))
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return if (code == 400) {
                        SearchUiState.Error("Invalid API Key. Please verify key in Settings.")
                    } else if (code == 429) {
                        SearchUiState.Error("Quota limit reached on your Gemini key.")
                    } else {
                        SearchUiState.Error("HTTP Error $code: ${response.message}")
                    }
                }

                val responseJson = JSONObject(response.body?.string().orEmpty())
                val candidate = responseJson.optJSONArray("candidates")?.optJSONObject(0)
                val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text", "No response returned.") ?: "No response returned."

                // Parse search sources if present
                val sourcesList = mutableListOf<SearchSourceInfo>()
                val groundingMeta = candidate?.optJSONObject("groundingMetadata")
                val chunks = groundingMeta?.optJSONArray("groundingChunks")
                if (chunks != null) {
                    for (i in 0 until chunks.length()) {
                        val web = chunks.getJSONObject(i).optJSONObject("web")
                        val title = web?.optString("title").orEmpty()
                        val uri = web?.optString("uri").orEmpty()
                        if (title.isNotBlank() && uri.isNotBlank()) {
                            sourcesList.add(SearchSourceInfo(title, uri))
                        }
                    }
                }

                SearchUiState.Success(query, text.trim(), sourcesList.take(3))
            }
        } catch (e: Exception) {
            SearchUiState.Error(e.localizedMessage ?: "Network connection failed")
        }
    }

    fun clear() {
        _uiState.value = SearchUiState.Idle
    }
}
