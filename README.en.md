# StreamHata Android

[![codecov](https://codecov.io/gh/Stream-Hata/streamhata-android/branch/master/graph/badge.svg)](https://codecov.io/gh/Stream-Hata/streamhata-android)

Native StreamHata Android applications built with Kotlin and Jetpack Compose.

## Modules

- `:app` — main application;
- `:chat-app` — standalone messenger;
- `:core` — API, cryptography, E2EE and shared models;
- `:tui-editor` — native message editor and plugin API.

## Build

Requires JDK 17 and Android SDK 35.

```bash
./gradlew :app:assembleDebug :chat-app:assembleDebug
```

Tests and coverage:

```bash
./gradlew :core:testDebugUnitTest :core:jacocoTestReport
```
