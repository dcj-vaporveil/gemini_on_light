package com.thelightphone.gemini

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

/**
 * Main screen for the Light Phone Gemini Voice Search tool.
 * Features distraction-free monochrome styling, voice input, and press-and-hold email forwarding.
 */
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

        // Context menu dialog state for press-and-hold
        var showContextMenu by remember { mutableStateOf(false) }
        var activeAnswerToForward by remember { mutableStateOf<Pair<String, String>?>(null) }

        LightTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LightText(
                        text = "Voice Search",
                        variant = LightTextVariant.Heading
                    )
                    LightText(
                        text = "Settings",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.lightClickable {
                            navigateTo(::SettingsScreen)
                        }
                    )
                }

                // Query Input Box
                BasicTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    textStyle = LightThemeTokens.typography.copy.copy(
                        color = LightThemeTokens.colors.text
                    ),
                    cursorBrush = SolidColor(LightThemeTokens.colors.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    decorationBox = { inner ->
                        if (inputQuery.isEmpty()) {
                            LightText("Ask a question or speak...", variant = LightTextVariant.Copy, lighten = true)
                        }
                        inner()
                    }
                )

                // Action Row: Search & Clear
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    LightText(
                        text = "Search",
                        variant = LightTextVariant.Detail,
                        modifier = Modifier
                            .lightClickable {
                                if (inputQuery.isNotBlank()) {
                                    viewModel.search(inputQuery)
                                }
                            }
                            .padding(end = 24.dp)
                    )

                    LightText(
                        text = "Clear",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.lightClickable {
                            inputQuery = ""
                            viewModel.clear()
                        }
                    )
                }

                // Response Body with Press & Hold for Email
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    when (val current = state) {
                        is SearchUiState.Idle -> {
                            LightText(
                                text = "Grounded web search powered by Gemini. Hold response to send to email.",
                                variant = LightTextVariant.Detail,
                                lighten = true
                            )
                        }
                        is SearchUiState.Searching -> {
                            LightText(
                                text = "Searching Google & Gemini...",
                                variant = LightTextVariant.Copy
                            )
                        }
                        is SearchUiState.MissingKey -> {
                            LightText(
                                text = "No Gemini API key set.\\nTap 'Settings' to add your free AI Studio key to DataStore.",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.lightClickable { navigateTo(::SettingsScreen) }
                            )
                        }
                        is SearchUiState.Error -> {
                            LightText(
                                text = "Error: ${current.message}",
                                variant = LightTextVariant.Detail,
                                lighten = true
                            )
                        }
                        is SearchUiState.Success -> {
                            // Question header
                            LightText(
                                text = "Q: \"${current.query}\"",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Answer Container with Press-and-Hold Listener
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                activeAnswerToForward = Pair(current.query, current.answer)
                                                showContextMenu = true
                                            }
                                        )
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                LightText(
                                    text = current.answer,
                                    variant = LightTextVariant.Copy
                                )
                            }

                            // Grounded sources
                            if (current.sources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LightText(
                                    text = "Sources: ${current.sources.joinToString(", ") { it.title }}",
                                    variant = LightTextVariant.Detail,
                                    lighten = true
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            LightText(
                                text = "Tip: Press & hold answer text to forward to email.",
                                variant = LightTextVariant.Detail,
                                lighten = true
                            )
                        }
                    }
                }

                // Press & Hold Context Dialog
                if (showContextMenu && activeAnswerToForward != null) {
                    val (q, a) = activeAnswerToForward!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightThemeTokens.colors.background)
                            .padding(top = 16.dp)
                    ) {
                        LightText(
                            text = "Message Options",
                            variant = LightTextVariant.Heading,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LightText(
                            text = "Send to Email",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable {
                                    showContextMenu = false
                                    EmailForwarder.sendToEmail(
                                        context = context,
                                        recipientEmail = "dcj@vaporveil.com",
                                        query = q,
                                        answer = a
                                    )
                                }
                                .padding(vertical = 10.dp)
                        )

                        LightText(
                            text = "Copy Text",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Gemini Response", a))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    showContextMenu = false
                                }
                                .padding(vertical = 10.dp)
                        )

                        LightText(
                            text = "Cancel",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable { showContextMenu = false }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
