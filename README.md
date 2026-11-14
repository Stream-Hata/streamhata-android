# StreamHata Android

[![codecov](https://codecov.io/gh/Stream-Hata/streamhata-android/branch/master/graph/badge.svg)](https://codecov.io/gh/Stream-Hata/streamhata-android)

Нативные Android-приложения StreamHata на Kotlin и Jetpack Compose.

## Модули

- `:app` — основное приложение;
- `:chat-app` — отдельный мессенджер;
- `:core` — API, криптография, E2EE и общие модели;
- `:tui-editor` — нативный редактор сообщений и plugin API.

## Сборка

Нужен JDK 17 и Android SDK 35.

```bash
./gradlew :app:assembleDebug :chat-app:assembleDebug
```

Тесты и coverage:

```bash
./gradlew :core:testDebugUnitTest :core:jacocoTestReport
```
