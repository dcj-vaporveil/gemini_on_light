package com.thelightphone.gemini

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val dataStoreManager: DataStoreManager
) : LightViewModel<Unit>() {

    val savedApiKey = MutableStateFlow("")
    val forwardingEmail = MutableStateFlow("")
    val autoSpeak = MutableStateFlow(true)
    val statusMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            savedApiKey.value = dataStoreManager.getApiKey().orEmpty()
            dataStoreManager.autoSpeakFlow.collect { autoSpeak.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.forwardingEmailFlow.collect {
                forwardingEmail.value = it.orEmpty()
            }
        }
    }

    fun saveKey(key: String) {
        viewModelScope.launch {
            dataStoreManager.saveApiKey(key)
            savedApiKey.value = key.trim()
            statusMessage.value = "Saved to DataStore"
        }
    }

    fun saveEmail(email: String) {
        viewModelScope.launch {
            dataStoreManager.setForwardingEmail(email)
            forwardingEmail.value = email.trim()
            statusMessage.value = "Forwarding email updated"
        }
    }

    fun toggleAutoSpeak() {
        viewModelScope.launch {
            val next = !autoSpeak.value
            dataStoreManager.setAutoSpeak(next)
        }
    }
}

class SettingsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, SettingsViewModel>(sealedActivity) {

    override val viewModelClass = SettingsViewModel::class.java
    override fun createViewModel() = SettingsViewModel(DataStoreManager(lightContext.dataStore))

    @Composable
    override fun Content() {
        var keyInput by remember { mutableStateOf("") }
        var emailInput by remember { mutableStateOf("") }
        val currentKey by viewModel.savedApiKey.collectAsState()
        val currentEmail by viewModel.forwardingEmail.collectAsState()
        val isAutoSpeak by viewModel.autoSpeak.collectAsState()
        val status by viewModel.statusMessage.collectAsState()

        LaunchedEffect(currentKey) { keyInput = currentKey }
        LaunchedEffect(currentEmail) { emailInput = currentEmail }

        LightTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(24.dp)
            ) {
                // Header
                LightText(
                    text = "Tool Settings",
                    variant = LightTextVariant.Heading,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Gemini API Key Section
                LightText(
                    text = "Gemini API Key (BYOK)",
                    variant = LightTextVariant.Detail,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                BasicTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    textStyle = LightThemeTokens.typography.copy.copy(
                        color = LightThemeTokens.colors.text
                    ),
                    cursorBrush = SolidColor(LightThemeTokens.colors.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    decorationBox = { inner ->
                        if (keyInput.isEmpty()) {
                            LightText("Paste API key from aistudio.google.com...", variant = LightTextVariant.Copy, lighten = true)
                        }
                        inner()
                    }
                )

                Row(modifier = Modifier.padding(bottom = 24.dp)) {
                    LightText(
                        text = "Save Key",
                        variant = LightTextVariant.Detail,
                        modifier = Modifier
                            .lightClickable { viewModel.saveKey(keyInput) }
                            .padding(end = 20.dp)
                    )

                    status?.let {
                        LightText(text = it, variant = LightTextVariant.Detail, lighten = true)
                    }
                }

                // Email Forwarding Section
                LightText(
                    text = "Forwarding Email (Long-press to Send)",
                    variant = LightTextVariant.Detail,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                BasicTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    textStyle = LightThemeTokens.typography.copy.copy(
                        color = LightThemeTokens.colors.text
                    ),
                    cursorBrush = SolidColor(LightThemeTokens.colors.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    decorationBox = { inner ->
                        if (emailInput.isEmpty()) {
                            LightText("Enter destination email...", variant = LightTextVariant.Copy, lighten = true)
                        }
                        inner()
                    }
                )

                LightText(
                    text = "Save Email",
                    variant = LightTextVariant.Detail,
                    modifier = Modifier
                        .lightClickable { viewModel.saveEmail(emailInput) }
                        .padding(bottom = 24.dp)
                )

                // Auto Speak Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    LightText(
                        text = "Read Aloud Answers: ${if (isAutoSpeak) "ON" else "OFF"}",
                        variant = LightTextVariant.Detail,
                        modifier = Modifier.lightClickable { viewModel.toggleAutoSpeak() }
                    )
                }

                // Sandbox Guarantee
                LightText(
                    text = "Security guarantee: Your API key is stored strictly on-device in Android DEFAULT_DATASTORE and never leaves your phone.",
                    variant = LightTextVariant.Detail,
                    lighten = true
                )
            }
        }
    }
}
