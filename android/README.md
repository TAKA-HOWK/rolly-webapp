# Android APK build

Этот проект оборачивает текущий `index.html` в нативное Android-приложение (`WebView`).

## Требования

- Android SDK (platform 35, build-tools)
- Java 17+
- Gradle 8+

## Подготовка окружения

1. Установите Android SDK.
2. Экспортируйте путь к SDK:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
# или
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
```

## Сборка debug APK

Вариант 1 (рекомендуется, с предварительными проверками):

```bash
./scripts/build-android-apk.sh
```

Вариант 2 (напрямую через Gradle):

```bash
cd android
gradle assembleDebug
```

Готовый файл:

`app/build/outputs/apk/debug/app-debug.apk`

## Если меняете сайт

После правок в корневом `index.html` обновите копию для Android:

```bash
cp ../index.html app/src/main/assets/index.html
```
