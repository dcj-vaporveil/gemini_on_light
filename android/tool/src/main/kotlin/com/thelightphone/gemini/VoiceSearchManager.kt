package com.thelightphone.gemini

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.audio.DefaultLightAudio
import com.thelightphone.sdk.audio.LightAudio
import com.thelightphone.sdk.audio.LightAudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Locale

sealed interface VoiceRecordingState {
    object Idle : VoiceRecordingState
    object Listening : VoiceRecordingState
    data class Transcribed(val text: String) : VoiceRecordingState
    data class Error(val message: String) : VoiceRecordingState
}

/**
 * Manages voice input on the Light Phone III.
 * Supports both the Light SDK LightAudioRecorder and native SpeechRecognizer.
 */
class VoiceSearchManager(
    private val context: Context,
    activity: SealedLightActivity? = null
) {
    private val _voiceState = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
    val voiceState: StateFlow<VoiceRecordingState> = _voiceState

    private var speechRecognizer: SpeechRecognizer? = null
    private val lightAudio: LightAudio? = activity?.let { DefaultLightAudio(it) }
    private var lightRecorder: LightAudioRecorder? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }
    }

    fun startListening() {
        _voiceState.value = VoiceRecordingState.Listening

        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } else if (lightAudio != null) {
            // Fallback to LightAudio file recorder
            try {
                lightRecorder = lightAudio.newRecorder()
                val targetFile = File(context.cacheDir, "voice_query.m4a")
                lightRecorder?.setOutputFile(targetFile)
                lightRecorder?.prepare()
                lightRecorder?.start()
            } catch (e: Exception) {
                _voiceState.value = VoiceRecordingState.Error("Audio recorder error: ${e.message}")
            }
        } else {
            _voiceState.value = VoiceRecordingState.Error("Voice input not available on this device")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        try {
            lightRecorder?.stop()
            lightRecorder?.release()
            lightRecorder = null
        } catch (_: Exception) {}
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _voiceState.value = VoiceRecordingState.Idle
        }
        override fun onError(error: Int) {
            _voiceState.value = VoiceRecordingState.Error("Speech recognition error code: $error")
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull().orEmpty()
            if (recognizedText.isNotBlank()) {
                _voiceState.value = VoiceRecordingState.Transcribed(recognizedText)
            } else {
                _voiceState.value = VoiceRecordingState.Idle
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        lightRecorder?.release()
        lightRecorder = null
    }
}
