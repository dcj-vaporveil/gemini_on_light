# Implementation Plan: Native Android Light SDK Tool Port

Port the Gemini Voice Search application into a complete, native Android project using Kotlin, Jetpack Compose, and Gradle, compliant with the official Light Phone III SDK (`:sdk:client`).

## Proposed Architecture & Structure

The repository will provide the complete, production-grade native Android project ready to push directly to GitHub for Light Tool Library compilation and signing, alongside maintaining preview compatibility:

```
android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── tool/
    ├── lighttool.toml
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        └── kotlin/com/thelightphone/gemini/
            ├── DataStoreKeys.kt          // Typed DataStore preferences keys
            ├── DataStoreManager.kt       // Secure API key storage and encryption wrapper
            ├── GeminiToolViewModel.kt    // MVVM controller with coroutines & OkHttp REST
            ├── HomeScreen.kt             // Compose UI with @InitialScreen and press-and-hold
            ├── SettingsScreen.kt         // Compose UI for entering and persisting the BYOK key
            ├── VoiceSearchManager.kt     // LightAudioRecorder and SpeechRecognizer integration
            └── EmailForwarder.kt         // Android ACTION_SENDTO mailto intent dispatcher
```

---

## User Review Required

> [!IMPORTANT]
> **Key Storage & API Security**:
> - We will use `androidx.datastore:datastore-preferences` sandboxed in `DEFAULT_DATASTORE` as required by the Light SDK.
> - The API key will be stored locally on-device and never transmitted to any third-party analytics.
> - When querying, the tool will connect directly to Google Generative AI over HTTPS using the user's key.

---

## Proposed Changes

### 1. DataStore Preferences & Secure Storage (`DataStoreManager.kt` & `DataStoreKeys.kt`)
- Implement `DataStoreManager` wrapping `DataStore<Preferences>`.
- Expose reactive `Flow<String?>` for the Gemini API key.
- Provide suspend functions: `saveApiKey(key: String)`, `getApiKey(): String?`, `clearApiKey()`.
- Add preferences for conciseness mode and auto-readout.

### 2. Networking & Gemini Voice Search (`GeminiToolViewModel.kt`)
- Asynchronous coroutine-based client using `OkHttpClient`.
- Query `gemini-3.8-flash` with Google Search grounding (`tools: [{ "googleSearch": {} }]`).
- Parse search grounding sources and candidate text.
- Support audio queries via `LightAudioRecorder`.

### 3. Jetpack Compose UI (`HomeScreen.kt` & `SettingsScreen.kt`)
- Subclass `LightScreen` and `LightViewModel` from `:sdk:client`.
- Authentic LightOS monochromatic design tokens (`LightThemeTokens`, `LightText`, `LightIcons`).
- Press-and-hold on responses using Compose `pointerInput { detectTapGestures(onLongPress = { ... }) }`.
- Action sheet with **"Send to Email"** invoking Android `Intent.ACTION_SENDTO` with prefilled subject and body.

### 4. Build System & Manifest Configuration
- `tool/build.gradle.kts`: Configured for Android API 34, Jetpack Compose, Coroutines, and OkHttp.
- `tool/lighttool.toml`: Manifest declaring `network` and `detached-audio` capabilities.
- `tool/src/main/AndroidManifest.xml`: Permissions for `INTERNET` and `RECORD_AUDIO`.

### 5. Web Preview Studio & Code Downloader
- Ensure the live web preview continues to compile and run seamlessly in AI Studio, with a direct file viewer and ZIP exporter for the Android project.

---

## Verification Plan

### Automated Verification
- Run `compile_applet` to ensure the studio interface compiles without errors.
- Run `lint_applet` (`tsc --noEmit`) to verify zero TypeScript errors.
- Validate Kotlin file syntax and package naming consistency against the official `lightphone/light-sdk` specification.

### Manual Verification
- Verify that all native Kotlin files (`DataStoreManager.kt`, `HomeScreen.kt`, `GeminiToolViewModel.kt`, etc.) are written and present.
- Verify press-and-hold email forwarding logic and DataStore preference persistence.
